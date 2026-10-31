# Running Events Manager API

How to run, try, test and measure the project on your machine. The [README](README.md) says what it does and links the live demo; [ARCHITECTURE.md](ARCHITECTURE.md) says how it works.

Pick the section that matches what you want to do:

- [Quick start (Docker)](#quick-start-docker): the whole stack running with a few commands, nothing to configure.
- [Local development (IDE)](#local-development-ide): the app in your IDE, to change code and debug.
- [Trying the API](#trying-the-api): logging in, Swagger UI and `curl`, for either of the two above.
- [Tests](#tests), [Monitoring](#monitoring-prometheus-and-grafana) and the [load test](#load-test-k6).
- [Reference](#reference): environment variables, Cognito settings, compose commands and commit conventions.

## Quick start (Docker)

You need [Docker Desktop](https://www.docker.com/products/docker-desktop/) running and Git.

```bash
git clone https://github.com/willianfaes/events-manager-api.git
cd events-manager-api
cp .env.example .env
docker compose up -d --build
```

The values in `.env.example` work as they are: the Cognito ones are public, and the database password is only used inside your machine. The first build downloads the dependencies and compiles the app inside Docker, so it takes a few minutes; later starts are fast.

The command starts PostgreSQL, Redis, Kafka and the app, which waits for the other three. Run `docker compose ps` until `events-manager` shows `healthy`, then open [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) and [log in](#trying-the-api). The app creates 8 sample events on startup, so the listing is not empty.

To stop, run `docker compose down`. Add `-v` to also delete the data.

Optional extras, each with its own profile so the command above stays small:

| Command                                             | Adds                                                                         |
|-----------------------------------------------------|------------------------------------------------------------------------------|
| `docker compose --profile monitoring up -d --build` | Prometheus and Grafana, see [Monitoring](#monitoring-prometheus-and-grafana) |
| `docker compose --profile kafka-ui up -d kafka-ui`  | Kafka UI on `http://localhost:8090`, to watch the registration messages      |
| `docker compose --profile consumer up -d --build`   | The notification service, see below                                         |

### The notification service

The consumer lives in [its own repository](https://github.com/willianfaes/events-notification-service). To run it next to the API, clone it next to this one, so the compose file finds it at `../events-notification-service`, and start the `consumer` profile:

```bash
docker compose --profile consumer up -d --build
```

It keeps its own database, `events_notification_db`, in the same PostgreSQL container. The database is created only when the `postgres_data` volume is new; on an existing volume, create it once by hand:

```bash
docker compose exec db createdb -U <DB_USER> events_notification_db
```

## Local development (IDE)

You need JDK 21 and Docker. Copy the two example files; the password in `application-local.properties` must match `DB_PASSWORD` in `.env`, and both examples use the same one:

```bash
cp .env.example .env
cp src/main/resources/application-local.properties.example src/main/resources/application-local.properties
```

Run the database, the cache and Kafka in Docker, and the app from your IDE:

```bash
docker compose up -d db redis kafka
```

Then run the app from IntelliJ (or `./mvnw spring-boot:run`). The `local` profile is active by default, reads `application-local.properties` and creates 8 sample events. Without Redis the app still works: each listing logs a warning and goes to PostgreSQL. Without internet access it also works: events and profiles are saved without coordinates. Without Kafka it works too: registrations are saved and their messages wait in the outbox, with one warning per second, until the broker is back.

To watch the messages arrive, start Kafka UI (`docker compose --profile kafka-ui up -d kafka-ui`), open `http://localhost:8090` and register for an event in Swagger: within a second the message shows up in the `registration-events` topic, with the event id as its key.

> The IDE app and the Docker app use the same database volume, so `docker compose down -v` also wipes the data the IDE uses.

## Trying the API

The API validates tokens issued by an AWS Cognito user pool (`infra/cognito.tf`) and never sees a password. Every API route needs an **access token**.

### Swagger UI

Open `http://localhost:8080/swagger-ui.html`, click **Authorize** and, under `cognito`, leave `client_secret` empty, tick the scopes and log in as `demo@example.com` / `Demo1234!`. You can also use **Sign up** on the login page with your own e-mail (Cognito sends a confirmation code; check the spam folder).

A new account has no profile yet: `GET /users/me` answers 404 until `POST /users/me` creates it. With a profile, try `GET /events?maxDistanceKm=50`: each event then shows its `distanceKm`, measured from the address in the profile.

The demo account is shared and has no admin rights: it can browse and register, but not create events, and its profile is read-only (`PUT /users/me` answers 403; sign up to try it with your own). The login button only works on `localhost:8080` and the deployed domain, the two callbacks the pool accepts.

### From a terminal

The same calls work with `curl`, against your machine or the live demo:

```bash
BASE=http://localhost:8080   # or https://events.williansoares.dev

# 1. Log in on Swagger UI (above), run any request and copy the token after "Bearer" from the
#    curl it shows. It expires after an hour
TOKEN='paste-the-access-token-here'

# 2. Create the profile once: 201, or 409 if the account already has one, which is fine.
#    It also geocodes the postal code, city and country for the distance filter
curl -s -i -X POST "$BASE/users/me" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"Demo User","age":30,"address":{"street":"Via del Corso 1","city":"Rome","postalCode":"00186","countryCode":"IT"}}'

# 3. List the upcoming events: paginated, with optional filters and sorting
curl -s -H "Authorization: Bearer $TOKEN" "$BASE/events?size=3&sort=DATE_ASC"

# 3b. Only the events up to 200 km from the address in the profile, each with its distanceKm
#     (404 without a profile, 409 if its address could not be located: send the body of step 2
#     with PUT /users/me to look it up again. The demo profile has fixed coordinates and is read-only)
curl -s -H "Authorization: Bearer $TOKEN" "$BASE/events?maxDistanceKm=200"

# 4. Read one event (needs jq to take the id from the list)
EVENT_ID=$(curl -s -H "Authorization: Bearer $TOKEN" "$BASE/events?size=1" | jq '.content[0].id')
curl -s -H "Authorization: Bearer $TOKEN" "$BASE/events/$EVENT_ID"

# 5. Register: 201, or 409 if this account is already in or the event is full
curl -s -i -X POST -H "Authorization: Bearer $TOKEN" "$BASE/events/$EVENT_ID/registrations"

# 6. Cancel: 204, and 404 on a second call because no active registration is left
curl -s -i -X DELETE -H "Authorization: Bearer $TOKEN" "$BASE/events/$EVENT_ID/registrations"
```

The listing filters are `name` (part of the name), `from` and `to` (dates as `yyyy-MM-dd`, `from` defaults to today) and `maxDistanceKm` (a positive number, measured from the address in the profile). `sort` is `DATE_ASC`, `DATE_DESC`, `NAME_ASC` or `NAME_DESC`, and `size` is capped at 50. The listing answers a page (values shortened):

```json
{
  "content": [
    { "id": 1, "name": "...", "category": "CONCERT", "schedule": "2026-12-05T21:00:00",
      "address": { "...": "..." }, "description": "...", "capacity": 50, "remainingSpots": 50 }
  ],
  "page": 0, "size": 3, "totalElements": 8, "totalPages": 3, "last": false
}
```

Errors share one shape, for example a second registration for the same event:

```json
{
  "timestamp": "2026-11-01T10:00:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "You are already registered for this event!",
  "path": "/events/1/registrations"
}
```

## Tests

What each kind of test proves is in [ARCHITECTURE.md](ARCHITECTURE.md#testing-strategy).

```bash
./mvnw test                                        # all tests; Docker must be running because the containers start from the tests
./mvnw -B clean verify                             # what CI runs: tests plus the JaCoCo report in target/site/jacoco/index.html
./mvnw org.pitest:pitest-maven:mutationCoverage    # mutation testing on the unit tests, report in target/pit-reports
```

### SonarQube on your machine (optional)

CI publishes to SonarQube Cloud. To analyze a change before pushing, run the Community Edition locally. It sits in the `tools` profile, so a plain `docker compose up` does not start it.

```bash
docker compose --profile tools up -d sonarqube
```

The first boot takes a minute or two. Open [http://localhost:9000](http://localhost:9000), log in with `admin` / `admin` (it asks you to change the password), create a project with the key `events-manager-api` and generate a **Project Analysis Token**. Then, in PowerShell:

```powershell
$env:SONAR_TOKEN = "your-token-here"
.\mvnw.cmd clean verify sonar:sonar -Dsonar.token=$env:SONAR_TOKEN
```

`sonar.projectKey` and `sonar.host.url` are already in `pom.xml`. The token is a credential: never commit it.

## Monitoring (Prometheus and Grafana)

The `monitoring` profile adds Prometheus and Grafana next to the app:

```bash
docker compose --profile monitoring up -d --build
```

| Service         | URL                                     | Notes                                                    |
|-----------------|-----------------------------------------|----------------------------------------------------------|
| API and Swagger | `http://localhost:8080/swagger-ui.html` | send requests here                                       |
| Grafana         | `http://localhost:3000`                 | user `admin`, password from `GRAFANA_PASSWORD` in `.env` |
| Prometheus      | `http://localhost:9090`                 | targets, queries and the alert state                     |

The **Events Manager** dashboard is provisioned from `observability/grafana/dashboards/`. The top row answers "is it fast?": p95 latency per route on the server, p95 and p99 as k6 sees them, and the listing cache hit ratio. Three collapsed rows below answer "why not?": **Traffic and errors** (scrape target, request rate, 5xx ratio, p50), **JVM and connection pool** (heap, GC pause time, Hikari) and **k6 load generator** (request rate, failed requests, virtual users). The `HighP95Latency` rule (p95 above 500 ms for 2 minutes) is in `observability/prometheus/alerts.yml`; it only changes state, nothing is notified.

- Prometheus scrapes the `events-manager` **container** (port 8081, internal to the compose network). An app started from the IDE does not show up in Grafana.
- The Grafana password is only applied when its volume is created; to change it later, remove the `grafana_data` volume.
- After editing the dashboard JSON, restart the `grafana` container so it reloads the file.

## Load test (k6)

A [k6](https://k6.io) script in `load-test/k6/script.js` exercises two things:

- **Registration race:** 200 distinct users try to register for the same event, which has 50 spots, at the same time. The thresholds require exactly 50 `201` and 150 `409`, so the run fails if a spot is oversold.
- **Browsing:** `GET /events` (random page) and `GET /events/{id}` at a constant 50 iterations per second for 2 minutes. The thresholds are an error rate under 1% and a p95 under 500 ms.

> The 200 users are real Cognito users whose password is not public, so the test only runs against a pool you control. To run it yourself, create a pool from `infra/` in your own AWS account and point `.env` to it.

The users and the `load-test` app client they sign in through exist only while `load_test_password` is set in `infra/terraform.tfvars`, because that client accepts a password sent straight to the Cognito API, which the Swagger client refuses. Create them with `terraform apply -target=aws_cognito_user_pool_client.load_test -target=aws_cognito_user.load_test -target=aws_cognito_user_in_group.load_test` and remove them after the session (see `infra/README.md`). In `.env`, the same password goes in `LOAD_TEST_PASSWORD` and `terraform output load_test_client_id` in `LOAD_TEST_CLIENT_ID`, next to `COGNITO_ISSUER_URI`. Before the test starts, k6 logs each user in through the Cognito API and creates its profile with `POST /users/me`, so the run needs internet access.

Run the seed before each session: the first run creates the test events (1,000 events plus the 50-spot one), and later runs move them forward again, so the race event never falls in the past:

```bash
docker compose --profile monitoring up -d --build
docker compose exec -T db psql -U <DB_USER> -d <DB_NAME> < load-test/seed.sql
docker compose run --rm k6
```

On PowerShell, use `Get-Content load-test/seed.sql | docker compose exec -T db psql -U <DB_USER> -d <DB_NAME>` for the second line. The k6 metrics are pushed to Prometheus, so the top row of the Grafana dashboard shows the client p95 next to the server p95, and the **k6 load generator** row shows the rest of the client side.

To measure without the cache, set `CACHE_TYPE=none` in `.env`, not only in the shell: `docker compose run k6` recreates `events-manager` with the current environment, so a value exported for one command is lost on the next.

## Reference

### Environment variables

Docker Compose reads them from `.env` and passes them to the containers.

| Variable                                     | Required                           | Default in `.env.example`                 | What it does                                                                                                                 |
|----------------------------------------------|------------------------------------|-------------------------------------------|------------------------------------------------------------------------------------------------------------------------------|
| `DB_NAME`, `DB_USER`                         | yes                                | `events_manager_db`, `events_manager_app` | Name and user of the PostgreSQL database                                                                                     |
| `DB_PASSWORD`                                | yes                                | `changeme`                                | Password of that user. Keep the same value in `application-local.properties` when you run the app from an IDE                |
| `COGNITO_ISSUER_URI`                         | yes                                | the project's pool                        | Issuer of the Cognito user pool. The app does not start without it. It is a public URL, not a secret                         |
| `COGNITO_HOSTED_UI_URL`, `COGNITO_CLIENT_ID` | no                                 | the project's pool                        | Add the login button to Swagger UI                                                                                           |
| `CACHE_TYPE`                                 | no                                 | `redis`                                   | `redis` caches the event listing, `none` turns the cache off, for example to measure without it                              |
| `GRAFANA_PASSWORD`                           | only with the `monitoring` profile | `changeme`                                | Password of the Grafana `admin` user, applied when its volume is created                                                     |
| `LOAD_TEST_PASSWORD`                         | only for the k6 load test          | `changeme`                                | Password of the Cognito load-test users                                                                                      |
| `LOAD_TEST_CLIENT_ID`                        | only for the k6 load test          | empty                                     | Id of the `load-test` app client, from `terraform output load_test_client_id`                                                |

In production the database and Cognito values come from AWS Parameter Store: the deploy script writes them to the host's `.env`, so no secret is baked into the image or stored in the repository.

### Cognito settings

The same three values, by where the app runs. They are public, and `terraform output` in `infra/` prints them:

| Value             | `application-local.properties` (IDE)                   | `.env` (Docker)         | Project's pool                                                         |
|-------------------|--------------------------------------------------------|-------------------------|------------------------------------------------------------------------|
| Issuer (required) | `spring.security.oauth2.resourceserver.jwt.issuer-uri` | `COGNITO_ISSUER_URI`    | `https://cognito-idp.eu-north-1.amazonaws.com/eu-north-1_3VrOILR4Y`    |
| Hosted login page | `app.cognito.hosted-ui-url`                            | `COGNITO_HOSTED_UI_URL` | `https://events-manager-164106395660.auth.eu-north-1.amazoncognito.com` |
| Swagger client id | `springdoc.swagger-ui.oauth.client-id`                 | `COGNITO_CLIENT_ID`     | `v4gdb6t181545k46nd74lpi9t`                                            |

> If startup fails with `Error processing condition on ...jwtDecoderByIssuerUri`, the issuer is missing.

### Compose commands

| Command                                 | Description                                                   |
|-----------------------------------------|---------------------------------------------------------------|
| `docker compose up -d`                  | Start the stack (uses existing images if already built)       |
| `docker compose up -d --build`          | Rebuild images and start (use after code changes)             |
| `docker compose down`                   | Stop and remove containers; `-v` also deletes the volumes     |
| `docker compose logs -f events-manager` | Follow the app's logs                                         |
| `docker compose up -d db redis kafka`   | Start only the dependencies (for local development in an IDE) |

### Commit conventions

Commits and pull request titles follow [Conventional Commits](https://www.conventionalcommits.org/): `type(scope): short summary`, with the types `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `build`, `ci` and `perf`. The scope is optional, and the summary has 1 to 72 characters.

The `.githooks/commit-msg` hook rejects a subject line that does not follow it. Git does not copy hooks with a clone, so enable it once per clone:

```bash
git config --local core.hooksPath .githooks
```
