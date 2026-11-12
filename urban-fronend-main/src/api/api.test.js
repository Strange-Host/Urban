import MockAdapter from "axios-mock-adapter";
import { beforeEach, describe, expect, it, vi } from "vitest";

describe("api refresh interceptor", () => {
  beforeEach(() => {
    vi.resetModules();
    localStorage.clear();
    // Skip the hard redirect path so jsdom does not attempt a navigation.
    window.history.replaceState({}, "", "/login");
  });

  async function loadApi() {
    const axios = (await import("axios")).default;
    const { api } = await import("./api");
    return { axios, api };
  }

  it("refreshes an expired access token once and replays the request", async () => {
    const { axios, api } = await loadApi();
    const apiMock = new MockAdapter(api);
    const bareMock = new MockAdapter(axios);

    localStorage.setItem("refreshToken", "refresh-token");

    let attempts = 0;
    apiMock.onGet("/api/v1/orders").reply((config) => {
      attempts += 1;
      if (attempts === 1) return [401, { title: "Unauthorized" }];
      expect(config.headers.Authorization).toBe("Bearer fresh-access");
      return [200, { content: [] }];
    });
    bareMock
      .onPost("http://localhost:8080/api/v1/auth/refresh")
      .reply(200, { accessToken: "fresh-access" });

    const response = await api.get("/api/v1/orders");

    expect(response.status).toBe(200);
    expect(attempts).toBe(2);
    expect(localStorage.getItem("accessToken")).toBe("fresh-access");
    expect(bareMock.history.post).toHaveLength(1);
  });

  it("clears the session when the refresh token is rejected", async () => {
    const { axios, api } = await loadApi();
    const apiMock = new MockAdapter(api);
    const bareMock = new MockAdapter(axios);

    localStorage.setItem("accessToken", "stale-access");
    localStorage.setItem("refreshToken", "dead-refresh");

    apiMock.onGet("/api/v1/orders").reply(401);
    bareMock
      .onPost("http://localhost:8080/api/v1/auth/refresh")
      .reply(401, { title: "Unauthorized" });

    await expect(api.get("/api/v1/orders")).rejects.toBeTruthy();

    expect(localStorage.getItem("accessToken")).toBeNull();
    expect(localStorage.getItem("refreshToken")).toBeNull();
  });

  it("does not attempt a refresh when no refresh token is stored", async () => {
    const { axios, api } = await loadApi();
    const apiMock = new MockAdapter(api);
    const bareMock = new MockAdapter(axios);

    apiMock.onGet("/api/v1/orders").reply(401);

    await expect(api.get("/api/v1/orders")).rejects.toBeTruthy();

    expect(bareMock.history.post).toHaveLength(0);
  });

  it("never retries a failed login through the refresh flow", async () => {
    const { axios, api } = await loadApi();
    const apiMock = new MockAdapter(api);
    const bareMock = new MockAdapter(axios);

    localStorage.setItem("refreshToken", "refresh-token");
    apiMock.onPost("/api/v1/auth/login").reply(401, { detail: "Authentication failed" });

    await expect(api.post("/api/v1/auth/login", {})).rejects.toBeTruthy();

    expect(apiMock.history.post).toHaveLength(1);
    expect(bareMock.history.post).toHaveLength(0);
  });
});
