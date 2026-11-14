import { describe, expect, it } from "vitest";
import { isValidPaybill } from "./Settings";

describe("isValidPaybill", () => {
  it("accepts 4-10 digit numbers and an empty value (clears it)", () => {
    expect(isValidPaybill("522533")).toBe(true);
    expect(isValidPaybill(" 522533 ")).toBe(true);
    expect(isValidPaybill("")).toBe(true);
  });

  it("rejects text, short and long values", () => {
    expect(isValidPaybill("abc")).toBe(false);
    expect(isValidPaybill("123")).toBe(false);
    expect(isValidPaybill("12345678901")).toBe(false);
    expect(isValidPaybill("<b>1</b>")).toBe(false);
  });
});
