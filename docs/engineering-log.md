# Engineering log

Chronological record of the audit, fixes, refactors and test work. Commits are on `main`; every phase below shipped as exactly one commit. All commits are pushed as of `e9e125d`.

## Phase index

| Phase | Commit | Scope |
| --- | --- | --- |
| Baseline | `8bebea0` | Imported working tree before the audit |
| P0 | `3147054` | IDORs, unauthenticated webhooks, OTP exposure, JWT hardening |
| P1 | `ad1f06b` | Payment matching, balance tracking, inventory deduction, status filters |
| P2 | `ecb6f93` | Auth/lead API gaps, Shopify signature, DTO contracts, error mapping |
| P3 | `82098c3` | Config, schema integrity, entity semantics, dead code |
| F1 | `0bb908c` | Frontend runtime blockers and backend data contracts |
| F2 | `2bedda5` | Permission-gated UI and refresh-token flow |
| F3 | `fdf3e48` | Dead UI removal, notification bell consolidation |
| E1 | `845b798` | N+1 removal, SQL report aggregate, hot-path indexes |
| E2 | `cfa8724` | Auth cache, batched payroll, trigram indexes, SSE heartbeat |
| B | `f942d0c` | Order state machine, paid-status proof, SMS after commit, V37 |
| E3 | `cf55376` | Lazy routes, shared reference data, server-side filtering |
| E4 | `b25ab67` | Orders/CRM module split, ESLint, first backend tests, CI |
| T1-T4 | `cc94551` | Backend integration, frontend unit, e2e, CI matrix |
| T5 | `e9e125d` | CI Node 24 correction |

## P0 - security (`3147054`)

### Authorization (IDOR)

- `GET /api/v1/auth/user/{id}` now allows ADMIN/MANAGER or the user themselves; staff asking for another id get 403.
- `GET /api/v1/orders/staff/{staffId}/summary` follows the same self-or-elevated rule.
- `GET /api/v1/payroll/staff/{staffId}` accepts `CAN_VIEW_OWN_PAYROLL` or `CAN_VIEW_PAYROLL_SUMMARY` and applies the same ownership check.
- `GET /api/v1/auth/staffs` restricted to ADMIN/MANAGER.
- Notification read endpoints scope every query to the authenticated user; marking another user's notification is rejected.
- Mutation endpoints received missing `@PreAuthorize` guards (logout, logout-check, user deletion).

### Webhooks

- WPForms lead creation (`POST /api/v1/leads`) requires `webhook.wpforms.secret`, supplied as `?secret=` or `X-WPForms-Secret`. Comparison is constant-time (`MessageDigest.isEqual`). Unconfigured secret returns 503; wrong secret returns 403.
- KCB IPN (`POST /api/v1/webhooks/kcb/ipn`) requires `kcb.ipn.secret`, supplied as `?token=` or `X-KCB-IPN-Secret`, same status rules.
- The JWT filter skips `/api/v1/webhooks` so these handlers can authenticate by shared secret instead of a bearer token.

### Login, OTP and tokens

- OTP values are no longer written to logs; phone numbers are masked. OTP storage uses `ConcurrentHashMap`, `SecureRandom`, six digits, 5-minute expiry, 3 attempts and a 60-second resend cooldown, with expired entries purged.
- Login is uniform: unknown email and wrong password both return 401 without revealing which failed. Soft-deleted users cannot log in.
- Refresh tokens carry `type=refresh`; access tokens carry `type=access`. The refresh endpoint rejects access tokens and validates the token against the stored value and expiry.
- JWT secrets must decode or fall back to at least 32 bytes, otherwise startup fails loudly.
- Registration validates required fields, lowercases email, rejects duplicates and seeds per-role permission rows in one transaction.
- Admin seeding moved behind `SEED_ADMIN_EMAIL`, `SEED_ADMIN_PASSWORD`, `SEED_ADMIN_PHONE`; no default credentials remain.

## P1 - money paths (`ad1f06b`)

- Order creation used the pre-delivery-fee total for `balance`; it now uses `finalTotal`. Counter orders start fully paid with zero balance and deduct stock immediately.
- Payments are validated (`amount > 0`), duplicate `transactionRef` values are rejected, and cancelled/returned orders refuse new payments.
- Matching an existing payment now rejects payments that are already matched, suspicious, or not successful.
- `paidAmount` and `balance` are updated together; balance is clamped at zero. A payment no longer forces `DISPATCHED_PAID`/`DISPATCHED_PARTIAL`; status changes go through `OrderService` so stock moves exactly once.
- A fully-paid SMS is sent only on the transition into a fully paid state.
- KCB IPN: duplicate `transactionRef` is ignored, cancelled orders store the payment unmatched, and balance/status use the same shared logic as manual payments.
- Inventory `increaseIfTracked`/`reduceIfTracked` replace unconditional stock changes, so products without an inventory row are not stock-tracked but tracked products still block insufficient stock.
- Revenue and COGS use `OrderStatus.isRevenueRecognized()` instead of a status allowlist. Soft-deleted orders are excluded.
- Staff summary only counts revenue-recognized orders for revenue, COGS and average order value; commission delegates to `PayrollService` so the KPI rule has one implementation.
- Payroll filters now use fully paid statuses (`DISPATCHED_PAID`, `DELIVERED_PAID`, `COMPLETED`).

## P2 - API contracts (`ecb6f93`)

- Added `RefreshTokenRequest` and a refresh endpoint.
- Added response DTOs that stop entity leakage and stabilize the API: `UserSummary`, `ExpenseResponse`, `RequisitionResponse`, `ProductLookupResponse`, updated `LeadResponse`, `PaymentResponse`, `OrderResponse`, `OrderItemResponse`.
- `GlobalExceptionHandler` now returns RFC 7807 `ProblemDetail` bodies with consistent status mapping (400/401/403/404/409/500).
- Leads: action-level permissions, safe conversion notes, follow-up/assignment fields, soft-delete filters, search and pagination.
- Shopify webhook verifies `X-Shopify-Hmac-Sha256` over the raw body with HMAC-SHA256, requires an `orders/*` topic, rejects malformed JSON and returns 503 when the secret is not configured.
- Products, couriers, categories, staff logs and requisitions received missing permission checks, soft-delete filters and matching DTOs.

## P3 - configuration and schema (`82098c3`)

- `application.yml`: Flyway enabled with `baseline-on-migrate`, `ddl-auto` defaults to `none`, pageable maximum is 500, CORS origins and every secret come from environment variables.
- `pom.xml` aligned with Spring Boot 4.0.2 / Jackson 3, H2 and security-test dependencies, and test-scoped configuration.
- Added `V34__schema_integrity.sql`:
  - installs `pgcrypto` for `gen_random_uuid()`;
  - reconciles `leads.productoptions` into `product_options`;
  - adds `notifications.deleted_at`;
  - adds missing foreign keys as `NOT VALID` so legacy rows do not block startup;
  - creates unique indexes on `payments.transaction_ref` and `customers.phone` only when no duplicates exist, otherwise logs a notice;
  - adds supporting indexes for leads, notifications, payments, order items and orders.
- Fixed Lombok semantics on entities (`@EqualsAndHashCode`/`toString` scope) and removed dead DTOs, services and controller methods.
- Removed hardcoded credentials and debug output from services.

## F1 - frontend contracts (`0bb908c`)

- Fixed runtime blockers on Expense, Inventory, Payment, Product and CRM pages caused by mismatched response shapes.
- Added backend support the UI already expected: customer lookup, inventory endpoints, requisition response mapping, lead conversion notes, product lookup for order lines and the corrected order response.
- Normalized axios error handling so the UI shows server messages instead of undefined.

## F2 - permissions and session (`2bedda5`)

- Navigation is generated from `navItems` and filtered by permission keys; routes and in-page actions are gated the same way.
- Requisition approval/rejection/payment buttons and staff-log actions respect permissions.
- Added the refresh-token flow in `api.js`:
  - one in-flight refresh shared by queued 401 responses;
  - auth endpoints never enter the refresh retry loop;
  - a failed refresh clears the session and redirects to `/login`;
  - an expired access token is still considered authenticated while a refresh token exists.
- Removed the redundant `logoutHandler` module.
- Backend staff-logs permission aligned with the UI.

## F3 - interface cleanup (`fdf3e48`)

- Removed dead date filters and the unsupported Card tab.
- Dashboard uses a real date range instead of a hardcoded one.
- StaffLogs and Category pagination corrected.
- Notification bell moved into the shared top bar so it exists on every page; duplicate CRM bell logic removed.

## E1 - query efficiency (`845b798`)

- Order list: page UUIDs first, then fetch the page's orders with items and products in one query, removing the per-order N+1.
- Profit report: one aggregate query for revenue and COGS (`sumRevenueAndCogs`) plus one expense aggregate, replacing per-order iteration.
- Added `V35__performance_indexes.sql` for orders, order items, staff logs, requisitions, payments, expenses and products.
- `spring.data.web.pageable.max-page-size: 500` caps client page sizes.

## E2 - repeated work and streaming (`cfa8724`)

- `AuthCache`: 60-second cache of user details and permissions, with eviction on user deletion, permission changes and other mutations. Authorities still come from the database, so a grant applies without re-login.
- Staff summary aggregates moved into SQL; all-staff payroll uses grouped weekly queries instead of one query per staff per week; staff assignment counts use projection queries.
- Added `V36__trigram_search_indexes.sql`: installs `pg_trgm` inside a guarded block and creates GIN trigram indexes only when the extension is available.
- Notification SSE supports multiple emitters per user (tabs), a scheduled heartbeat comment, and cleanup on completion/timeout/error.
- The bell falls back to a 5-minute unread poll if the stream drops.

## Backend follow-up (`f942d0c`)

- `OrderStatus` became the single source of truth for lifecycle rules: revenue recognition, fully-paid, delivered/closed, dispatched family, `afterPayment` and `canTransitionTo`.
- Manual status updates validate transitions. Marking an order `DISPATCHED_PAID` or `DELIVERED_PAID` requires `paidAmount >= totalAmount`, which closes the commission loophole.
- Same-status updates are no-ops. Cancelling a previously dispatched order returns stock. `RETURNED_GOOD` returns stock once.
- Counter orders are created with `paidAmount = total` and `balance = 0`; order items are attached to the saved order so the response includes them.
- Order reads use a fetch-join query for items and products.
- Payments no longer advance dispatch state on their own; they only adjust paid/balance and possibly `DISPATCHED_PARTIAL`/`DISPATCHED_PAID` for orders already dispatched.
- Order SMS events are `@Async` `@TransactionalEventListener(AFTER_COMMIT)` handlers, so messages are not sent for rolled-back transactions.
- Lead follow-up scheduler handles missing/expired follow-ups without rescheduling endlessly.
- Added `V37__orders_account_number_unique.sql`: unique index on `orders.account_number` (payment reference), skipped with a notice if duplicates already exist.

## E3 - loading and filtering (`cf55376`)

- Every route is lazy-loaded behind `Suspense`, splitting the bundle.
- `referenceData.js` shares product/category/staff lookups across pages with a 2-minute cache and explicit invalidation after mutations.
- List filters moved server-side with debounced queries for orders, products, payments, staff, categories and couriers.
- Order search covers account number, city, notes, customer name/phone and product name; product search covers SKU and name.

## E4 - structure and first tests (`b25ab67`)

- `Orders.jsx` split into `orders/OrderModals.jsx`, `orderUtils.js`, `orderStyles.js` and `orders.css`; state stays in the parent page.
- `crm.jsx` split into `crm/LeadList.jsx`, `AddLeadModal.jsx`, `ConvertLeadModal.jsx`, `StatCard.jsx`, `Toast.jsx` and `crmUtils.js`.
- Added ESLint flat config with react-hooks and react-refresh rules.
- First backend unit tests: `JwtServiceTest`, `AuthCacheTest`, `PayrollServiceImplTest`, `ReportServiceImplTest`.
- First CI workflow: backend `mvnw test`, frontend lint and build.

## T1-T5 - test layers and CI (`cc94551`, `e9e125d`)

- Backend integration harness using `@SpringBootTest(webEnvironment = RANDOM_PORT)` and the JDK `HttpClient`, with an H2 test profile and seeded users/tokens. 23 integration tests across authentication, authorization and webhook security.
- Frontend unit tests: 25 tests across six files using Vitest, jsdom, Testing Library and axios-mock-adapter.
- Playwright end-to-end suite: 14 journeys for login, route protection, orders and CRM, with a mocked API.
- CI expanded to three jobs with artifacts; frontend CI moved to Node 24 because Vitest 5 and jsdom 31 require Node 22.22+.

## Defects found by the new tests

1. **Profit report failed on every call (400).** `OrderItemRepository.sumRevenueAndCogs` declared `Object[]` for a JPQL query returning two aggregate columns, producing a nested array; parsing it as a number threw `Character array is missing "e" notation`. Changed the repository to `List<Object[]>` and the service reads the first row.
2. **Register returned 401 instead of 403 for non-admins.** A filter-level `hasRole("ADMIN")` rule on `/api/v1/auth/register` handled denial before method security, and the entry point returns 401. Removing the filter rule lets `@PreAuthorize` produce the intended 403.
3. **Unpaid dispatches were highlighted as paid in the UI.** `getParcelAgeHighlight` matched statuses with `includes("PAID")`, which also matches `DISPATCHED_UNPAID`. It now compares exact status values.
4. **The e2e suite rendered a blank app.** The route glob `**/api/**` also intercepted Vite module requests such as `/src/api/authApi.js`. Replaced with a URL predicate on `pathname.startsWith("/api/")`.
5. **Frontend CI failed at unit tests on Node 20.** `vitest@5.1.0` requires `^22.12 || ^24 || >=26` and `jsdom@31` requires `>=22.22`. CI now uses Node 24.

## Migrations added

| Migration | Purpose |
| --- | --- |
| `V34__schema_integrity.sql` | pgcrypto, column reconciliation, missing FKs (NOT VALID), guarded unique indexes |
| `V35__performance_indexes.sql` | b-tree indexes for list/report/dashboard paths |
| `V36__trigram_search_indexes.sql` | guarded `pg_trgm` GIN indexes for `lower(...) LIKE '%term%'` searches |
| `V37__orders_account_number_unique.sql` | unique account number, guarded against existing duplicates |

"Guarded" means the migration logs a notice and skips the index instead of failing the deployment when legacy data or extensions make it impossible.

## Known limitations and next steps

- **Migrations are not exercised by tests.** The H2 profile disables Flyway and uses `create-drop`, so V34-V37 must be validated against a real PostgreSQL copy before deploying.
- **OTP storage is in-process.** Restarting or running multiple instances loses OTPs and cooldowns. Redis is the intended replacement.
- **SMS is not tested end to end.** Integration tests use a blank phone number so OTP generation succeeds without calling Zettatel.
- **E2E uses a mocked API.** It verifies UI flows and client contract assumptions, not the real backend.
- **One refresh token per user.** A new login invalidates older refresh tokens; refresh does not rotate the token.
- **Frontend unit coverage excludes pages, layout and components.** Coverage focuses on utilities, auth and the API layer.
- **Lint has 11 pre-existing `exhaustive-deps` warnings.** They are warnings only; the build and CI are green.
