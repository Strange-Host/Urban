import { describe, expect, it } from "vitest";
import {
  ORDER_STATUSES,
  formatDateTime,
  formatMoney,
  getOrderProductLines,
  getParcelAgeHighlight,
  niceStatus,
  previewText,
  statusStyle,
} from "./orderUtils";

const DAY_MS = 24 * 60 * 60 * 1000;

function daysAgo(days) {
  return new Date(Date.now() - days * DAY_MS).toISOString();
}

describe("orderUtils", () => {
  it("exposes the paybill and the full status list", () => {
    expect(ORDER_STATUSES).toContain("AWAITING_DISPATCH");
    expect(ORDER_STATUSES).toContain("COMPLETED");
    expect(new Set(ORDER_STATUSES).size).toBe(ORDER_STATUSES.length);
  });

  it("formats money with a KES prefix", () => {
    expect(formatMoney(1500)).toBe("KES 1,500");
    expect(formatMoney("2500")).toBe("KES 2,500");
    expect(formatMoney(null)).toBe("KES 0");
  });

  it("formats dates and falls back to a dash", () => {
    expect(formatDateTime(null)).toBe("-");
    expect(formatDateTime("2026-01-02T08:05:00")).toMatch(/^2026-01-02 \d{2}:\d{2}$/);
  });

  it("maps statuses to badge colours", () => {
    expect(statusStyle("DELIVERED_PAID")).toEqual({ color: "#166534", bg: "#dcfce7" });
    expect(statusStyle("COMPLETED")).toEqual({ color: "#166534", bg: "#dcfce7" });
    expect(statusStyle("DISPATCHED_UNPAID")).toEqual({ color: "#1d4ed8", bg: "#dbeafe" });
    expect(statusStyle("AWAITING_DISPATCH")).toEqual({ color: "#b45309", bg: "#ffedd5" });
    expect(statusStyle("CANCELLED")).toEqual({ color: "#9f1239", bg: "#ffe4e6" });
    expect(statusStyle("SOMETHING_ELSE")).toEqual({ color: "#374151", bg: "#f3f4f6" });
  });

  it("turns enum names into readable labels", () => {
    expect(niceStatus("DISPATCHED_PARTIAL")).toBe("Dispatched Partial");
    expect(niceStatus(null)).toBe("");
  });

  it("highlights long-running parcels by age", () => {
    expect(getParcelAgeHighlight(null)).toBeNull();
    expect(getParcelAgeHighlight({ createdAt: "not-a-date" })).toBeNull();
    expect(
      getParcelAgeHighlight({ createdAt: daysAgo(1), orderStatus: "DISPATCHED_PAID" })
    ).toBe("#0ea5a4");
    expect(
      getParcelAgeHighlight({ createdAt: daysAgo(1), orderStatus: "DELIVERED_PAID" })
    ).toBe("#16a34a");
    expect(
      getParcelAgeHighlight({ createdAt: daysAgo(9), orderStatus: "DISPATCHED_UNPAID" })
    ).toBe("#f97316");
    expect(
      getParcelAgeHighlight({ createdAt: daysAgo(15), orderStatus: "AWAITING_DISPATCH" })
    ).toBe("#dc2626");
    expect(
      getParcelAgeHighlight({ createdAt: daysAgo(2), orderStatus: "AWAITING_DISPATCH" })
    ).toBeNull();
  });

  it("truncates delivery notes", () => {
    expect(previewText("")).toBe("No delivery notes");
    expect(previewText("short note")).toBe("short note");
    expect(previewText("x".repeat(70))).toHaveLength(61);
  });

  it("builds product lines from items, fallbacks and legacy fields", () => {
    expect(getOrderProductLines({ items: [{ productName: "Shoes", quantity: 2 }] }))
      .toEqual(["Shoes (×2)"]);
    expect(getOrderProductLines({ orderItems: [{ product: { name: "Bag" } }] })).toEqual(["Bag"]);
    expect(getOrderProductLines({ products: ["Scarf"] })).toEqual(["Scarf"]);
    expect(getOrderProductLines({ productName: "Coat" })).toEqual(["Coat"]);
    expect(getOrderProductLines({})).toEqual(["-"]);
  });
});
