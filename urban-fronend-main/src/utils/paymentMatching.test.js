import { describe, expect, it } from "vitest";
import { matchProblem, paymentFlags } from "./paymentMatching";

const payment = { id: "p1", amount: 500, matched: false, suspicious: false };
const order = { orderStatus: "DISPATCHED_UNPAID", totalAmount: 1000, paidAmount: 0, balance: 1000 };

describe("paymentFlags", () => {
  it("marks unmatched and suspicious payments", () => {
    expect(paymentFlags(payment).map((f) => f.key)).toEqual(["unmatched"]);
    expect(paymentFlags({ ...payment, suspicious: true }).map((f) => f.key)).toEqual(["suspicious", "unmatched"]);
    expect(paymentFlags({ ...payment, matched: true })).toEqual([]);
  });
});

describe("matchProblem", () => {
  it("allows a payment that fits an open order", () => {
    expect(matchProblem(payment, order)).toBe("");
    expect(matchProblem({ ...payment, amount: 1000 }, order)).toBe("");
  });

  it("explains each reason a match would be refused", () => {
    expect(matchProblem(payment, null)).toContain("Choose an order");
    expect(matchProblem({ ...payment, suspicious: true }, order)).toContain("flagged for review");
    expect(matchProblem(payment, { ...order, orderStatus: "CANCELLED" })).toContain("Cancelled");
    expect(matchProblem(payment, { ...order, balance: 0 })).toContain("fully paid");
    expect(matchProblem({ ...payment, amount: 1200 }, order)).toContain("more than the order balance");
  });
});
