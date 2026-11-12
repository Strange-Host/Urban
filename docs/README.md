# Urban POS documentation

This directory records what the system does today and everything that was changed during the hardening and testing effort (2026-11-12). It is written against the code as of commit `e9e125d`.

## Repository layout

| Path | Contents |
| --- | --- |
| `urban-pos-dev/` | Spring Boot REST API, Flyway migrations, JUnit test suite |
| `urban-fronend-main/` | React SPA, Vitest unit tests, Playwright end-to-end tests |
| `.github/workflows/ci.yml` | Three-job CI pipeline |
| `docs/` | This documentation |

## Stack

| Layer | Technology |
| --- | --- |
| Backend framework | Spring Boot 4.0.2, Java 17 language level |
| Persistence | Spring Data JPA / Hibernate 7, PostgreSQL 17, Flyway |
| Security | Spring Security 7, JWT (jjwt), BCrypt |
| JSON | Jackson 3 (`tools.jackson`) |
| API docs | Springdoc 3 / Swagger UI |
| Frontend | React 19, Vite 7.3.1, React Router 7, Tailwind CSS 4, axios |
| Backend tests | JUnit 6, Mockito 5, AssertJ, H2 in PostgreSQL mode |
| Frontend tests | Vitest 5 + jsdom + Testing Library, Playwright 1.65 |

## Quick start

### Backend

Required environment variables (no defaults for secrets):

```bash
PGHOST=localhost PGPORT=5432 PGDATABASE=urban PGUSER=postgres PGPASSWORD=...
JWT_SECRET=<at least 32 bytes, base64 or raw>
JWT_ACCESS_EXPIRATION=900000        # milliseconds
JWT_REFRESH_EXPIRATION=604800000    # milliseconds
CORS_ALLOWED_ORIGINS=http://localhost:5173
```

Optional but needed for full functionality:

```bash
WPFORMS_WEBHOOK_SECRET=...
SHOPIFY_WEBHOOK_SECRET=...
KCB_IPN_SECRET=...
KCB_CONSUMER_KEY=... KCB_CONSUMER_SECRET=... KCB_BASE_URL=...
ZETTATEL_API_KEY=... ZETTATEL_USER=... ZETTATEL_PASS=...
SEED_ADMIN_EMAIL=... SEED_ADMIN_PASSWORD=... SEED_ADMIN_PHONE=...
```

The first admin is not hardcoded. If no admin exists and the `SEED_ADMIN_*` variables are absent, startup logs a warning and creates nothing.

```bash
cd urban-pos-dev
JAVA_HOME=/path/to/jdk ./mvnw -o test    # offline; see docs/testing-and-ci.md
JAVA_HOME=/path/to/jdk ./mvnw spring-boot:run
```

### Frontend

```bash
cd urban-fronend-main
npm ci
npm run dev          # http://localhost:5173
```

`VITE_API_BASE_URL` defaults to `http://localhost:8080/api/v1`.

## Verification status

Verified locally on 2026-11-12 and in CI run `43242644239` (commit `e9e125d`):

| Layer | Result |
| --- | --- |
| Backend unit + integration | 43 / 43 passing |
| Frontend unit | 25 / 25 passing |
| Frontend lint | 0 errors, 11 pre-existing `exhaustive-deps` warnings |
| Frontend production build | passing |
| Playwright end-to-end | 14 / 14 passing |
| CI jobs (backend, frontend, e2e) | all green |

The e2e suite runs against a mocked API; it does not exercise a live backend. See "Known gaps" in the testing guide.

## Documentation index

- [Engineering log](engineering-log.md) - every phase, root cause, fix and commit.
- [Backend security and business rules](backend-security-and-domains.md) - current behaviour reference.
- [Testing and CI](testing-and-ci.md) - test architecture, commands, pipeline, troubleshooting.

## Quick command reference

```bash
# Backend
cd urban-pos-dev && ./mvnw test                       # unit + integration
cd urban-pos-dev && ./mvnw -Dtest=ReportServiceImplTest test

# Frontend
cd urban-fronend-main
npm run lint
npm test                       # Vitest, single run
npm run test:watch
npm run test:coverage
npm run build
npm run test:e2e               # Playwright, starts Vite on port 4173
npx playwright test e2e/orders.spec.js --headed
```
