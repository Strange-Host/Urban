# Testing and CI

The project has four verification layers: backend unit tests, backend integration tests, frontend unit tests and browser end-to-end tests. All of them run in CI.

## Summary

| Layer | Tooling | Tests | Location |
| --- | --- | --- | --- |
| Backend unit | JUnit 6, Mockito, AssertJ | 20 | `urban-pos-dev/src/test/java/.../{Security,Service}` |
| Backend integration | `@SpringBootTest(RANDOM_PORT)`, JDK `HttpClient`, H2 | 23 | `urban-pos-dev/src/test/java/.../Controller` |
| Frontend unit | Vitest 5, jsdom, Testing Library, axios-mock-adapter | 25 | `urban-fronend-main/src/**/*.test.{js,jsx}` |
| End-to-end | Playwright 1.65, Chromium | 14 | `urban-fronend-main/e2e` |

Backend total: 43. Frontend unit total: 25. E2E total: 14.

## Backend tests

### Running

```bash
cd urban-pos-dev
./mvnw test
./mvnw -Dtest=ReportServiceImplTest test
```

- Maven Surefire runs every class ending in `Test`, so unit and integration tests run together.
- Integration tests need no external services: they use an in-memory H2 database in PostgreSQL compatibility mode and a random HTTP port.

### Unit tests

| Class | Tests | Covers |
| --- | --- | --- |
| `JwtServiceTest` | 5 | access vs refresh typing, validity/expiry, weak secret rejection |
| `AuthCacheTest` | 5 | TTL lookup, eviction, ADMIN vs granted-permission authorities |
| `PayrollServiceImplTest` | 6 | weekly KPI threshold, 50/30 commission, multi-week ranges, batched all-staff output |
| `ReportServiceImplTest` | 3 | aggregate conversion, zero/missing totals, net profit arithmetic |
| `UrbanTrendApplicationTests` | 1 | Spring context loads |

### Integration tests

`support/IntegrationTestBase` provides:

- `@SpringBootTest(webEnvironment = RANDOM_PORT)` and `@ActiveProfiles("test")`;
- autowired repositories, `PasswordEncoder`, `JwtService` and `AuthCache`;
- `createUser(role, permissions...)` with a unique email and all permission rows seeded with the requested granted flags;
- `bearer(user)` which evicts the auth cache and generates a real access token;
- `get`/`postJson` helpers using the JDK `HttpClient` against `http://localhost:{port}`.

The test profile (`src/test/resources/application-test.yml`):

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:urban;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1
  flyway:
    enabled: false
  jpa:
    hibernate:
      ddl-auto: create-drop
jwt:
  secret: <test-only 32-byte Base64 value>
webhook:
  wpforms:
    secret: test-wpforms-secret
kcb:
  ipn:
    secret: test-kcb-secret
```

| Class | Tests | Covers |
| --- | --- | --- |
| `AuthenticationIntegrationTest` | 7 | unknown email and wrong password both 401; valid login with a blank phone returns `OTP_SENT` without calling SMS; malformed email 400; refresh rejects an access token; register 403 for staff; register 401 anonymous |
| `AuthorizationIntegrationTest` | 9 | anonymous 401; cashier without `CAN_VIEW_ORDERS` 403; manager with the permission 200; `size=1000` capped to 500; invalid status enum 400; profit report 403/200 by permission; staff directory role gate; notifications scoped to the user |
| `WebhookSecurityIntegrationTest` | 7 | WPForms 403 without/wrong secret, valid secret persists a lead, duplicate `wpEntryId` ignored; KCB 403 without/wrong secret, incomplete payload 400 |

The blank-phone trick keeps the login test deterministic: `MessagingService` skips SMS when no valid phone exists, so no network call is attempted.

## Frontend unit tests

### Running

```bash
cd urban-fronend-main
npm test                # vitest run
npm run test:watch
npm run test:coverage
```

Node 24 is required (Vitest 5 supports `^22.12 || ^24 || >=26`; jsdom 31 requires `>=22.22`).

### Setup

`vitest.config.js`:

- `environment: "jsdom"`;
- `setupFiles: ["./src/test/setup.js"]`, which imports `@testing-library/jest-dom/vitest` and runs Testing Library cleanup plus `localStorage.clear()` after every test;
- includes `src/**/*.{test,spec}.{js,jsx}`, excludes `e2e`, `node_modules`, `dist`;
- v8 coverage reporting to `coverage/`, limited to utilities, auth and API modules (pages/layout/components are excluded intentionally).

| File | Tests | Covers |
| --- | --- | --- |
| `src/api/api.test.js` | 4 | single-flight refresh and replay; failed refresh clears session; no refresh token; login never retried |
| `src/auth/useAuth.test.jsx` | 4 | no token, undecodable token, authorities from claims, authorities serialized as objects |
| `src/auth/PermissionGate.test.jsx` | 3 | permission grant, missing permission fallback, role-only gate |
| `src/pages/orders/orderUtils.test.js` | 8 | money/date formatting, status colours, parcel age highlight, product lines |
| `src/pages/crm/crmUtils.test.js` | 4 | stat tones, date formatting, phone links, product option JSON |
| `src/pages/crm/StatCard.test.jsx` | 2 | label/value rendering and missing icon handling |

`api.test.js` uses `vi.resetModules()` with dynamic imports so every test gets a fresh axios instance and interceptor state. It also resets the URL to `/login` before each test so the redirect branch does not attempt a jsdom navigation.

## End-to-end tests

### Running

```bash
cd urban-fronend-main
npm run test:e2e
npx playwright test e2e/orders.spec.js
npx playwright test --headed
npx playwright show-report
```

`playwright.config.js` starts the Vite dev server on `http://127.0.0.1:4173` (`--strictPort`), reuses an existing server locally, and uses Chromium Desktop Chrome. CI adds one retry, two workers, an HTML report and a first-retry trace.

### Mocking

`e2e/support.js` intercepts API traffic so the journeys need no backend:

- `makeToken`, `ADMIN_TOKEN`, `CASHIER_TOKEN` produce unsigned JWTs with realistic claims and authorities;
- `signIn(page, token)` seeds `localStorage` before the app boots;
- `mockApi(page, options)` fulfills orders, leads, products, staff, notifications, couriers, profit report and user lookups, with overrides for login and OTP responses;
- `ORDER` and `LEAD` are shared fixtures.

The route matcher is a URL predicate, **not** `"**/api/**"`. The glob also matches Vite's own modules under `/src/api/` and leaves the application blank; the predicate checks `pathname.startsWith("/api/")`.

### Specs

| Spec | Tests | Covers |
| --- | --- | --- |
| `login.spec.js` | 4 | empty-form validation, rejected sign-in, OTP redirect, OTP to dashboard |
| `auth-guard.spec.js` | 3 | signed-out redirect, signed-in shell, signed-in user not sent back to login |
| `orders.spec.js` | 4 | order list, create-order modal, status filter, cashier navigation gating |
| `crm.spec.js` | 3 | lead table, add-lead modal, tab filter |

Selector conventions used in these specs:

- `getByRole` first (`heading`, `button`, `cell`, `link`);
- `getByLabel("Password", { exact: true })` because the show/hide button's accessible name contains "Password";
- responsive pages render both a table and a card list, so assertions target the visible variant (for example `getByRole("cell")` on the desktop viewport) or use `.filter({ visible: true })`.

## CI pipeline

`.github/workflows/ci.yml` runs on pushes to `main` and on every pull request. A concurrency group cancels superseded runs.

| Job | Steps |
| --- | --- |
| Backend unit and integration tests | checkout, Temurin Java 21 with Maven cache, `./mvnw -B test`, upload `surefire-reports` on failure |
| Frontend lint, unit tests and build | checkout, Node 24 with npm cache, `npm ci`, `npm run lint`, `npm test`, `npm run build` |
| End-to-end tests | checkout, Node 24, `npm ci`, `npx playwright install --with-deps chromium`, `npm run test:e2e`, upload `playwright-report` and `test-results` (always) |

Notes:

- The backend job uses Java 21 while the project compiles with `java.version=17`. The code targets 17 and CI uses a newer LTS to run the build.
- The frontend jobs must use Node 24. Node 20 fails at the unit-test step because Vitest 5 and jsdom 31 declare Node 22.22+.
- E2E needs Chromium and downloads it per run; the npm cache does not cover browsers.
- Playwright artifacts are uploaded even when the job is cancelled, with 7-day retention.

Latest verified run: workflow `CI` run `43242644239` on commit `e9e125d`, all three jobs green.

## Local environment caveats

- **Offline Maven build:** run with `JAVA_HOME` pointing at a modern JDK (the workspace bundles JDK 25). The shell default may be JRE 1.8 and will fail.
- **Avoid `./mvnw clean` offline:** the local repository may not contain `maven-clean-plugin`; without `clean`, `mvnw test` still recompiles changed sources.
- **Flyway is not tested:** the H2 profile disables it, so migrations V34-V37 are never executed by tests.
- **E2E starts its own server:** do not run it against a port already occupied by another Vite instance unless you intend to reuse it.
- **No live PostgreSQL or SMS provider:** integration tests are hermetic; deployment-time behaviour of migrations and messaging must be verified separately.

## Troubleshooting

| Symptom | Cause | Fix |
| --- | --- | --- |
| Playwright app is blank | API route glob also blocked Vite modules | match `pathname.startsWith("/api/")` |
| `strict mode violation: getByLabel('Password')` | label text and the show-password button's `aria-label` both match | pass `{ exact: true }` |
| Element found but hidden | desktop table and mobile card list both render | use the visible variant (`getByRole("cell")`) or `.filter({ visible: true })` |
| CI unit tests fail on Node 20 | Vitest 5/jsdom 31 engine requirements | use Node 24 |
| Frontend build works, e2e fails to load modules | stale Vite cache or blocked esbuild postinstall | remove `node_modules/.vite`, reinstall dependencies |
| Backend offline build fails immediately | JDK 1.8 in PATH or offline `clean` requested | set `JAVA_HOME` to JDK 25 and omit `clean` |

## Known gaps

- No backend end-to-end test with a real browser or real PostgreSQL.
- E2E mocks the API, so backend contract changes can drift until integration tests or manual checks catch them.
- Background work (SMS delivery, lead reminder scheduler, SSE heartbeat timing) is not asserted by automated tests.
- Frontend coverage intentionally ignores pages, layout and components; UI behaviour is covered by e2e instead.
