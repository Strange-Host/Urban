import { describe, expect, it } from "vitest";
import { orderTotals } from "./orderTotals";

describe("orderTotals", () => {
  it("adds the delivery fee on top of the discounted goods for online orders", () => {
    expect(orderTotals(1000, 100, 200, "ONLINE")).toEqual({
      discount: 100,
      deliveryFee: 200,
      grandTotal: 1100,
    });
  });

  it("ignores any delivery fee on counter sales", () => {
    expect(orderTotals(1000, 0, 200, "COUNTER").grandTotal).toBe(1000);
  });

  it("never goes below zero and treats blanks as zero", () => {
    expect(orderTotals(100, 500, "", "ONLINE").grandTotal).toBe(0);
    expect(orderTotals("", "", "", "ONLINE").grandTotal).toBe(0);
  });
});
