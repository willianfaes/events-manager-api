# Events Manager API
An API to manage local events and user registrations based on location relevance.

## Core Features (MVP)
- User Authentication (JWT)
- Event Management (CRUD with location details)
- Event Registration (Join/Leave events)
- Filtering events by City and Country

## Running with Docker

This project ships with a multi-stage `Dockerfile` and a `docker-compose.yml` that spins up both the app and its PostgreSQL database.

### Prerequisites
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running

### Setup

1. Copy the example environment file and adjust values as needed:
```bash
   cp .env.example .env
```
   > ⚠️ Replace `DB_PASSWORD` with a real value before running — the database container will not start with an empty password. This is fine for local development, but should never be reused for a real deployment.

2. Build and start the stack:
```bash
   docker compose up -d --build
```
   This builds the app image (Maven build → jlink custom runtime → minimal Alpine image) and starts both the `db` and `events-manager` containers. The app waits for the database to report healthy before starting.

3. The API will be available at `http://localhost:8080`.

### Useful commands

| Command | Description |
|---|---|
| `docker compose up -d` | Start the stack (uses existing images if already built) |
| `docker compose up -d --build` | Rebuild images and start (use after code changes) |
| `docker compose down` | Stop and remove containers |
| `docker compose logs -f events-manager` | Follow the app's logs |
| `docker compose up -d db` | Start only the database (useful for local development in an IDE) |

### Local development without Docker for the app

You can run the database in Docker while running the Spring Boot app directly from your IDE for faster iteration and debugging:

```bash
docker compose up -d db
```

Then run the app normally from IntelliJ — it will fall back to `localhost:5432` per the defaults in `application.properties`.
