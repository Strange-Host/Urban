import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("../api/api", () => ({
  api: { defaults: { baseURL: "http://api.test" }, get: vi.fn() },
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
    api.get.mockReset();
    api.get.mockResolvedValue({ data: [] });
  });

  afterEach(() => {
    resetNotificationStream();
    vi.useRealTimers();
    vi.unstubAllGlobals();
    localStorage.clear();
  });

  it("shares one connection between subscribers and closes when the last one leaves", () => {
    const first = vi.fn();
    const second = vi.fn();

    const offFirst = subscribeToNotifications({ onNotification: first });
    const offSecond = subscribeToNotifications({ onNotification: second });

    expect(FakeEventSource.instances).toHaveLength(1);
    FakeEventSource.instances[0].fire("notification", JSON.stringify({ id: 7 }));
    expect(first).toHaveBeenCalledWith({ id: 7 });
    expect(second).toHaveBeenCalledWith({ id: 7 });

    offFirst();
    expect(FakeEventSource.instances[0].closed).toBe(false);
    offSecond();
    expect(FakeEventSource.instances[0].closed).toBe(true);
  });

  it("reconnects with the refreshed token after the connection drops", async () => {
    subscribeToNotifications({ onNotification: vi.fn() });
    expect(FakeEventSource.instances[0].url).toContain("access_token=token-1");

    // The token is refreshed (by the api interceptor) while the stream is down.
    api.get.mockImplementation(async () => {
      localStorage.setItem("accessToken", "token-2");
      return { data: [] };
    });

    FakeEventSource.instances[0].onerror();
    expect(FakeEventSource.instances[0].closed).toBe(true);

    await vi.advanceTimersByTimeAsync(2500);

    expect(api.get).toHaveBeenCalledWith("/api/v1/notifications/unread");
    expect(FakeEventSource.instances).toHaveLength(2);
    expect(FakeEventSource.instances[1].url).toContain("access_token=token-2");
  });

  it("tells subscribers when the stream (re)opens so they can reload what they missed", () => {
    const onOpen = vi.fn();
    subscribeToNotifications({ onOpen });

    FakeEventSource.instances[0].fire("open");

    expect(onOpen).toHaveBeenCalledTimes(1);
  });

  it("does not open a connection when signed out", () => {
    localStorage.removeItem("accessToken");

    subscribeToNotifications({ onNotification: vi.fn() });

    expect(FakeEventSource.instances).toHaveLength(0);
  });
});
