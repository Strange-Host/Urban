// Payment lines for a counter (walk-in) sale. A sale can be split across methods,
// e.g. part cash and part M-Pesa; the backend requires the lines to add up to the
// sale total exactly.
export const PAYMENT_METHODS = [
  { value: "CASH", label: "Cash" },
  { value: "MPESA", label: "M-Pesa" },
  { value: "BANK", label: "Bank" },
];

export function emptyPaymentLine() {
  return { method: "CASH", amount: "", transactionRef: "" };
}

export function needsReference(method) {
  return method === "MPESA" || method === "BANK";
}

const round2 = (value) => Math.round(Number(value) * 100) / 100;

/** Turns the form lines into API payments. A single blank amount means "the full total". */
export function normalizeCounterPayments(lines, grandTotal) {
  const list = Array.isArray(lines) ? lines : [];

  return list.map((line) => {
    const raw = String(line.amount ?? "").trim();
    const amount = raw === "" && list.length === 1 ? round2(grandTotal) : Number(raw);

    return {
      method: line.method,
      amount,
      transactionRef: String(line.transactionRef ?? "").trim() || null,
    };
  });
}

/** Returns an error message, or "" when the payments are valid for this total. */
export function validateCounterPayments(lines, grandTotal) {
  const payments = normalizeCounterPayments(lines, grandTotal);

  if (payments.length === 0) return "Add at least one payment.";

  for (const payment of payments) {
    if (!PAYMENT_METHODS.some((m) => m.value === payment.method)) {
      return "Choose a payment method for every payment.";
    }
    if (!Number.isFinite(payment.amount) || payment.amount <= 0) {
      return "Every payment needs an amount greater than 0.";
    }
    if (needsReference(payment.method) && !payment.transactionRef) {
      return "Enter the transaction code for every M-Pesa or bank payment.";
    }
  }

  const paid = round2(payments.reduce((sum, p) => sum + p.amount, 0));
  const total = round2(grandTotal);

  if (paid !== total) {
    return `Payments add up to ${paid.toFixed(2)} but the sale total is ${total.toFixed(2)}.`;
  }

  return "";
}
