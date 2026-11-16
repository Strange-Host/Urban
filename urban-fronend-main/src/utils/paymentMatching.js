import { orderAcceptsPayment, orderBalance } from "../pages/orders/paymentEntry";

const round2 = (value) => Math.round(Number(value) * 100) / 100;

/** Small badges describing why a payment needs a person's attention. */
export function paymentFlags(payment) {
  const flags = [];
  if (payment?.suspicious) flags.push({ key: "suspicious", label: "Review", tone: "danger" });
  if (payment && payment.matched === false) flags.push({ key: "unmatched", label: "Unmatched", tone: "warning" });
  return flags;
}

/** Whether this payment may be attached to this order (mirrors the server's rules). */
export function matchProblem(payment, order) {
  if (!payment || !order) return "Choose an order.";
  if (payment.suspicious) {
    return "This payment is flagged for review and cannot be matched.";
  }
  if (!orderAcceptsPayment(order)) {
    return "Cancelled and returned orders cannot take payments.";
  }

  const balance = orderBalance(order);
  if (balance <= 0) return "This order is already fully paid.";
  if (round2(payment.amount) > balance) {
    return `The payment (${round2(payment.amount).toFixed(2)}) is more than the order balance (${balance.toFixed(2)}).`;
  }
  return "";
}
