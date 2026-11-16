import { render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("../api/api", () => ({ api: { get: vi.fn(), patch: vi.fn() } }));
vi.mock("../utils/notificationStream", () => ({ subscribeToNotifications: vi.fn(() => () => {}) }));

import { api } from "../api/api";
import Dashboard from "./Dashboard";

const forbidden = { response: { status: 403, data: { detail: "Access Denied" } } };

function respond(handlers) {
  api.get.mockImplementation((url) => {
    // Most specific URL first: "/api/v1/orders" is a prefix of the top-selling URL.
    const handler = Object.entries(handlers)
      .sort(([a], [b]) => b.length - a.length)
      .find(([key]) => url.includes(key));
    return handler ? handler[1]() : Promise.resolve({ data: [] });
  });
}

function renderDashboard() {
  return render(
    <MemoryRouter>
      <Dashboard />
    </MemoryRouter>
  );
}

describe("Dashboard", () => {
  beforeEach(() => {
    api.get.mockReset();
    api.patch.mockReset();
    api.get.mockResolvedValue({ data: [] });
  });

  it("still shows the sections a user may see when the profit report is forbidden", async () => {
    respond({
      "/api/reports/profit": () => Promise.reject(forbidden),
      "/api/v1/orders/dashboard/top-selling": () => Promise.reject(forbidden),
      "/api/v1/orders": () => Promise.resolve({ data: { totalElements: 42, content: [] } }),
    });

    renderDashboard();

    await waitFor(() => expect(screen.getByText("42")).toBeTruthy());
    expect(screen.queryByText("Revenue")).toBeNull();
    expect(screen.queryByText("Top Selling Products")).toBeNull();
    expect(screen.queryByText(/do not have permission/i)).toBeNull();
    expect(screen.getByText("Recent Orders")).toBeTruthy();
  });

  it("shows an error for a real failure instead of hiding it", async () => {
    respond({
      "/api/reports/profit": () =>
        Promise.reject({ response: { status: 500, data: { detail: "Report is unavailable" } } }),
      "/api/v1/orders": () => Promise.resolve({ data: { totalElements: 1, content: [] } }),
    });

    renderDashboard();

    await waitFor(() => expect(screen.getByText(/Report is unavailable/)).toBeTruthy());
  });
});
