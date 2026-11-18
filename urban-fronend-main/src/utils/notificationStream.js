import { api } from "../api/api";

// One shared live-notification (SSE) connection for the whole app.
//
// Why not a plain EventSource per component:
//  - the token was captured once, so after the access token expired every reconnect
//    reused the stale one, got a 401 and the browser gave up for good (notifications
//    silently stopped until a page reload);
//  - the bell and the CRM page each opened their own connection.
//
// EventSource cannot send an Authorization header, and the access token must not go in
// the URL (it ends up in server and proxy logs). So each connection first asks for a
// short-lived one-time ticket with the normal authenticated request (which also refreshes
// an expired access token) and puts only that ticket in the URL.
//
// The connection is shared between subscribers and re-opened after any error with a
// fresh ticket and a growing delay.

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

let opening = false;

async function open() {
  if (source || opening || subscribers.size === 0) return;
  if (!localStorage.getItem("accessToken")) return;

  opening = true;
  let ticket;
  try {
    const res = await api.post("/api/v1/notifications/stream-ticket");
    ticket = res?.data?.ticket;
  } catch {
    // Offline or signed out: try again with a growing delay.
  } finally {
    opening = false;
  }

  if (!ticket) {
    scheduleReconnect();
    return;
  }

  // Everyone may have left, or another call may have connected, while we waited.
  if (!ticket || source || subscribers.size === 0) return;

  const base = api.defaults.baseURL || "";
  const es = new EventSource(
    `${base}/api/v1/notifications/subscribe?ticket=${encodeURIComponent(ticket)}`
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
    // Do not let the browser retry by itself: the ticket is single-use.
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

    // Asking for a ticket is an authenticated call: it refreshes an expired access token
    // (api interceptor), or sends the user to the login page if the refresh token is no
    // longer valid.
    await open();
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
  void open();

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
  opening = false;
}
