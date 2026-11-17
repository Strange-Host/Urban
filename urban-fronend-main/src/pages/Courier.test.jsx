import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("../api/api", () => ({ api: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() } }));
vi.mock("../auth/useAuth", () => ({ useAuth: () => ({ can: () => true, user: { id: "admin", role: "ADMIN" } }) }));

import { api } from "../api/api";
import Couriers from "./Courier";

const courier = { id: "c1", name: "Sam Rider", phoneNumber: "0711111111", active: true };

function order(i) {
  return { id: `o${i}`, customerName: `Customer ${i}`, totalAmount: 100 * i, orderStatus: "DELIVERED_PAID", createdAt: "2026-11-01T10:00:00" };
}

describe("Courier orders panel", () => {
  beforeEach(() => {
    api.get.mockReset();
    api.get.mockImplementation((url, config) => {
      if (url === "/api/v1/orders") {
        const page = config.params.page;
        const rows = page === 0 ? [order(1), order(2)] : [order(3)];
        return Promise.resolve({ data: { content: rows, totalElements: 3 } });
      }
      return Promise.resolve({ data: { content: [courier], totalPages: 1 } });
    });
  });

  it("lets you load the rest of a courier's orders instead of stopping at the first page", async () => {
    render(<Couriers />);

    // The page renders a desktop row and a mobile card for each courier.
    await screen.findAllByText("Sam Rider");

    const ordersButton = screen.getAllByRole("button").find((b) => b.textContent.trim() === "Orders");
    fireEvent.click(ordersButton);

    await waitFor(() => expect(screen.getByText("Showing 2 of 3")).toBeTruthy());
    fireEvent.click(screen.getByText("Load more"));

    await waitFor(() => expect(screen.getByText("Showing 3 of 3")).toBeTruthy());
    expect(screen.queryByText("Load more")).toBeNull();
  });
});
