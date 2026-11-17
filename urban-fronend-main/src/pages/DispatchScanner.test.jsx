import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("../api/ordersApi", () => ({
  fetchOrders: vi.fn(),
  scanBarcode: vi.fn(),
  undoScan: vi.fn(),
  completeDispatch: vi.fn(),
}));
vi.mock("../api/stickersApi", () => ({ getErrorDetail: vi.fn(async (e, fallback) => e?.response?.data?.detail || fallback) }));
vi.mock("../auth/useAuth", () => ({ useAuth: () => ({ can: () => true, user: { id: "u1", name: "Packer", role: "STAFF" } }) }));
vi.mock("../utils/printer", () => ({ printReceiptToUSB: vi.fn() }));
vi.mock("../utils/settings", () => ({ loadMpesaPaybill: vi.fn(async () => "") }));

import { completeDispatch, fetchOrders, scanBarcode } from "../api/ordersApi";
import DispatchScanner from "./DispatchScanner";

const order = { id: "o1", accountNumber: "8025631#ABCDE", customerName: "Amina", orderStatus: "AWAITING_DISPATCH" };

function scan(code) {
  const input = screen.getByPlaceholderText(/Waiting for scan/);
  fireEvent.change(input, { target: { value: code } });
  fireEvent.keyDown(input, { key: "Enter" });
}

describe("Dispatch scanner", () => {
  beforeEach(() => {
    fetchOrders.mockReset();
    scanBarcode.mockReset();
    completeDispatch.mockReset();
    fetchOrders.mockResolvedValue({ content: [order] });
  });

  it("offers Finish dispatch when every unit is packed but the order did not dispatch", async () => {
    // The last two stickers were scanned together: the order shows 2 of 2 but is still awaiting dispatch.
    scanBarcode.mockResolvedValue({
      serial: "UT00000002",
      orderId: "o1",
      accountNumber: "8025631#ABCDE",
      orderStatus: "AWAITING_DISPATCH",
      unitsScanned: 2,
      unitsRequired: 2,
      orderDispatched: false,
      message: "Packed 2 of 2 units",
    });
    completeDispatch.mockResolvedValue({
      orderId: "o1",
      accountNumber: "8025631#ABCDE",
      orderStatus: "DISPATCHED_PAID",
      unitsScanned: 2,
      unitsRequired: 2,
      orderDispatched: true,
    });

    render(<DispatchScanner />);
    await waitFor(() => expect(fetchOrders).toHaveBeenCalled());

    scan("UT00000002");

    const button = await screen.findByText("Finish dispatch");
    fireEvent.click(button);

    await waitFor(() => expect(completeDispatch).toHaveBeenCalledWith("o1"));
    await waitFor(() => expect(screen.getByText("Order dispatched")).toBeTruthy());
    expect(screen.queryByText("Finish dispatch")).toBeNull();
  });

  it("does not offer it while units are still missing", async () => {
    scanBarcode.mockResolvedValue({
      serial: "UT00000001",
      orderId: "o1",
      accountNumber: "8025631#ABCDE",
      orderStatus: "AWAITING_DISPATCH",
      unitsScanned: 1,
      unitsRequired: 2,
      orderDispatched: false,
      message: "Packed 1 of 2 units",
    });

    render(<DispatchScanner />);
    await waitFor(() => expect(fetchOrders).toHaveBeenCalled());

    scan("UT00000001");

    await waitFor(() => expect(screen.getByText("Packing order")).toBeTruthy());
    expect(screen.queryByText("Finish dispatch")).toBeNull();
  });

  it("shows the server's reason when finishing is refused", async () => {
    scanBarcode.mockResolvedValue({
      serial: "UT00000002",
      orderId: "o1",
      accountNumber: "8025631#ABCDE",
      orderStatus: "AWAITING_DISPATCH",
      unitsScanned: 2,
      unitsRequired: 2,
      orderDispatched: false,
      message: "Packed 2 of 2",
    });
    completeDispatch.mockRejectedValue({ response: { data: { detail: "Insufficient stock for Leather Wallet: 1 available, 2 needed" } } });

    render(<DispatchScanner />);
    await waitFor(() => expect(fetchOrders).toHaveBeenCalled());
    scan("UT00000002");
    fireEvent.click(await screen.findByText("Finish dispatch"));

    await waitFor(() => expect(screen.getByText(/Insufficient stock for Leather Wallet/)).toBeTruthy());
  });
});
