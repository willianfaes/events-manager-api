# Events Manager API

A Spring Boot API for event sign-ups. Admins publish events with a fixed number of spots; users sign in through AWS Cognito, find events near them and register, and the API keeps every event within its capacity even when many people register in the same second.

*My intention with this project is to practise what a backend needs in production, not only what makes it run: tests that prove the rules, numbers I can reproduce, decisions I can explain, and a real deploy I operate myself.*

**Live demo:** [Swagger UI](https://events.williansoares.dev/swagger-ui.html). Click **Authorize** and sign in with `demo@example.com` / `Demo1234!` (a public demo account with no admin rights), or create your own account on the login page.

[Live demo](https://events.williansoares.dev/swagger-ui.html) · [Project board](https://github.com/users/willianfaes/projects/1) · [Architecture](ARCHITECTURE.md) · [Run locally](RUNNING.md)

<!-- Hero GIF: Swagger login, register (201), then 409 when the event is full. Recorded last, with the stack live. -->

## Architecture

![Architecture of Events Manager API on AWS: Route 53 and an Application Load Balancer in front of one EC2 host running the API, PostgreSQL, Redis, Kafka and the notification service in Docker Compose, with Cognito for sign-in and GitHub Actions deploying through ECR and Systems Manager](docs/diagrams/architecture.drawio.svg)

Everything runs on one EC2 host on purpose, to keep the demo inside the free plan; [Why one EC2](ARCHITECTURE.md#why-one-ec2) says what that costs and what would change first. The whole AWS setup is Terraform in `infra/`, so after a teardown the stack comes back with `terraform apply` and one pipeline run. How each part works is in [ARCHITECTURE.md](ARCHITECTURE.md).

## How it was built

- **Planned on a board.** The work was planned as issues on the [project board](https://github.com/users/willianfaes/projects/1) and delivered as pull requests: 85 issues done, 12 closed as not planned and 111 pull requests merged (November 2026).
- **The rule before the code.** An issue states its rule with Given/When/Then cases, and the tests are written first. For example, [issue #94](https://github.com/willianfaes/events-manager-api/issues/94) has the capacity rule and its cases, concurrency included; [PR #104](https://github.com/willianfaes/events-manager-api/pull/104) closes it with the tests, the reason for the lock and a green build.
- **A readable history.** Conventional Commits, checked by a `commit-msg` hook, and squash merges, so each pull request is one commit on `main`.
- **Every change through CI.** The whole test suite, containers included, runs on every pull request in about two minutes. A merge to `main` also runs SonarQube Cloud and deploys to AWS.
- **Checking the tests themselves.** [PIT](https://pitest.org) changes the code (flips a condition, removes a call) and reruns the unit tests; a change that no test catches points to a weak test. It runs on demand, not in CI, and one of its findings is the second story in [What went wrong](#what-went-wrong-along-the-way).
- **Changing course in steps.** The API had its own login; the move to Cognito took three pull requests ([#157](https://github.com/willianfaes/events-manager-api/pull/157), [#158](https://github.com/willianfaes/events-manager-api/pull/158), [#159](https://github.com/willianfaes/events-manager-api/pull/159)), each one small enough to deploy on its own.

## What it does

| Feature                                                                      | How it is proven                                                                                                    |
|------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------|
| Registration never exceeds the capacity of the event                         | A concurrency integration test, and a [k6 race](RUNNING.md#load-test-k6): 200 users, 50 spots, exactly 50 `201` and 150 `409` |
| Sign-in with AWS Cognito (OIDC), roles taken from Cognito groups             | One test table with a row per route for the `401` and `403` rules                                                   |
| Event listing with filters, sorting and pagination, cached in Redis for 30 s | Integration tests, including one that pauses Redis and checks the fallback to PostgreSQL                            |
| Filter by distance from the address in the profile (`maxDistanceKm`)         | Tests on a real PostgreSQL, including one that checks the database and Java draw the line at the same distance      |
| External geocoding call with timeouts, cache, retry and a circuit breaker     | Tests with a fake provider that fails on purpose, including one that proves an open circuit makes no call           |
| Registrations and cancellations published to Kafka through an outbox         | An end-to-end test from the API to a real Kafka broker, and a test that a failed send leaves the messages pending |
| Metrics, dashboard and alert (Prometheus and Grafana)                        | Dashboard provisioned from the repository                                                                           |
| Deploy to AWS from every merge to `main`                                     | CI/CD workflow, with the image tagged by the commit SHA                                                             |

<!-- Numbers, one line: k6 race 50/150, browse p95 with and without the cache, coverage. Values pending (formal k6 run). Sonar badges when the repository is public. -->

## What went wrong along the way

- **Adding a repository test broke another one.** Each JPA test class built its own Spring context and connection pool until PostgreSQL ran out of connections. They now share one, and an ArchUnit rule fails the build if a new test class does not.
- **Tests passed without checking the result.** PIT showed that a registration could be saved without its user and event while every test stayed green. The tests now check what is saved and what is returned.

Each one with its root cause, fix and guard is in [ARCHITECTURE.md](ARCHITECTURE.md#what-went-wrong-along-the-way).

## Quickstart

```bash
cp .env.example .env              # works as it is
docker compose up -d --build      # API, PostgreSQL, Redis and Kafka, with 8 sample events
# then open http://localhost:8080/swagger-ui.html and log in as demo@example.com / Demo1234!
```

[RUNNING.md](RUNNING.md) covers running from the IDE, the example requests, the tests, the monitoring and the load test.

## Known gaps

- **One host, no backups.** Destroying the EC2 host erases the data; the first fix is a managed database.
- **Kafka is one broker.** It is not fault tolerant.
- **The listing can be up to 30 seconds stale.** Registering stays the source of truth and answers `409` when the event is full.

The full list, each gap with its fix, is in [ARCHITECTURE.md](ARCHITECTURE.md#known-gaps).

## Stack

| Area                   | Technology                                                                                                        |
|------------------------|-------------------------------------------------------------------------------------------------------------------|
| Language and framework | Java 21, Spring Boot 3.5 (Web, Data JPA, Validation, Security, Actuator)                                          |
| Authentication         | Amazon Cognito (OIDC), Spring Security OAuth2 resource server                                                     |
| Database               | PostgreSQL 16, schema versioned with Flyway, Hibernate with `ddl-auto=validate`                                   |
| Cache                  | Redis 7.4 through Spring Cache, with a fallback to PostgreSQL                                                     |
| External API           | Nominatim (OpenStreetMap) through `RestClient`, protected with Resilience4j                                       |
| Messaging              | Apache Kafka 3.9 (KRaft, one broker) through Spring for Apache Kafka, with a transactional outbox                 |
| Mapping and API docs   | MapStruct, Lombok, springdoc-openapi (Swagger UI)                                                                 |
| Observability          | Micrometer, Prometheus and Grafana, with a provisioned dashboard and one alert rule                               |
| Tests                  | JUnit 5, Mockito, AssertJ, Spring Security Test, Testcontainers (PostgreSQL, Redis and Kafka), WireMock, ArchUnit |
| Quality                | JaCoCo (coverage), PIT (mutation testing), SonarQube Cloud                                                        |
| Load test              | k6, with its metrics pushed to Prometheus                                                                         |
| Infrastructure as code | Terraform (`infra/`), state in S3                                                                                 |
| Cloud                  | AWS: EC2, Application Load Balancer, Route 53, ACM, Cognito, ECR, Systems Manager, Parameter Store                 |
| CI/CD                  | GitHub Actions, Docker multi-stage image                                                                          |

## License

[MIT](LICENSE)
