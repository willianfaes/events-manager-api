# Events Manager API
An API to manage local events and user registrations based on location relevance.

## Core Features (MVP)
- User authentication with AWS Cognito (OIDC tokens)
- Event CRUD
- Event categories (party, sport, concert, cultural)
- Event Registration (Join/Leave events)

## Running with Docker

This project ships with a multi-stage `Dockerfile` and a `docker-compose.yml` that spins up both the app and its PostgreSQL database.

### Commit Conventions

This repository follows [Conventional Commits](https://www.conventionalcommits.org/) to keep commit history consistent and readable.

**Format:** `type(scope): short summary`

**Types:** `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `build`, `ci`, `perf`
**Scope:** optional and free-form (lowercase letters, digits, `.`, `_`, `/`, `-`), usually the feature or area touched, such as `events`, `users`, `infra` or `build`. The summary after `: ` has 1 to 72 characters.

**Examples:**
```
feat(events): add name and date range filter to GET /events
fix(users): add id tie-breaker to RegistrationSort
test(infra): share one Postgres container across the tests
docs: clean up the repository root
```

**Setup (run once per clone):**

```bash
git config --local core.hooksPath .githooks
git update-index --chmod=+x .githooks/commit-msg
```

- The `.githooks/commit-msg` hook validates the subject line against the convention above and rejects non-conforming commits. It skips merge, revert, `fixup!`/`squash!`, and initial commits automatically.
- This setup is local to each clone — Git doesn't sync hooks or config automatically, so anyone working on this repo needs to run the commands above once after cloning.
- Enforcement is local-only (bypassable with `git commit --no-verify`); a CI check on PR titles/commits is a natural next step if stricter enforcement is needed.

## Code quality

This project uses [SonarQube](https://www.sonarsource.com/products/sonarqube/) (Community Edition, self-hosted) for static code analysis.

### Running SonarQube locally

Start the SonarQube server (runs under the `tools` profile, so it won't start with a regular `docker compose up -d` or `docker compose up -d db`):

```bash
docker compose --profile tools up -d sonarqube
```

First boot takes a minute or two. Once ready, open [http://localhost:9000](http://localhost:9000) and log in (default `admin` / `admin`, you'll be prompted to change it on first login).

Create a project (key: `events-manager-api`) and generate a **Project Analysis Token** under your account's Security settings. Tokens are shown once — copy it immediately.

### Running an analysis

Set the token for your session, then run the build with the `sonar` goal:

```powershell
$env:SONAR_TOKEN = "your-token-here"
.\mvnw.cmd clean verify sonar:sonar -Dsonar.token=$env:SONAR_TOKEN
```

`sonar.projectKey` and `sonar.host.url` are already configured in `pom.xml`, so no need to pass them on the command line.

Results are available at [http://localhost:9000/dashboard?id=events-manager-api](http://localhost:9000/dashboard?id=events-manager-api).

> ⚠️ The token is a credential — never commit it or share it. Regenerate it if it's ever exposed.

### Prerequisites
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running

### Setup

1. Copy the example environment and properties files, and adjust values as needed:
```bash
   cp .env.example .env
   cp src/main/resources/application-local.properties.example src/main/resources/application-local.properties
```
> ⚠️ Replace `DB_PASSWORD` in `.env` with a real value, and set the **same** password in `application-local.properties`. Docker Compose reads `.env` when creating the Postgres container; Spring Boot (when run outside Docker) reads `application-local.properties` and has no visibility into `.env`. These two must match, or you'll hit an authentication error. `.env` also needs the Cognito values (see [Logging in](#logging-in-aws-cognito)) and, for the monitoring profile, `GRAFANA_PASSWORD`.

2. Build and start the stack:
```bash
   docker compose up -d --build
```
This builds the app image (Maven build → `eclipse-temurin:21-jre-alpine` runtime) and starts both the `db` and `events-manager` containers. The app waits for the database to report healthy before starting.

3. The API will be available at `http://localhost:8080`. Check that the app is ready with `docker compose ps`: `events-manager` shows `healthy` once `/actuator/health` answers. The health and metrics endpoints run on the management port (8081), which is not published to the host.

### Useful commands

| Command | Description |
|---|---|
| `docker compose up -d` | Start the stack (uses existing images if already built) |
| `docker compose up -d --build` | Rebuild images and start (use after code changes) |
| `docker compose down` | Stop and remove containers |
| `docker compose logs -f events-manager` | Follow the app's logs |
| `docker compose up -d db` | Start only the database (useful for local development in an IDE) |
| `docker compose --profile monitoring up -d --build` | Start the stack with Prometheus and Grafana |

### Monitoring (Prometheus and Grafana)

The `monitoring` profile adds Prometheus and Grafana next to the app:

```bash
docker compose --profile monitoring up -d --build
```

| Service | URL | Notes |
|---|---|---|
| API and Swagger | `http://localhost:8080/swagger-ui.html` | send requests here |
| Grafana | `http://localhost:3000` | user `admin`, password from `GRAFANA_PASSWORD` in `.env` |
| Prometheus | `http://localhost:9090` | targets, queries and the alert state |

The **Events Manager** dashboard is provisioned from `observability/grafana/dashboards/` and shows request rate and 5xx ratio, p50 and p95 latency per route, JVM heap, GC pause time and the Hikari pool. The `HighP95Latency` rule (p95 above 500 ms for 2 minutes) is in `observability/prometheus/alerts.yml`; it only changes state, nothing is notified.

- Prometheus scrapes the `events-manager` **container** (port 8081, internal to the compose network). An app started from the IDE does not show up in Grafana.
- The Grafana password is only applied when its volume is created; to change it later, remove the `grafana_data` volume.
- After editing the dashboard JSON, restart the `grafana` container so it reloads the file.

### Load test (k6)

A [k6](https://k6.io) script in `load-test/k6/script.js` exercises two things:

- **Registration race:** 200 distinct users try to register for the same event, which has 50 spots, at the same time. The thresholds require exactly 50 `201` and 150 `409`, so the run fails if a spot is oversold.
- **Browsing:** `GET /events` (random page) and `GET /events/{id}` at a constant 50 iterations per second for 2 minutes. The thresholds are an error rate under 1% and a p95 under 500 ms.

The 200 users are real Cognito users: create them once with `load_test_password` in `infra/terraform.tfvars` and `terraform apply -target=aws_cognito_user.load_test -target=aws_cognito_user_in_group.load_test` (see `infra/README.md`), and put the same password in `LOAD_TEST_PASSWORD` in `.env`, next to `COGNITO_ISSUER_URI` and `COGNITO_CLIENT_ID`. Before the test starts, k6 logs each one in through the Cognito API and creates its profile with `POST /users/me`, so the run needs internet access.

The Docker database starts empty, so create the test events once (1,000 events plus the 50-spot one):

```bash
docker compose --profile monitoring up -d --build
docker compose exec -T db psql -U <DB_USER> -d <DB_NAME> < load-test/seed.sql
docker compose run --rm k6
```

On PowerShell, use `Get-Content load-test/seed.sql | docker compose exec -T db psql -U <DB_USER> -d <DB_NAME>` for the second line. The k6 metrics are pushed to Prometheus, so the **k6 (client)** panels of the Grafana dashboard show the client side next to the server side.

#### Baseline, before any cache

Fresh volumes, seed, one warm-up run (discarded) and three measured runs, on the final image (Java 21):

| Browsing, 100 requests/s for 2 min | Median of 3 runs | Range |
|---|---|---|
| p50 | 5.2 ms | 5.17 – 5.21 ms |
| p95 | 6.6 ms | 6.62 – 6.65 ms |
| p99 | 10.0 ms | 9.93 – 10.53 ms |
| Error rate | 0% (0 of 12,002) | — |

The race gave 50 created and 150 rejected in every run, and the database held exactly 50 active registrations.

**Environment:** Intel Core i5-7400 (4 cores), 16 GB RAM, Windows 10, Docker Desktop with 4 CPUs and 7.7 GB. The app, the database, Prometheus, Grafana and k6 all run on this one machine, so the load generator competes with the app.

**What this is not:** a synthetic test on a small dataset (1,000 events) with the generator on the same host. The numbers are for comparing a change against this baseline (the cache, in #72), not a statement about how many users the API serves.

### Local development without Docker for the app

You can run the database in Docker while running the Spring Boot app directly from your IDE for faster iteration and debugging:

```bash
docker compose up -d db
```

Then run the app normally from IntelliJ (or `./mvnw spring-boot:run`) — the `local` Spring profile is active by default, reading your password from `application-local.properties` (see Setup step 1 above). No IDE-specific run configuration is needed.

### Logging in (AWS Cognito)

The API validates tokens issued by an AWS Cognito user pool (`infra/cognito.tf`) and never sees a password. It does not start without the issuer; the other two values only add the login button to Swagger UI.

| Value | `application-local.properties` | `.env` (Docker) |
|---|---|---|
| Issuer (required) | `spring.security.oauth2.resourceserver.jwt.issuer-uri` | `COGNITO_ISSUER_URI` |
| Hosted login page | `app.cognito.hosted-ui-url` | `COGNITO_HOSTED_UI_URL` |
| Swagger client id | `springdoc.swagger-ui.oauth.client-id` | `COGNITO_CLIENT_ID` |

The project's pool (public values, not secrets; `terraform output` in `infra/` prints the same):

```
issuer:    https://cognito-idp.eu-north-1.amazonaws.com/eu-north-1_3VrOILR4Y
hosted UI: https://events-manager-164106395660.auth.eu-north-1.amazoncognito.com
client id: v4gdb6t181545k46nd74lpi9t
```

In Swagger UI (`http://localhost:8080/swagger-ui.html`), click **Authorize** and, under `cognito`, leave `client_secret` empty, tick the scopes and log in as `demo@example.com` / `Demo1234!`. Or paste a token from the CLI command in `infra/README.md` under `bearerAuth`. The login button only works on `localhost:8080`, the only callback the pool accepts.

> If startup fails with `Error processing condition on ...jwtDecoderByIssuerUri`, the issuer is missing.
