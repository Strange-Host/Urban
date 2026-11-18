import { describe, expect, it } from "vitest";
import {
  addDays,
  daysInRange,
  defaultRange,
  endOfDayIso,
  startOfDayIso,
  startOfWeek,
  toLocalIsoDate,
} from "./dateRange";

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

describe("payroll weeks", () => {
  it("starts the week on Monday, including for a Sunday", () => {
    expect(toLocalIsoDate(startOfWeek(new Date(2026, 10, 18)))).toBe("2026-11-16"); // Wednesday
    expect(toLocalIsoDate(startOfWeek(new Date(2026, 10, 16)))).toBe("2026-11-16"); // Monday
    expect(toLocalIsoDate(startOfWeek(new Date(2026, 10, 22)))).toBe("2026-11-16"); // Sunday
    expect(toLocalIsoDate(startOfWeek(new Date(2026, 10, 23)))).toBe("2026-11-23"); // next Monday
  });

  it("adds days across a month end", () => {
    expect(toLocalIsoDate(addDays(new Date(2026, 10, 29), 3))).toBe("2026-12-02");
    expect(toLocalIsoDate(addDays(new Date(2026, 10, 3), -7))).toBe("2026-10-27");
  });

  it("counts days inclusively", () => {
    expect(daysInRange("2026-11-18", "2026-11-18")).toBe(1);
    expect(daysInRange("2026-11-16", "2026-11-22")).toBe(7);
    expect(daysInRange("2026-01-01", "2026-04-02")).toBe(92);
  });
});
