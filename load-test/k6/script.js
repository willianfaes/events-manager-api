import http from 'k6/http';
import { check, fail } from 'k6';
import { Counter } from 'k6/metrics';

const BASE = __ENV.BASE_URL || 'http://events-manager:8080';
const USERS = 200;
const PASSWORD = 'LoadTest1234!';
const JSON_HEADERS = { 'Content-Type': 'application/json' };

const created = new Counter('registrations_created');
const rejected = new Counter('registrations_rejected');

export const options = {
  scenarios: {
    race: {
      executor: 'per-vu-iterations',
      exec: 'race',
      vus: USERS,
      iterations: 1,
      maxDuration: '1m',
    },
  },
  thresholds: {
    registrations_created: ['count==50'],
    registrations_rejected: ['count==150'],
  },
};

function email(n) {
  return `load-test-user-${n}@example.com`;
}

function bearer(token) {
  return { Authorization: `Bearer ${token}` };
}

export function setup() {
  const setupTags = { phase: 'setup' };
  const tokens = [];

  for (let n = 1; n <= USERS; n++) {
    // A second run gets a conflict here, which is fine: the user already exists.
    http.post(`${BASE}/auth/register`, JSON.stringify({
      name: `Load Test ${n}`,
      email: email(n),
      age: 30,
      password: PASSWORD,
      address: { street: 'Via del Corso 1', city: 'Rome', stateOrProvince: 'Lazio', postalCode: '00186', countryCode: 'IT' },
    }), { headers: JSON_HEADERS, tags: setupTags });

    const login = http.post(`${BASE}/auth/login`, JSON.stringify({ email: email(n), password: PASSWORD }),
      { headers: JSON_HEADERS, tags: setupTags });
    const token = login.json('token');
    if (!token) {
      fail(`login failed for ${email(n)} (status ${login.status})`);
    }
    tokens.push(token);
  }

  const found = http.get(`${BASE}/events?name=load-test-race&size=1`,
    { headers: bearer(tokens[0]), tags: setupTags });
  const content = found.json('content');
  if (!content || content.length === 0) {
    fail('event load-test-race not found: run load-test/seed.sql first');
  }
  const eventId = content[0].id;

  // Makes the race repeatable: drops the registrations left by a previous run (404 means there was none).
  for (const token of tokens) {
    http.del(`${BASE}/events/${eventId}/registrations`, null, { headers: bearer(token), tags: setupTags });
  }

  return { tokens, eventId };
}

export function race(data) {
  const res = http.post(`${BASE}/events/${data.eventId}/registrations`, null, {
    headers: bearer(data.tokens[__VU - 1]),
    tags: { name: 'POST /events/{eventId}/registrations' },
  });

  if (res.status === 201) {
    created.add(1);
  } else if (res.status === 409) {
    rejected.add(1);
  }
  check(res, { 'is 201 or 409': (r) => r.status === 201 || r.status === 409 });
}
