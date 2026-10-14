# Events Manager API
An API to manage local events and user registrations based on location relevance.

See ROADMAP.md for the full data model, endpoint list, and future plans.

## Core Features (MVP)
- User Authentication (JWT)
- Event CRUD
- Event categories (party, sport, concert, cultural)
- User interest selection across multiple categories
- Event Registration (Join/Leave events)

## Running with Docker

This project ships with a multi-stage `Dockerfile` and a `docker-compose.yml` that spins up both the app and its PostgreSQL database.

### Commit Conventions

This repository follows [Conventional Commits](https://www.conventionalcommits.org/) to keep commit history consistent and readable.

**Format:** `type(scope): short summary`

**Types:** `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `build`, `ci`, `perf`
**Scopes:** `auth`, `events`, `users`, `core`, `docs`, `test`, `chore`

**Examples:**
```
feat(auth): add login endpoint
fix(events): prevent duplicate registrations
test(auth): add token service tests
docs(readme): clarify docker setup
```

**Setup (run once per clone):**

```bash
git config --local commit.template .gitmessage.txt
git config --local core.hooksPath .githooks
git update-index --chmod=+x .githooks/commit-msg
```

- The `.gitmessage.txt` template prefills the commit message — in the CLI and in IntelliJ's Commit tool window automatically, no plugin required.
- The `.githooks/commit-msg` hook validates the subject line against the convention above and rejects non-conforming commits. It skips merge, revert, `fixup!`/`squash!`, and initial commits automatically.
- This setup is local to each clone — Git doesn't sync hooks or config automatically, so anyone working on this repo needs to run the three commands above once after cloning.
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
> ⚠️ Replace `DB_PASSWORD` in `.env` with a real value, and set the **same** password in `application-local.properties`. Docker Compose reads `.env` when creating the Postgres container; Spring Boot (when run outside Docker) reads `application-local.properties` and has no visibility into `.env`. These two must match, or you'll hit an authentication error. `.env` also needs `JWT_SECRET` (at least 32 bytes, the app refuses to start without it) and, for the monitoring profile, `GRAFANA_PASSWORD`.

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

### Local development without Docker for the app

You can run the database in Docker while running the Spring Boot app directly from your IDE for faster iteration and debugging:

```bash
docker compose up -d db
```

Then run the app normally from IntelliJ (or `./mvnw spring-boot:run`) — the `local` Spring profile is active by default, reading your password from `application-local.properties` (see Setup step 1 above). No IDE-specific run configuration is needed.
