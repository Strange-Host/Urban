# Backend security and business rules

Current behaviour of the Spring Boot API. This is a reference, not a changelog; see [engineering-log.md](engineering-log.md) for how it got here.

## Request pipeline

1. CORS (`CorsConfig`) - origins come from `app.cors.allowed-origins`, credentials allowed, methods `GET/POST/PUT/DELETE/PATCH/OPTIONS`, all headers, exposed `Authorization`, `X-Sticker-Batch-Id`, `Content-Disposition`.
2. `JwtAuthenticationFilter` - stateless; runs before `UsernamePasswordAuthenticationFilter`.
3. Method security (`@EnableMethodSecurity`) on controllers.
4. `GlobalExceptionHandler` - RFC 7807 `ProblemDetail` responses.

CSRF is disabled because the API is stateless and authenticated by bearer tokens. Sessions are `STATELESS`.

## Authentication lifecycle

```
POST /api/v1/auth/login          { email, password }  -> 200 { message: "OTP_SENT" }
POST /api/v1/auth/verify-otp     { email, otp }       -> 200 { accessToken, refreshToken }
POST /api/v1/auth/refresh        { refreshToken }     -> 200 { accessToken, refreshToken }
POST /api/v1/auth/logout         (Bearer)             -> 200, clears refresh token + lastLogoutAt
POST /api/v1/auth/register       (ADMIN only)
```

- Login validates the password with BCrypt but issues no token; it sends a six-digit OTP to the stored phone number.
- OTP: 5-minute expiry, 3 verification attempts, 60-second resend cooldown, kept in memory (`OtpService`). Wrong or expired OTP returns 401.
- Verify-otp issues an access token (15 minutes by default, `JWT_ACCESS_EXPIRATION`) and a refresh token (7 days by default, `JWT_REFRESH_EXPIRATION`). The refresh token is stored on the user row and reused; it is not rotated.
- Access and refresh tokens carry a `type` claim. `JwtAuthenticationFilter` only accepts access tokens; `AuthService.refresh` only accepts refresh tokens and additionally checks the stored value and expiry.
- JWT secret: Base64-decoded when possible, otherwise raw; must be at least 32 bytes or startup fails.
- Soft-deleted users are excluded from login, refresh and token authentication.

### Token extraction

- `Authorization: Bearer <token>` for normal requests.
- `?access_token=<token>` only for `GET /api/v1/notifications/subscribe`, because `EventSource` cannot send headers.
- The filter skips `/api/v1/webhooks*` entirely; those endpoints authenticate by shared secret or HMAC.

### Authorities

The JWT contains `role` and `authorities` claims, but authorization does **not** trust them. The filter resolves the user by email and loads authorities from the database:

1. `AuthCache` is checked first (60-second TTL).
2. On a miss, `UserRepository` loads the user and `UserPermissionRepository` loads granted permissions.
3. ADMIN receives every `Permission`; every other role receives only permissions whose `granted` flag is true.

Permission changes apply within one cache TTL without forcing a re-login. Code that mutates a user or permissions must call `AuthCache.evict(email)`; user deletion already does.

## Roles and permissions

`UserRole`: `ADMIN`, `MANAGER`, `STAFF`, `CASHIER`, `ATTENDANT`.

- ADMIN: all permissions.
- MANAGER: all operational permissions (leads, orders, products, categories, customers, inventory, payments, requisitions, expenses, payroll summary, profit report, staff view).
- STAFF: leads, order creation/status, product/category/customer view, requisitions, own payroll.
- CASHIER: order creation/view, product/category/customer view, payments, requisitions, own payroll.
- ATTENDANT: no default permissions (explicit grants only).

`Permission` groups: products, categories, orders, leads, customers, inventory, payments, requisitions, expenses, payroll, reports, staff/admin, staff logs and couriers. See `Enum/Permission.java` for the full list.

### Key endpoint guards

| Endpoint | Guard |
| --- | --- |
| `POST /api/v1/orders` | `CAN_CREATE_ORDERS` |
| `GET /api/v1/orders`, `GET /orders/{id}` | `CAN_VIEW_ORDERS` |
| `PATCH /orders/{id}/status` | `CAN_UPDATE_ORDER_STATUS` |
| `PATCH /orders/{id}/notes`, `/courier` | `CAN_UPDATE_ORDER` |
| `GET /orders/staff/{id}/summary` | `CAN_VIEW_ORDER_SUMMARY` + self-or-elevated |
| `GET /orders/dashboard/top-selling` | `CAN_VIEW_TOP_SELLING` |
| `POST /api/v1/payments` | `CAN_CREATE_PAYMENTS` |
| `GET /payments`, `/payments/unmatched` | `CAN_VIEW_PAYMENTS` |
| `GET /payments/order/{id}` | `CAN_VIEW_ORDER_PAYMENTS` |
| `POST /payments/{paymentId}/match/{orderId}` | `CAN_MATCH_PAYMENTS` |
| `POST /api/v1/leads` | shared secret (webhook) |
| `GET/PATCH leads` | `CAN_VIEW_LEADS` / `CAN_UPDATE_LEADS` / `CAN_UPDATE_LEAD_STATUS` / `CAN_CONVERT_LEADS` / `CAN_ASSIGN_LEADS` |
| `GET /api/v1/auth/staffs` | `ADMIN` or `MANAGER` |
| `GET /auth/user/{id}` | authenticated + self-or-elevated |
| `DELETE /auth/user/{id}` | `ROLE_ADMIN` |
| `POST /auth/register` | `ROLE_ADMIN` (method security -> 403) |
| `GET /api/v1/payroll/staff/{id}` | own-payroll or summary permission + self-or-elevated |
| `GET /api/v1/payroll/all` | `CAN_VIEW_PAYROLL_SUMMARY` |
| `GET /api/reports/profit` | `CAN_VIEW_PROFIT_REPORT` |
| `GET /api/v1/staff-logs` | `CAN_VIEW_STAFF_LOGS` |
| `GET /api/v1/notifications/*` | authenticated; every query scoped to the current user |

## Public and secret-authenticated endpoints

Permitted without a bearer token in `SecurityConfig`:

- `POST /api/v1/auth/login`, `/verify-otp`, `/refresh`
- `POST /api/v1/webhooks/kcb/ipn`
- `POST /api/v1/shopify/**`
- `POST /api/v1/leads`
- Swagger/OpenAPI (`/v3/api-docs/**`, `/swagger-ui/**`)
- `OPTIONS /**`

### Webhook authentication

| Webhook | Mechanism | Failure behaviour |
| --- | --- | --- |
| WPForms leads | `webhook.wpforms.secret` via `?secret=` or `X-WPForms-Secret`, constant-time compare | 503 when unconfigured, 403 when wrong, duplicate `wpEntryId` returns 200 "Duplicate lead ignored." |
| KCB IPN | `kcb.ipn.secret` via `?token=` or `X-KCB-IPN-Secret`, constant-time compare | 503 when unconfigured, 403 when wrong, 400 for incomplete payload |
| Shopify orders | `X-Shopify-Hmac-Sha256` HMAC-SHA256 over the raw body using `shopify.webhook.secret` | 503 when unconfigured, 403 on bad signature, 400 for missing body/malformed JSON/unsupported topic |

## Error contract

All handled errors return `application/problem+json` (`ProblemDetail`) with `title`, `detail`, `status`.

| Status | Triggers |
| --- | --- |
| 400 | validation errors, malformed JSON/date/parameter, illegal arguments |
| 401 | bad credentials, invalid/expired token, missing authentication |
| 403 | access denied, invalid webhook secret, signature failure |
| 404 | `ResourceNotFoundException` |
| 409 | illegal state (e.g. insufficient stock), data integrity conflicts |
| 500 | unhandled exception; details are hidden from the response and logged |

A filter-level authorization denial returns 401 because the entry point is used; method-level denials return 403. This is why `/auth/register` is guarded only by `@PreAuthorize`.

## Order lifecycle

`OrderStatus` owns the rules; services must not hardcode status lists.

```
AWAITING_DISPATCH
  -> DISPATCHED_PAID | DISPATCHED_PARTIAL | DISPATCHED_UNPAID | CANCELLED

DISPATCHED_*
  -> any DISPATCHED_* | DELIVERED_PAID | DELIVERED_UNPAID
  -> RETURN_REQUESTED | CANCELLED

DELIVERED_PAID | DELIVERED_UNPAID
  -> DELIVERED_PAID | DELIVERED_UNPAID | COMPLETED | RETURN_REQUESTED

COMPLETED -> RETURN_REQUESTED
RETURN_REQUESTED -> RETURN_IN_TRANSIT
RETURN_IN_TRANSIT -> RETURNED_AWAITING_INSPECTION
RETURNED_AWAITING_INSPECTION -> RETURNED_GOOD | RETURNED_DAMAGED
```

Terminal states: `CANCELLED`, `RETURNED_GOOD`, `RETURNED_DAMAGED`. Same-status updates are no-ops.

Status predicates:

- `isRevenueRecognized()`: all dispatched, delivered and completed states (not awaiting dispatch, cancellations or returns).
- `isFullyPaid()`: `DISPATCHED_PAID`, `DELIVERED_PAID`, `COMPLETED`.
- `isDeliveredOrClosed()`: both delivered states plus `COMPLETED`.
- `isDispatchedFamily()`: the three `DISPATCHED_*` states.
- `afterPayment(fullyPaid)`: dispatched orders become paid/partial; `DELIVERED_UNPAID` becomes `DELIVERED_PAID`; other states are unchanged.

Rules enforced by `OrderService`:

- Manual status changes must satisfy `canTransitionTo`.
- `DISPATCHED_PAID` and `DELIVERED_PAID` require `paidAmount >= totalAmount`; staff cannot mark an unpaid order paid.
- Counter orders (`OrderType.COUNTER`) start as `DISPATCHED_PAID` with `paidAmount = total` and `balance = 0`, and deduct stock on creation.
- Stock is deducted on the first transition into a dispatched state and returned on `CANCELLED` (if previously dispatched) and on `RETURNED_GOOD` (once).
- Order reads use a fetch join so items and products are returned with the order.

## Payments and reconciliation

`POST /api/v1/payments` (manual record):

- amount must be > 0;
- `transactionRef` must be unique when supplied;
- payments against `CANCELLED` or `RETURN*` orders are rejected;
- a payment linked to an order updates `paidAmount` and recomputes `balance = max(total - paid, 0)`;
- status moves only within dispatched/delivered states via `OrderStatus.afterPayment`; a payment never dispatches an order by itself;
- the first transition to fully paid sends a confirmation SMS through the after-commit event listener.

`POST /payments/{paymentId}/match/{orderId}`:

- rejects payments that are already matched, suspicious, or not in `SUCCESS`/`PAID` status;
- rejects cancelled/returned orders;
- marks the payment matched, records `reconciledBy`/`reconciledAt`, and applies it to the order.

KCB IPN (`KcbIpnServiceImpl`):

- requires `transactionID`, a valid positive amount and a supported currency (`KES`);
- a duplicate `transactionID` is ignored (idempotent);
- a payment is saved even when unmatched: no order for the account number, unsupported currency, or cancelled order;
- unsupported currency is marked suspicious and cannot be matched later;
- matched payments use `PaymentMethod.BANK`, status `SUCCESS`, and the same paid/balance/status logic as manual payments.

`orders.account_number` is the payment reference; V37 makes it unique so two orders cannot share a KCB BusinessKey.

## Inventory

- Products with no inventory row are not stock-tracked; `reduceIfTracked`/`increaseIfTracked` no-op for them.
- Tracked products reject reductions below zero with `IllegalStateException("Insufficient stock")`, returned as 409.
- Restock and manual reduction go through `InventoryService`; stock units (scan-based dispatch) are a separate table.

## Reporting and payroll

Profit report (`GET /api/reports/profit?from=&to=`):

- revenue and COGS come from one aggregate query over order items of revenue-recognized, non-deleted orders;
- expenses come from one aggregate query;
- `grossProfit = revenue - cogs`, `netProfit = grossProfit - expenses`;
- missing aggregates are treated as zero.

Staff summary:

- revenue, COGS, profit and average order value consider revenue-recognized orders only;
- commission delegates to `PayrollService` so there is one KPI implementation;
- deleted orders are excluded.

Payroll:

- daily pay is 300 KES;
- weekly KPI threshold is 80% of assigned leads converted into paid orders;
- commission is 50 KES per fully paid order at or above the threshold, 30 KES below it;
- all-staff payroll runs grouped weekly queries per metric instead of per-staff queries;
- fully paid statuses drive commission: `DISPATCHED_PAID`, `DELIVERED_PAID`, `COMPLETED`.

## Notifications

- `GET /api/v1/notifications/subscribe` returns an SSE stream; authentication may use `?access_token=`.
- Multiple emitters are supported per user (multiple tabs, bell plus page-level listeners).
- A scheduled heartbeat sends SSE comments to keep proxies from closing idle streams.
- Dead emitters are removed on completion, timeout, error or write failure.
- `GET /unread`, `PATCH /{id}/read` and `PATCH /read-all` are scoped to the authenticated user; modifying another user's notification is rejected.
- The UI keeps the stream open and falls back to polling unread notifications every 5 minutes.

## Data integrity and migrations

- Flyway owns the schema. `ddl-auto` defaults to `none`; the H2 test profile overrides it with `create-drop` and disables Flyway.
- V34-V37 are written to be deployable against a populated production database:
  - missing foreign keys use `NOT VALID`;
  - unique indexes are skipped with a notice when duplicates exist;
  - trigram indexes are skipped when `pg_trgm` cannot be installed;
  - `IF NOT EXISTS` is used throughout.
- Because tests do not run Flyway, always dry-run the migrations against a PostgreSQL copy before deploying.

## Limits and defaults

| Setting | Value |
| --- | --- |
| Maximum page size | 500 (`spring.data.web.pageable.max-page-size`) |
| Access token | `JWT_ACCESS_EXPIRATION` ms (15 minutes in the test profile) |
| Refresh token | `JWT_REFRESH_EXPIRATION` ms (7 days in the test profile) |
| OTP | 6 digits, 5 minutes, 3 attempts, 60-second cooldown |
| Auth cache TTL | 60 seconds |
| Reference data cache (frontend) | 120 seconds |
| Notification fallback poll | 5 minutes |
