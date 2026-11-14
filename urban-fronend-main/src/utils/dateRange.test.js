import { describe, expect, it } from "vitest";
import { defaultRange, toLocalIsoDate } from "./dateRange";

describe("dateRange", () => {
  it("formats local dates with zero padding", () => {
    expect(toLocalIsoDate(new Date(2026, 0, 5))).toBe("2026-01-05");
  });

  it("builds a rolling range that always ends today", () => {
    const now = new Date(2027, 2, 10);
    expect(defaultRange(30, now)).toEqual({ from: "2027-02-08", to: "2027-03-10" });
  });
});
