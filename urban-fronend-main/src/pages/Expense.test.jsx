import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("../api/api", () => ({ api: { get: vi.fn(), post: vi.fn() } }));

import { api } from "../api/api";
import Expenses from "./Expense";

const expense = (id, amount) => ({
  id,
  expenseType: "RENT",
  amount,
  paymentMethod: "CASH",
  referenceCode: null,
  description: `Expense ${id}`,
});

describe("Expenses page", () => {
  beforeEach(() => {
    api.get.mockReset();
    api.get.mockImplementation((url, config) => {
      if (url.endsWith("/summary")) {
        return Promise.resolve({ data: { total: 125000, count: 431 } });
      }
      const page = config?.params?.page ?? 0;
      return Promise.resolve({
        data: { content: [expense(`e${page}`, 1000)], totalPages: 22, number: page },
      });
    });
  });

  it("shows totals for every expense, not just the first page of records", async () => {
    render(<Expenses />);

    await waitFor(() => expect(screen.getAllByText(/125,000/).length).toBeGreaterThan(0));
    expect(screen.getByText("431")).toBeTruthy();
  });

  it("pages through the list instead of stopping at the first 200", async () => {
    render(<Expenses />);
    await waitFor(() => expect(screen.getByText("Page 1 of 22")).toBeTruthy());

    fireEvent.click(screen.getByText("Next"));

    await waitFor(() => expect(screen.getByText("Page 2 of 22")).toBeTruthy());
    expect(api.get).toHaveBeenCalledWith(
      "/api/v1/expenses",
      expect.objectContaining({ params: expect.objectContaining({ page: 1, size: 20 }) })
    );
  });
});
