import { describe, expect, it } from "vitest";
import { canTakePayment, orderBalance, validatePaymentEntry } from "./paymentEntry";

const order = { orderStatus: "DISPATCHED_PARTIAL", totalAmount: 1000, paidAmount: 400, balance: 600 };

describe("order balance and payments", () => {
  it("uses the balance, falling back to total minus paid", () => {
    expect(orderBalance(order)).toBe(600);
    expect(orderBalance({ totalAmount: 1000, paidAmount: 250 })).toBe(750);
    expect(orderBalance({ totalAmount: 100, paidAmount: 300 })).toBe(0);
    expect(orderBalance(null)).toBe(0);
  });

  it("offers payment for part-paid orders but not for paid, cancelled or returned ones", () => {
    expect(canTakePayment(order)).toBe(true);
    expect(canTakePayment({ ...order, orderStatus: "AWAITING_DISPATCH" })).toBe(true);
    expect(canTakePayment({ ...order, balance: 0 })).toBe(false);
    expect(canTakePayment({ ...order, orderStatus: "CANCELLED" })).toBe(false);
    expect(canTakePayment({ ...order, orderStatus: "RETURN_IN_TRANSIT" })).toBe(false);
  });

  it("accepts a valid cash payment up to the balance", () => {
    expect(validatePaymentEntry({ method: "CASH", amount: "600" }, order)).toBe("");
    expect(validatePaymentEntry({ method: "CASH", amount: "0.01" }, order)).toBe("");
  });

  it("rejects an amount above the balance (not the total)", () => {
    expect(validatePaymentEntry({ method: "CASH", amount: "700" }, order)).toContain("balance of 600.00");
  });

  it("requires a code for M-Pesa and bank but not for cash", () => {
    expect(validatePaymentEntry({ method: "MPESA", amount: "100", reference: " " }, order)).toContain("transaction code");
    expect(validatePaymentEntry({ method: "BANK", amount: "100", reference: "FT123" }, order)).toBe("");
  });

  it("rejects bad amounts, unknown methods and fully paid orders", () => {
    expect(validatePaymentEntry({ method: "CASH", amount: "abc" }, order)).toContain("greater than zero");
    expect(validatePaymentEntry({ method: "CARD", amount: "10" }, order)).toContain("method");
    expect(validatePaymentEntry({ method: "CASH", amount: "10" }, { ...order, balance: 0 })).toContain("fully paid");
  });
});
