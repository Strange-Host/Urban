import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("../../api/paymentsApi", () => ({
  searchOrdersForPayment: vi.fn(),
  matchPaymentToOrder: vi.fn(),
}));

import { matchPaymentToOrder, searchOrdersForPayment } from "../../api/paymentsApi";
import MatchPaymentModal from "./MatchPaymentModal";

const payment = {
  id: "pay-1",
  amount: 500,
  payerName: "Amina Njeri",
  transactionRef: "TX1",
  notes: "UNMATCHED KCB IPN: wrong account",
  matched: false,
  suspicious: false,
};

const openOrder = {
  id: "o1",
  accountNumber: "8025631#ABCDE",
  customerName: "Amina Njeri",
  orderStatus: "DISPATCHED_UNPAID",
  totalAmount: 1000,
  paidAmount: 0,
  balance: 1000,
};
const smallOrder = { ...openOrder, id: "o2", accountNumber: "8025631#SMALL", balance: 200, totalAmount: 200 };

function renderModal(props = {}) {
  const onClose = vi.fn();
  const onMatched = vi.fn();
  render(<MatchPaymentModal payment={payment} onClose={onClose} onMatched={onMatched} {...props} />);
  return { onClose, onMatched };
}

describe("MatchPaymentModal", () => {
  beforeEach(() => {
    searchOrdersForPayment.mockReset();
    matchPaymentToOrder.mockReset();
    searchOrdersForPayment.mockResolvedValue([openOrder, smallOrder]);
  });

  it("searches for the payer's orders and matches the chosen one", async () => {
    matchPaymentToOrder.mockResolvedValue({});
    const { onMatched } = renderModal();

    await waitFor(() => expect(screen.getByText(/8025631#ABCDE/)).toBeTruthy());
    expect(searchOrdersForPayment).toHaveBeenCalledWith("Amina Njeri");

    fireEvent.click(screen.getAllByRole("radio")[0]);
    fireEvent.click(screen.getByText("Match payment"));

    await waitFor(() => expect(matchPaymentToOrder).toHaveBeenCalledWith("pay-1", "o1"));
    await waitFor(() => expect(onMatched).toHaveBeenCalled());
  });

  it("refuses an order whose balance is smaller than the payment", async () => {
    renderModal();
    await waitFor(() => expect(screen.getByText(/8025631#SMALL/)).toBeTruthy());

    fireEvent.click(screen.getAllByRole("radio")[1]);

    expect(screen.getByText(/more than the order balance/)).toBeTruthy();
    expect(screen.getByText("Match payment").disabled).toBe(true);
    expect(matchPaymentToOrder).not.toHaveBeenCalled();
  });

  it("shows the server's reason when matching fails", async () => {
    matchPaymentToOrder.mockRejectedValue({ response: { data: { detail: "Payment already matched" } } });
    renderModal();
    await waitFor(() => expect(screen.getByText(/8025631#ABCDE/)).toBeTruthy());

    fireEvent.click(screen.getAllByRole("radio")[0]);
    fireEvent.click(screen.getByText("Match payment"));

    await waitFor(() => expect(screen.getByText("Payment already matched")).toBeTruthy());
  });
});
