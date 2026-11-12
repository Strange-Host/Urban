import { describe, expect, it } from "vitest";
import { formatDateTime, formatPhoneLink, formatProductOptions, statTones } from "./crmUtils";

describe("crmUtils", () => {
  it("provides a tone class for every stat card colour", () => {
    expect(statTones.green).toContain("emerald");
    expect(Object.keys(statTones)).toEqual(
      expect.arrayContaining(["slate", "blue", "green", "violet", "red", "amber"])
    );
  });

  it("formats dates and returns an empty string when missing", () => {
    expect(formatDateTime(null)).toBe("");
    expect(formatDateTime("2026-03-04T09:30:00")).toMatch(/^2026-03-04 \d{2}:\d{2} (AM|PM)$/);
  });

  it("builds tel links from messy phone numbers", () => {
    expect(formatPhoneLink("")).toBe("");
    expect(formatPhoneLink("0712 345 678")).toBe("tel:0712345678");
    expect(formatPhoneLink("+254 (712) 345-678")).toBe("tel:+254712345678");
    expect(formatPhoneLink("   ")).toBe("");
  });

  it("renders product option JSON into readable labels", () => {
    expect(formatProductOptions(null)).toBeNull();
    expect(formatProductOptions("not json")).toBeNull();
    expect(formatProductOptions("{}")).toBeNull();

    const options = formatProductOptions(
      JSON.stringify({
        "Available Colors": ["Red", "Blue"],
        "Choose size": "M",
        Material: "Cotton",
      })
    );

    expect(options).toContain("Colors: Red, Blue");
    expect(options).toContain("Size: M");
    expect(options).toContain("Material: Cotton");
    expect(options.split(" · ")).toHaveLength(3);
  });
});
