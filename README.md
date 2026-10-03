[English](README.md) | [简体中文](README.zh-CN.md)

# E-Library Service

A small REST service for a digital library: browse books, look up a title, borrow
a copy, return it, and see your current loans.

This repository contains the implementation of the backend take-home assignment,
including the source code, a design document (`docs/design.md`) and this README.

## Scope

The five required features are implemented end to end:

| Feature | Endpoint |
| --- | --- |
| Browse books (paged) | `GET /api/v1/books` |
| Book details | `GET /api/v1/books/{bookId}` |
| Borrow a book | `POST /api/v1/books/{bookId}/loans` |
| Return a book | `POST /api/v1/loans/{loanId}/return` |
| Current loans | `GET /api/v1/users/me/loans` |

Deliberately out of scope: registration/login/OAuth, roles, due dates and fines,
reservations, e-book file storage, a front end, and book/user management
endpoints. Books and users come from seeded demo data, so the service is useful
immediately after `mvn spring-boot:run` without any setup.

## Tech stack

| Area | Choice |
| --- | --- |
| Language | Java 17 |
| Application | Spring Boot 3.3.5 (Web / MVC) |
| Build | Maven |
| Persistence | Spring Data JPA / Hibernate 6.5 with `hibernate-community-dialects` (SQLite dialect) |
| Database | SQLite via `sqlite-jdbc` 3.46 |
| Migrations | Flyway 10 (schema versioning + repeatable seed) |
| Validation | Jakarta Bean Validation |
| API docs | Springdoc OpenAPI 2.6 (Swagger UI) |
| Tests | JUnit 5, Mockito, Spring Boot Test, MockMvc |
| Demo UI (optional) | Vue 3 + Vite + Vue Router |

Requires **JDK 17+** and **Maven 3.9+**. No external database or container is
needed. The optional demo UI additionally needs Node 18+ (Node 20 LTS recommended).

## Quick start

```bash
mvn spring-boot:run
```

The service listens on `http://localhost:8080`. On first start Flyway creates
`data/library.db`, applies the schema and loads the demo data. Interactive API
documentation is at:

- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

Or run the packaged jar:

```bash
mvn clean package
java -jar target/e-library-service-0.1.0.jar
```

## Demo front end (optional)

A small Vue single-page UI lives in `frontend/` for clicking through the five
features. It is an **extra deliverable**: the assignment only asked for the
backend, and the service is fully usable without it.

```bash
# terminal 1 - backend
mvn spring-boot:run

# terminal 2 - front end
cd frontend
npm install
npm run dev
```

Then open http://localhost:5173 . The Vite dev server proxies `/api` to
`http://localhost:8080`, so browser requests stay same-origin and the backend
needs no CORS configuration. Use the switcher in the top bar to act as either
demo user.

The UI covers the book list with paging, book details, borrow, my loans, return,
and a shared banner that renders the backend's `error.code` / `error.message`
(for example "暂无库存" when borrowing the out-of-stock book 6).

**Optional single-jar delivery.** To serve the UI from Spring Boot instead of the
dev server:

```bash
cd frontend
npm install
npm run build:single      # builds into ../src/main/resources/static
cd ..
mvn spring-boot:run       # http://localhost:8080/ now serves the UI too
```

The generated assets are git-ignored and are never part of the Maven build, so
`mvn clean package` still works on a machine without Node installed.

## Identity simulation (`X-User-Id`)

There is no authentication. The current user is taken from the **`X-User-Id`**
request header, which must be a positive integer referring to a seeded user.
Endpoints that need a user reject missing, non-numeric or non-positive values
with `400 VALIDATION_ERROR`, and an unknown user with `404 USER_NOT_FOUND`.

This keeps the assignment focused on the domain and API design. A real system
would take the user from an authenticated context instead; the service-layer
ownership checks would stay exactly the same.

## Demo data

Seeded by `R__seed_demo_data.sql` (repeatable and idempotent — re-running never
duplicates or overwrites rows).

Users:

| id | name |
| --- | --- |
| 1 | Alice Chen |
| 2 | Brian Lee |

Books:

| id | title | author | total | available |
| --- | --- | --- | --- | --- |
| 1 | Clean Code | Robert C. Martin | 3 | 3 |
| 2 | The Pragmatic Programmer | Andrew Hunt, David Thomas | 2 | 2 |
| 3 | Designing Data-Intensive Applications | Martin Kleppmann | 4 | 4 |
| 4 | Refactoring | Martin Fowler | 1 | 1 |
| 5 | Domain-Driven Design | Eric Evans | 2 | 2 |
| 6 | Working Effectively with Legacy Code | Michael C. Feathers | 1 | 0 |

Book 6 starts out of stock so `409 BOOK_UNAVAILABLE` can be reproduced without
borrowing anything first.

## API examples

All responses are JSON. The base path is `/api/v1`.

### 1. Browse books

```bash
curl "http://localhost:8080/api/v1/books?page=0&size=3"
```

```json
{
  "items": [
    {
      "id": 1,
      "title": "Clean Code",
      "author": "Robert C. Martin",
      "isbn": "9780132350884",
      "totalCopies": 3,
      "availableCopies": 3
    }
  ],
  "page": 0,
  "size": 3,
  "totalElements": 6,
  "totalPages": 2
}
```

`page` is zero-based (default `0`); `size` defaults to `20` and must be between
`1` and `100`. Results are ordered by `id` so paging is stable.

### 2. Book details

```bash
curl "http://localhost:8080/api/v1/books/1"
```

```json
{
  "id": 1,
  "title": "Clean Code",
  "author": "Robert C. Martin",
  "isbn": "9780132350884",
  "description": "A handbook of agile software craftsmanship.",
  "totalCopies": 3,
  "availableCopies": 3,
  "createdAt": "2026-01-05T08:05:00Z"
}
```

An unknown id returns `404 BOOK_NOT_FOUND`.

### 3. Borrow a book

```bash
curl -X POST "http://localhost:8080/api/v1/books/1/loans" -H "X-User-Id: 1"
```

```json
{
  "id": 1,
  "bookId": 1,
  "userId": 1,
  "borrowedAt": "2026-10-03T11:05:28.958Z",
  "returnedAt": null
}
```

Returns `201 Created` and decrements `availableCopies` by one. A user may hold
several copies of the same title.

### 4. Return a book

```bash
curl -X POST "http://localhost:8080/api/v1/loans/1/return" -H "X-User-Id: 1"
```

```json
{
  "id": 1,
  "bookId": 1,
  "userId": 1,
  "borrowedAt": "2026-10-03T11:05:28.958Z",
  "returnedAt": "2026-10-03T11:05:29.354Z"
}
```

Restores one copy. Returned records are kept for history; they simply stop being
"active". Only the owning user may return a loan.

### 5. Current loans

```bash
curl "http://localhost:8080/api/v1/users/me/loans" -H "X-User-Id: 1"
```

```json
{
  "items": [
    {
      "id": 1,
      "bookId": 1,
      "bookTitle": "Clean Code",
      "bookAuthor": "Robert C. Martin",
      "borrowedAt": "2026-10-03T11:05:28.958Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

`status` defaults to `active` and is the only supported value. Each open loan is
returned as its own entry — including several copies of the same title. The
identity always comes from the header, never from a query parameter.

## Error model

Every error uses the same envelope, which makes client handling predictable:

```json
{
  "error": {
    "code": "BOOK_UNAVAILABLE",
    "message": "Book 6 has no copies available."
  }
}
```

`details` is added when a specific parameter or header is at fault:

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "The X-User-Id header must be provided.",
    "details": [
      { "field": "X-User-Id", "reason": "must be provided" }
    ]
  }
}
```

| Code | HTTP | Meaning |
| --- | --- | --- |
| `VALIDATION_ERROR` | 400 | Invalid parameter or header |
| `FORBIDDEN` | 403 | The loan belongs to another user |
| `BOOK_NOT_FOUND` | 404 | Unknown book |
| `USER_NOT_FOUND` | 404 | Unknown user |
| `LOAN_NOT_FOUND` | 404 | Unknown loan |
| `NOT_FOUND` | 404 | Unknown route or resource |
| `METHOD_NOT_ALLOWED` | 405 | HTTP method not supported by the route |
| `BOOK_UNAVAILABLE` | 409 | No copies are currently available |
| `LOAN_ALREADY_RETURNED` | 409 | The loan was already returned |
| `SERVICE_UNAVAILABLE` | 503 | Database busy (e.g. `SQLITE_BUSY`); retry later |
| `INTERNAL_ERROR` | 500 | Unexpected server error |

Stack traces, SQL text and file paths are never returned to clients. A database
busy error is mapped to `503`, never to `409 BOOK_UNAVAILABLE`, so clients can
tell "try again" apart from "the shelf is empty".

## Build and test

```bash
mvn clean test        # unit + integration tests (uses its own SQLite file)
mvn clean package     # runs tests and builds the executable jar
```

The suite covers the service rules with Mockito (44 tests overall) and the HTTP
contract with MockMvc against a real SQLite database, including:

- browsing, detail and pagination validation;
- borrow/return happy paths and every documented error code;
- the stock invariant (`0 <= availableCopies <= totalCopies`) after borrows and returns;
- repeat returns not restoring stock twice;
- `NOT NULL`, `UNIQUE(isbn)`, `CHECK` and foreign-key constraints being enforced by SQLite;
- a deterministic concurrency test: 8 borrowers race for 2 copies and exactly 2 succeed;
- the OpenAPI document and Swagger UI being reachable.

Tests never touch `data/library.db`; they use `target/e-library-it.db`, which is
rebuilt from the migrations.

## Project structure

```text
src/main/java/com/example/elibrary/
├── ELibraryApplication.java
├── config/          # Clock, identity argument resolver, MVC + OpenAPI config
├── controller/      # HTTP routing and status codes
├── domain/          # Book, User, Loan entities and the Instant<->text converter
├── dto/response/    # Response models (entities are never serialized directly)
├── exception/       # Error codes, business exception, global error handler
├── repository/      # Spring Data JPA repositories incl. conditional updates
└── service/         # Transaction boundaries and business rules

src/main/resources/
├── application.yml
└── db/migration/
    ├── V1__create_schema.sql      # tables, checks, foreign keys, index
    └── R__seed_demo_data.sql      # idempotent demo data

src/test/java/com/example/elibrary/
├── service/         # Mockito unit tests
└── integration/     # MockMvc + real SQLite tests

frontend/            # optional Vue demo UI (see "Demo front end")

docs/design.md       # design document and review record
```

## Design decisions and trade-offs

The assignment says the thought process and trade-offs matter more than feature
completeness, so the reasoning is written down rather than left implicit. The short
version:

| Decision | Why | What it costs |
| --- | --- | --- |
| Layered monolith | Small domain; keeps the focus on responsibility boundaries | No independent scaling |
| SQLite with a one-connection pool | Zero setup for the reviewer | Reads serialize too; not built for write concurrency |
| Denormalized `availableCopies` + conditional update | The list must show availability, and overselling must be impossible | Redundant data, guarded by `CHECK` constraints and a concurrency test |
| `returnedAt` instead of a status column | "Status disagrees with the timestamp" becomes impossible | Cannot express renewals or lost copies |
| Borrowing deliberately not idempotent | Holding several copies of one title is legal, so a retry means "one more copy" | A client retry borrows an extra copy; would need `Idempotency-Key` |
| Identity simulated with `X-User-Id` | No auth requirement, but multi-user behaviour must be demoable | Not a security mechanism |
| Flyway owns the schema (`ddl-auto=none`) | SQLite's loose typing makes Hibernate's schema validation unreliable | No startup schema check; integration tests enforce the constraints instead |
| Demo UI through the Vite proxy by default | Same-origin, so the backend needs no CORS and no change | A split deployment would still need CORS |

The alternatives considered, the cost of each choice, and the conditions that would
make me change my mind are tabulated in [docs/design.md](docs/design.md) (§16).
Deliberately out of scope: search/filter, book and user management endpoints, and
authentication.

## Possible next steps

- `Idempotency-Key` support for borrow requests.
- Optional keyword/availability filters on `GET /api/v1/books`.
- Pagination links (RFC 8288) instead of raw page numbers.
- A real authentication provider replacing `X-User-Id`.
- Migrating from SQLite to a server database if write concurrency matters.
