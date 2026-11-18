import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("../api/api", () => ({
  api: { defaults: { baseURL: "http://api.test" }, post: vi.fn() },
}));

import { api } from "../api/api";
import { resetNotificationStream, subscribeToNotifications } from "./notificationStream";

class FakeEventSource {
  static instances = [];

  constructor(url) {
    this.url = url;
    this.closed = false;
    this.listeners = {};
    FakeEventSource.instances.push(this);
  }

  addEventListener(name, handler) {
    this.listeners[name] = handler;
  }

  close() {
    this.closed = true;
  }

  fire(name, data) {
    this.listeners[name]?.({ data });
  }
}

describe("notification stream", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    FakeEventSource.instances = [];
    vi.stubGlobal("EventSource", FakeEventSource);
    localStorage.setItem("accessToken", "token-1");
    api.post.mockReset();
    let issued = 0;
    api.post.mockImplementation(async () => ({ data: { ticket: `ticket-${++issued}` } }));
  });

  afterEach(() => {
    resetNotificationStream();
    vi.useRealTimers();
    vi.unstubAllGlobals();
    localStorage.clear();
  });

  it("shares one connection between subscribers and closes when the last one leaves", async () => {
    const first = vi.fn();
    const second = vi.fn();

    const offFirst = subscribeToNotifications({ onNotification: first });
    const offSecond = subscribeToNotifications({ onNotification: second });
    await vi.advanceTimersByTimeAsync(0);

    expect(api.post).toHaveBeenCalledTimes(1);
    expect(FakeEventSource.instances).toHaveLength(1);
    FakeEventSource.instances[0].fire("notification", JSON.stringify({ id: 7 }));
    expect(first).toHaveBeenCalledWith({ id: 7 });
    expect(second).toHaveBeenCalledWith({ id: 7 });

    offFirst();
    expect(FakeEventSource.instances[0].closed).toBe(false);
    offSecond();
    expect(FakeEventSource.instances[0].closed).toBe(true);
  });

  it("puts only a one-time ticket in the URL, never the access token", async () => {
    subscribeToNotifications({ onNotification: vi.fn() });
    await vi.advanceTimersByTimeAsync(0);

    expect(api.post).toHaveBeenCalledWith("/api/v1/notifications/stream-ticket");
    const url = FakeEventSource.instances[0].url;
    expect(url).toBe("http://api.test/api/v1/notifications/subscribe?ticket=ticket-1");
    expect(url).not.toContain("token-1");
  });

  it("asks for a fresh ticket when reconnecting after the connection drops", async () => {
    subscribeToNotifications({ onNotification: vi.fn() });
    await vi.advanceTimersByTimeAsync(0);

    FakeEventSource.instances[0].onerror();
    expect(FakeEventSource.instances[0].closed).toBe(true);

    await vi.advanceTimersByTimeAsync(2500);

    expect(api.post).toHaveBeenCalledTimes(2);
    expect(FakeEventSource.instances).toHaveLength(2);
    expect(FakeEventSource.instances[1].url).toContain("ticket=ticket-2");
  });

  it("keeps retrying while the ticket request fails", async () => {
    api.post.mockRejectedValueOnce(new Error("offline"));
    subscribeToNotifications({ onNotification: vi.fn() });
    await vi.advanceTimersByTimeAsync(0);
    expect(FakeEventSource.instances).toHaveLength(0);

    await vi.advanceTimersByTimeAsync(0);
    await vi.advanceTimersByTimeAsync(2500);

    expect(FakeEventSource.instances).toHaveLength(1);
  });

  it("tells subscribers when the stream (re)opens so they can reload what they missed", async () => {
    const onOpen = vi.fn();
    subscribeToNotifications({ onOpen });
    await vi.advanceTimersByTimeAsync(0);

    FakeEventSource.instances[0].fire("open");

    expect(onOpen).toHaveBeenCalledTimes(1);
  });

  it("does not open a connection when signed out", async () => {
    localStorage.removeItem("accessToken");

    subscribeToNotifications({ onNotification: vi.fn() });
    await vi.advanceTimersByTimeAsync(0);

    expect(api.post).not.toHaveBeenCalled();
    expect(FakeEventSource.instances).toHaveLength(0);
  });
});
