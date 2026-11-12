const encode = (value) => Buffer.from(JSON.stringify(value)).toString("base64url");

export function makeToken(claims, lifetimeSeconds = 3600) {
  return [
    encode({ alg: "HS256", typ: "JWT" }),
    encode({ exp: Math.floor(Date.now() / 1000) + lifetimeSeconds, ...claims }),
    "e2e-signature",
  ].join(".");
}

export const ADMIN_TOKEN = makeToken({
  id: "00000000-0000-0000-0000-00000000ad01",
  sub: "admin@urban.test",
  firstName: "Ada",
  lastName: "Admin",
  role: "ADMIN",
  authorities: [
    "ROLE_ADMIN",
    "CAN_VIEW_ORDERS",
    "CAN_CREATE_ORDERS",
    "CAN_VIEW_LEADS",
    "CAN_VIEW_PRODUCTS",
    "CAN_VIEW_STAFF",
  ],
});

export const CASHIER_TOKEN = makeToken({
  id: "00000000-0000-0000-0000-00000000ca01",
  sub: "cashier@urban.test",
  firstName: "Cara",
  lastName: "Cashier",
  role: "CASHIER",
  authorities: ["ROLE_CASHIER", "CAN_VIEW_ORDERS"],
});

export async function signIn(page, token = ADMIN_TOKEN) {
  await page.addInitScript((value) => {
    localStorage.setItem("accessToken", value);
    localStorage.setItem("refreshToken", "e2e-refresh-token");
  }, token);
}

export const ORDER = {
  id: "11111111-1111-1111-1111-111111111111",
  customerName: "Amina Yusuf",
  customerPhone: "0712000111",
  orderStatus: "DISPATCHED_PAID",
  totalAmount: 4200,
  balance: 0,
  deliveryCity: "Nairobi",
  accountNumber: "ACC-100",
  deliveryNotes: "Call on arrival",
  createdAt: new Date().toISOString(),
  items: [{ productName: "Leather Bag", quantity: 1 }],
};

export const LEAD = {
  id: "22222222-2222-2222-2222-222222222222",
  name: "Brian Otieno",
  phoneNumber: "0722333444",
  location: "Mombasa",
  status: "NEW",
  notes: "Interested in the tote bag",
  createdAt: new Date().toISOString(),
  followUpAt: null,
  assignedTo: null,
  convertedOrderId: null,
};

function pageFixture(content) {
  return {
    content,
    totalElements: content.length,
    totalPages: content.length ? 1 : 0,
    number: 0,
    size: 10,
    first: true,
    last: true,
    numberOfElements: content.length,
    empty: content.length === 0,
  };
}

const EMPTY_PAGE = pageFixture([]);
const PROFIT_REPORT = {
  revenue: 0,
  cogs: 0,
  grossProfit: 0,
  expenses: 0,
  netProfit: 0,
};

/**
 * Answers every API call the app makes, so the journeys run without a backend.
 * Individual specs can override the login response or the fixture lists.
 */
export async function mockApi(page, options = {}) {
  const {
    orders = [ORDER],
    leads = [LEAD],
    loginStatus = 200,
    loginBody = { message: "OTP_SENT" },
    verifyStatus = 200,
    verifyBody = { accessToken: ADMIN_TOKEN, refreshToken: "e2e-refresh-token", message: "SUCCESS" },
  } = options;

  // Predicate (not a "**/api/**" glob): the glob would also swallow Vite's own
  // module requests such as /src/api/authApi.js and leave the app blank.
  const isApiRequest = (url) => url.pathname.startsWith("/api/");

  await page.route(isApiRequest, async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const path = url.pathname;
    const method = request.method();

    if (path.endsWith("/notifications/subscribe")) {
      return route.fulfill({
        status: 200,
        headers: { "content-type": "text/event-stream", "access-control-allow-origin": "*" },
        body: "",
      });
    }

    if (method === "POST") {
      if (path === "/api/v1/auth/login") {
        return json(route, loginBody, loginStatus);
      }
      if (path === "/api/v1/auth/verify-otp") {
        return json(route, verifyBody, verifyStatus);
      }
      return json(route, {}, 200);
    }

    if (method !== "GET") {
      return json(route, {}, 200);
    }

    if (path === "/api/v1/orders/dashboard/top-selling") return json(route, []);
    if (path === "/api/v1/orders") return json(route, pageFixture(orders));
    if (path === "/api/v1/leads") return json(route, pageFixture(leads));
    if (path === "/api/v1/products" || path === "/api/v1/products/search") {
      return json(route, pageFixture([{ id: "prod-1", name: "Leather Bag", sellingPrice: 4200 }]));
    }
    if (path === "/api/v1/auth/staffs") return json(route, pageFixture([]));
    if (path === "/api/v1/notifications/unread") return json(route, []);
    if (path === "/api/v1/couriers/active") return json(route, []);
    if (path === "/api/reports/profit") return json(route, PROFIT_REPORT);
    if (path.startsWith("/api/v1/auth/user/")) {
      return json(route, {
        id: "00000000-0000-0000-0000-00000000ad01",
        firstName: "Ada",
        lastName: "Admin",
        phoneNumber: "254700000000",
      });
    }

    return json(route, EMPTY_PAGE);
  });
}

function json(route, body, status = 200) {
  return route.fulfill({
    status,
    headers: { "content-type": "application/json", "access-control-allow-origin": "*" },
    body: JSON.stringify(body),
  });
}
