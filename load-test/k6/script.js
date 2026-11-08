import http from 'k6/http';
import { check, fail } from 'k6';
import exec from 'k6/execution';
import { Counter } from 'k6/metrics';

const BASE = __ENV.BASE_URL || 'http://events-manager:8080'; // NOSONAR: internal compose network, the app serves plain HTTP
const USERS = 200;
// The load-test users are created in the Cognito pool by infra/cognito.tf (load_test_password).
const ISSUER = __ENV.COGNITO_ISSUER_URI;
const CLIENT_ID = __ENV.LOAD_TEST_CLIENT_ID; // the load-test app client, the only one that accepts a password
const PASSWORD = __ENV.LOAD_TEST_PASSWORD;
const LIST_SIZE = 20;
const LIST_PAGES = 50;
const JSON_HEADERS = { 'Content-Type': 'application/json' };

const created = new Counter('registrations_created');
const rejected = new Counter('registrations_rejected');

export const options = {
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  // 200 Cognito logins (a round trip to AWS each) do not fit in the default 60 s.
  setupTimeout: '5m',
  scenarios: {
    race: {
      executor: 'per-vu-iterations',
      exec: 'race',
      vus: USERS,
      iterations: 1,
      maxDuration: '1m',
    },
    browse: {
      executor: 'constant-arrival-rate',
      exec: 'browse',
      startTime: '10s',
      rate: 50,
      timeUnit: '1s',
      duration: '2m',
      preAllocatedVUs: 50,
      maxVUs: USERS,
    },
  },
  thresholds: {
    registrations_created: ['count==50'],
    registrations_rejected: ['count==150'],
    'http_req_failed{scenario:browse}': ['rate<0.01'],
    'http_req_duration{scenario:browse}': ['p(95)<500'],
  },
};

function username(n) {
  return `load-test-user-${n}@example.com`;
}

function bearer(token) {
  return { Authorization: `Bearer ${token}` };
}

// Logs in through the Cognito API (the same call as `aws cognito-idp initiate-auth`); the endpoint is the
// issuer's origin. The token is signed by Cognito, so the API validates it like any other.
function cognitoLogin(n, tags) {
  const res = http.post(ISSUER.split('/').slice(0, 3).join('/') + '/', JSON.stringify({
    AuthFlow: 'USER_PASSWORD_AUTH',
    ClientId: CLIENT_ID,
    AuthParameters: { USERNAME: username(n), PASSWORD: PASSWORD },
  }), {
    headers: {
      'Content-Type': 'application/x-amz-json-1.1',
      'X-Amz-Target': 'AWSCognitoIdentityProviderService.InitiateAuth',
    },
    tags,
  });
  const token = res.status === 200 ? res.json('AuthenticationResult.AccessToken') : null;
  if (!token) {
    fail(`Cognito login failed for ${username(n)} (status ${res.status})`);
  }
  return token;
}

export function setup() {
  if (!ISSUER || !CLIENT_ID || !PASSWORD) {
    fail('set COGNITO_ISSUER_URI, LOAD_TEST_CLIENT_ID and LOAD_TEST_PASSWORD in .env');
  }
  const setupTag = (name) => ({ phase: 'setup', name: `setup: ${name}` });
  const tokens = [];

  for (let n = 1; n <= USERS; n++) {
    const token = cognitoLogin(n, setupTag('cognito login'));

    // A second run gets 409 here, which is fine: the profile already exists.
    const profile = http.post(`${BASE}/users/me`, JSON.stringify({
      name: `Load Test ${n}`,
      age: 30,
      address: { street: 'Via del Corso 1', city: 'Rome', stateOrProvince: 'Lazio', postalCode: '00186', countryCode: 'IT' },
    }), { headers: { ...JSON_HEADERS, ...bearer(token) }, tags: setupTag('create profile') });
    if (profile.status !== 201 && profile.status !== 409) {
      fail(`profile failed for ${username(n)} (status ${profile.status})`);
    }
    tokens.push(token);
  }

  const found = http.get(`${BASE}/events?name=load-test-race&size=1`,
    { headers: bearer(tokens[0]), tags: setupTag('find race event') });
  const content = found.json('content');
  if (!content || content.length === 0) {
    fail('event load-test-race not found: run load-test/seed.sql first');
  }
  const eventId = content[0].id;

  // Makes the race repeatable: drops the registrations left by a previous run (404 means there was none).
  for (const token of tokens) {
    http.del(`${BASE}/events/${eventId}/registrations`, null, { headers: bearer(token), tags: setupTag('clear registrations') });
  }

  const eventIds = [];
  for (let page = 0; page < 20; page++) {
    const res = http.get(`${BASE}/events?page=${page}&size=50`, { headers: bearer(tokens[0]), tags: setupTag('list events') });
    if (res.status !== 200) break;
    for (const event of res.json('content')) {
      eventIds.push(event.id);
    }
  }

  return { tokens, eventId, eventIds };
}

export function race(data) {
  const res = http.post(`${BASE}/events/${data.eventId}/registrations`, null, {
    // iterationInTest is 0..199 inside this scenario; __VU is shared with the browse scenario's VUs.
    headers: bearer(data.tokens[exec.scenario.iterationInTest]),
    tags: { name: 'POST /events/{eventId}/registrations' },
  });

  if (res.status === 201) {
    created.add(1);
  } else if (res.status === 409) {
    rejected.add(1);
  }
  check(res, { 'is 201 or 409': (r) => r.status === 201 || r.status === 409 });
}

export function browse(data) {
  const headers = bearer(data.tokens[(__VU - 1) % USERS]);

  // A random page and a random event keep the mix realistic; always asking for page 0 would flatter any cache.
  const page = Math.floor(Math.random() * LIST_PAGES);
  const list = http.get(`${BASE}/events?page=${page}&size=${LIST_SIZE}`,
    { headers, tags: { name: 'GET /events' } });

  const id = data.eventIds[Math.floor(Math.random() * data.eventIds.length)];
  const detail = http.get(`${BASE}/events/${id}`, { headers, tags: { name: 'GET /events/{id}' } });

  check(list, { 'list is 200': (r) => r.status === 200 });
  check(detail, { 'detail is 200': (r) => r.status === 200 });
}
