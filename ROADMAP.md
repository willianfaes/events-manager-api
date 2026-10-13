# ROADMAP

## Vision

Events Manager is a location-aware API for discovering, organizing, and joining local events. It is meant for people who want a clean, practical way to manage event attendance and for the developer building a portfolio backend that feels real rather than toy-like.

The original motivation was simple: instead of another generic demo app, build something grounded in everyday city life and useful enough to show to real engineers and recruiters - including the Turin context that made the project feel worth doing in the first place.

## MVP Scope

The MVP is the core backend for users, events, and registrations.

### Core entities
- `User`
- `UserAddress`
- `Event`
- `EventAddress`
- `EventRegistration`
- `Category` (table, referenced by `Event` and by users via `UserInterest`)
- `UserInterest` (join entity between `User` and `Category`)

### Core features
- user sign-up and login with JWT
- role-based access control (USER, ADMIN) enforced via @PreAuthorize
- create and read events
- register and unregister a user for an event
- registration respects event capacity (max/available spots), enforced
transactionally to prevent overselling under concurrent requests
- user selects one or more categories they're interested in
- soft delete behavior for the main domain entities where the schema already supports it
- event location data stored directly with the event
- basic filtering/search foundations as the API grows

### Explicit boundaries
- no live third-party ingestion
- no Maps API
- no scraping external event sources
- no recommendation engine yet
- no social graph or chat features yet
- no age-based filtering (interests are self-selected by category only)

Those belong in Future Vision, not in the MVP.

## Entities & Relationships

Think of the data model like this:

- A `User` has exactly one `UserAddress`.
- An `Event` has exactly one `EventAddress`.
- A `User` can have many `EventRegistration` records.
- An `Event` can have many `EventRegistration` records.
- Each `EventRegistration` links exactly one `User` to exactly one `Event`.
- `Category` is its own table (id, name) — the single source of truth for valid categories, replacing the old hardcoded CHECK constraint.
- An `Event` references exactly one `Category`.
- A `User` can be interested in many `Category` values, and each `Category` can have many interested `User`s, via the `UserInterest` join table.
- `EventRegistration` is the join entity that represents participation, with soft delete support so a user can leave and later rejoin.
- `Event` carries a maximum capacity and a derived/tracked available-spots count; registration is only allowed while spots remain.
- `User` carries a role (`USER` or `ADMIN`); only `ADMIN` can create events.

In ER-diagram terms:

- `User 1 ── 1 UserAddress`
- `Event 1 ── 1 EventAddress`
- `Event * ── 1 Category`
- `User 1 ── * EventRegistration`
- `Event 1 ── * EventRegistration`
- `EventRegistration * ── 1 User`
- `EventRegistration * ── 1 Event`
- `User 1 ── * UserInterest`
- `Category 1 ── * UserInterest`
- `UserInterest * ── 1 User`
- `UserInterest * ── 1 Category`

## Endpoints

| Entity | Method | Path | Purpose |
|---|---|---|---|
| Auth | `POST` | `/auth/login` | Authenticate a user and return a JWT token response. |
| Auth | `POST` | `/auth/register` | Create a new user account. |
| Event | `GET` | `/events` | List events. |
| Event | `GET` | `/events/{id}` | Fetch a single event by ID. |
| Event | `POST` | `/admin/events` | Create a new event (admin only). |
(note: restricted to ADMIN, enforced via @PreAuthorize)
| Registration | `GET` | `/users/me/registrations?upcoming={bool}` | List the authenticated user's event registrations, paginated — upcoming or past. |
| Registration | `POST` | `/events/{eventId}/registrations` | Register the authenticated user for an event. |
| Registration | `DELETE` | `/events/{eventId}/registrations` | Unregister the authenticated user from an event. |
| User Interests | `GET` | `/users/me/interests` | List the authenticated user's selected categories. |
| User Interests | `PUT` | `/users/me/interests` | Set/replace the authenticated user's selected categories. |

## Future Vision (Post-MVP)

These ideas are intentionally parked until the MVP is stable:

- smart profiling / smarter event recommendations
- Maps integration for richer location UX
- live ingestion from third-party sources
- event discovery from external platforms
- automated matching based on location, category, and user behavior
- notifications and richer user engagement flows

Nothing here is deleted. It is just not in scope yet.

## Roadmap / Now-Next-Later

| Now | Next | Later |
|---|---|---|
| Stabilize the daily dev workflow, keep the build/test flow reliable, and finish the core auth + event + registration path. | Convert the test suite to be self-contained with Testcontainers, remove temporary script workarounds, and tighten the MVP API behavior. | Add Maps, live ingestion, recommendations, and other higher-ambition product layers once the core backend is solid. |
| Replace the hardcoded Ca<br/>tegory CHECK constraint with a proper `categories` table and add the `user_interests` join table so users can select multiple categories. | Add the missing API polish: validation, filtering, pagination, clearer error handling, and Swagger `@Operation`/`@ApiResponse` annotations documenting error codes per endpoint. | Expand into the broader future vision without forcing it into the MVP. |
| Finish soft-delete on `EventRegistration` to match the pattern already used on `User`, `Event`, and `EventAddress`. | Keep the roadmap and implementation in sync so future AI help always knows what comes next. | Treat this as the point where the project becomes a richer city-events platform rather than just a backend exercise. |
| — | Add role-based authorization (USER/ADMIN) with `@PreAuthorize` on event creation. | — |
| — | Add event capacity (max/available spots) with `@Transactional` registration to prevent race conditions on the last spot. | — |
| — | Add a paginated, sorted "my registrations" endpoint (upcoming vs. past). | — |
| — | Expand the global exception handler beyond duplicate-email to cover capacity and authorization errors with clean JSON responses. | — |