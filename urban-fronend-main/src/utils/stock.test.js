import { describe, expect, it } from "vitest";
import { stockBadgeStyle } from "./stock";

describe("stockBadgeStyle", () => {
  it("uses the product's own reorder level when it has one", () => {
    // 30 in stock with a reorder level of 50 is low even though 30 > 15.
    expect(stockBadgeStyle(30, 50).color).toBe("#b45309");
    expect(stockBadgeStyle(50, 50).color).toBe("#b45309");
    expect(stockBadgeStyle(51, 50).color).toBe("#166534");
    // 4 in stock with a reorder level of 2 is fine even though 4 <= 5.
    expect(stockBadgeStyle(4, 2).color).toBe("#166534");
  });

  it("is red when out of stock regardless of the reorder level", () => {
    expect(stockBadgeStyle(0, 0).color).toBe("#991b1b");
    expect(stockBadgeStyle(0, 10).color).toBe("#991b1b");
  });

  it("falls back to the old thresholds without a reorder level", () => {
    expect(stockBadgeStyle(5).color).toBe("#991b1b");
    expect(stockBadgeStyle(15, null).color).toBe("#b45309");
    expect(stockBadgeStyle(16, "").color).toBe("#166534");
  });

  it("is neutral grey when the stock is not known", () => {
    expect(stockBadgeStyle("-", 10).color).toBe("#374151");
    expect(stockBadgeStyle(undefined).color).toBe("#374151");
  });
});
