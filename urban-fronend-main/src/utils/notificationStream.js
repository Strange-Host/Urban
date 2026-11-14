import { api } from "../api/api";

// One shared live-notification (SSE) connection for the whole app.
//
// Why not a plain EventSource per component:
//  - the token is sent as a query parameter and captured once, so after the access
//    token expired every reconnect reused the stale token, got a 401 and the browser
//    gave up for good (notifications silently stopped until a page reload);
//  - the bell and the CRM page each opened their own connection.
//
// Here the connection is opened with the *current* token, shared between subscribers,
// and re-opened after any error with a freshly refreshed token and a growing delay.

const MIN_DELAY_MS = 2000;
const MAX_DELAY_MS = 30000;

const subscribers = new Set();
let source = null;
let retryTimer = null;
let retryDelay = MIN_DELAY_MS;

function emit(handlerName, payload) {
  subscribers.forEach((subscriber) => {
    try {
      subscriber[handlerName]?.(payload);
    } catch (err) {
      console.error("Notification subscriber failed", err);
    }
  });
}

function open() {
  if (source || subscribers.size === 0) return;

  const token = localStorage.getItem("accessToken");
  if (!token) return;

  const base = api.defaults.baseURL || "";
  const es = new EventSource(
    `${base}/api/v1/notifications/subscribe?access_token=${encodeURIComponent(token)}`
  );
  source = es;

  es.addEventListener("notification", (event) => {
    try {
      emit("onNotification", JSON.parse(event.data));
    } catch (err) {
      console.error("Failed to parse notification", err);
    }
  });

  es.addEventListener("open", () => {
    retryDelay = MIN_DELAY_MS;
    emit("onOpen");
  });

  es.onerror = () => {
    // Do not let the browser retry by itself: it would reuse the old token.
    es.close();
    if (source === es) source = null;
    scheduleReconnect();
  };
}

function scheduleReconnect() {
  if (retryTimer || subscribers.size === 0) return;

  retryTimer = setTimeout(async () => {
    retryTimer = null;
    if (subscribers.size === 0) return;

    try {
      // Any authenticated call refreshes an expired access token (api interceptor), or
      // sends the user to the login page if the refresh token is no longer valid.
      await api.get("/api/v1/notifications/unread");
    } catch {
      // Still offline or signed out; the next attempt will try again.
    }

    open();
    if (!source) scheduleReconnect();
  }, retryDelay);

  retryDelay = Math.min(retryDelay * 2, MAX_DELAY_MS);
}

/**
 * Subscribe to live notifications. Returns an unsubscribe function; the shared
 * connection closes when the last subscriber leaves.
 */
export function subscribeToNotifications({ onNotification, onOpen } = {}) {
  const subscriber = { onNotification, onOpen };
  subscribers.add(subscriber);
  open();

  return () => {
    subscribers.delete(subscriber);

    if (subscribers.size === 0) {
      if (source) {
        source.close();
        source = null;
      }
      if (retryTimer) {
        clearTimeout(retryTimer);
        retryTimer = null;
      }
      retryDelay = MIN_DELAY_MS;
    }
  };
}

// For tests only.
export function resetNotificationStream() {
  subscribers.clear();
  if (source) source.close();
  source = null;
  if (retryTimer) clearTimeout(retryTimer);
  retryTimer = null;
  retryDelay = MIN_DELAY_MS;
}
