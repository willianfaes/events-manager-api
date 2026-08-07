# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands & Development Workflow

- **Build & Package:** `./mvnw clean install` (or `./mvnw clean install -DskipTests` to skip tests)
- **Run Application:** `./mvnw spring-boot:run`
- **Run Tests:** `./mvnw test`
- **Run a Single Test:** `./mvnw test -Dtest=ClassName` or `./mvnw test -Dtest=ClassName#methodName`
- **Database (Docker):** 
  - Start DB container: `docker compose up -d`
  - Reset DB and volumes: `docker compose down -v`
  - View DB logs: `docker compose logs -f`

## High-Level Architecture & Structure

- **Tech Stack:** Spring Boot 3.5.14, Java 25, PostgreSQL, Flyway, Spring Data JPA, Spring Security with JWT (Auth0), Lombok, and MapStruct.
- **Package-by-Feature Structure:** The application is organized into vertical feature packages under `dev.will.eventsmanager`:
  - `auth/`: Authentication logic, security filters, JWT tokens, DTOs, and controller.
  - `events/`: Event management, categories (`PARTY`, `SPORT`, `CONCERT`, `CULTURAL`), addresses, registrations, repositories, controllers, and services.
  - `users/`: User entities, repositories, and address mapping.
  - `core/`: Global exceptions (e.g., `BusinessException`).
- **Database Migrations:** Managed using Flyway with migration scripts located in `src/main/resources/db/migration/` (e.g., `V1__create_database_eventsmanager.sql`).
- **Security:** Spring Security filter chain configured via `SecurityConfig` and custom JWT filters (`SecurityFilter`) utilizing Auth0 JWT library.
