import { PAYMENT_METHODS, needsReference } from "./counterPayments";

const round2 = (value) => Math.round(Number(value) * 100) / 100;

/** What the customer still owes on an order. */
export function orderBalance(order) {
  if (!order) return 0;
  if (order.balance !== null && order.balance !== undefined && order.balance !== "") {
    return Math.max(0, round2(order.balance));
  }
  return Math.max(0, round2(Number(order.totalAmount || 0) - Number(order.paidAmount || 0)));
}

/** Cancelled and returned orders take no money (same rule as the server). */
export function orderAcceptsPayment(order) {
  const status = String(order?.orderStatus || "");
  return status !== "CANCELLED" && !status.startsWith("RETURN");
}

/** Whether to offer "Record payment" for this order. */
export function canTakePayment(order) {
  return orderAcceptsPayment(order) && orderBalance(order) > 0;
}

/** Returns an error message, or "" when the entry can be sent to the server. */
export function validatePaymentEntry({ method, amount, reference }, order) {
  if (!PAYMENT_METHODS.some((m) => m.value === method)) {
    return "Choose a payment method.";
  }

  const value = Number(amount);
  if (!Number.isFinite(value) || value <= 0) {
    return "Enter a payment amount greater than zero.";
  }

  if (needsReference(method) && !String(reference ?? "").trim()) {
    return "Enter the transaction code for M-Pesa and bank payments.";
  }

  const balance = orderBalance(order);
  if (balance <= 0) return "This order is already fully paid.";
  if (round2(value) > balance) {
    return `Payment cannot exceed the balance of ${balance.toFixed(2)}.`;
  }

  return "";
}
