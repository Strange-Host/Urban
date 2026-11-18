import { describe, expect, it } from "vitest";
import { defaultRange, endOfDayIso, startOfDayIso, toLocalIsoDate } from "./dateRange";

describe("dateRange", () => {
  it("formats local dates with zero padding", () => {
    expect(toLocalIsoDate(new Date(2026, 0, 5))).toBe("2026-01-05");
  });

  it("builds a rolling range that always ends today", () => {
    const now = new Date(2027, 2, 10);
    expect(defaultRange(30, now)).toEqual({ from: "2027-02-08", to: "2027-03-10" });
  });
});

describe("whole-day boundaries", () => {
  it("covers the whole day including the last second", () => {
    expect(startOfDayIso("2026-11-18")).toBe("2026-11-18T00:00:00");
    expect(endOfDayIso("2026-11-18")).toBe("2026-11-18T23:59:59.999999");
  });
});
