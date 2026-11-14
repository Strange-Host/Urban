import { describe, expect, it } from "vitest";
import {
  emptyPaymentLine,
  normalizeCounterPayments,
  validateCounterPayments,
} from "./counterPayments";

describe("counter sale payments", () => {
  it("treats a single blank cash line as the full total", () => {
    const lines = [emptyPaymentLine()];

    expect(normalizeCounterPayments(lines, 1000)).toEqual([
      { method: "CASH", amount: 1000, transactionRef: null },
    ]);
    expect(validateCounterPayments(lines, 1000)).toBe("");
  });

  it("accepts a split that adds up exactly", () => {
    const lines = [
      { method: "CASH", amount: "300", transactionRef: "" },
      { method: "MPESA", amount: "700", transactionRef: " QWE123 " },
    ];

    expect(validateCounterPayments(lines, 1000)).toBe("");
    expect(normalizeCounterPayments(lines, 1000)[1].transactionRef).toBe("QWE123");
  });

  it("rejects totals that do not match, in either direction", () => {
    const under = [{ method: "CASH", amount: "900", transactionRef: "" }];
    const over = [{ method: "CASH", amount: "1100", transactionRef: "" }];

    expect(validateCounterPayments(under, 1000)).toContain("add up to 900.00");
    expect(validateCounterPayments(over, 1000)).toContain("sale total is 1000.00");
  });

  it("requires a transaction code for M-Pesa and bank payments", () => {
    const lines = [{ method: "MPESA", amount: "1000", transactionRef: "  " }];

    expect(validateCounterPayments(lines, 1000)).toContain("transaction code");
  });

  it("rejects empty, zero and non-numeric amounts when there are several lines", () => {
    const blank = [
      { method: "CASH", amount: "500", transactionRef: "" },
      { method: "CASH", amount: "", transactionRef: "" },
    ];
    const text = [{ method: "CASH", amount: "abc", transactionRef: "" }];

    expect(validateCounterPayments(blank, 1000)).toContain("greater than 0");
    expect(validateCounterPayments(text, 1000)).toContain("greater than 0");
    expect(validateCounterPayments([], 1000)).toContain("at least one payment");
  });

  it("is not thrown off by floating point cents", () => {
    const lines = [
      { method: "CASH", amount: "0.1", transactionRef: "" },
      { method: "CASH", amount: "0.2", transactionRef: "" },
    ];

    expect(validateCounterPayments(lines, 0.3)).toBe("");
  });
});
