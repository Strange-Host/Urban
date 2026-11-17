import { describe, expect, it } from "vitest";
import { normalizeKenyanPhone } from "./phone";

describe("normalizeKenyanPhone", () => {
  it("turns every common spelling into the local 07 form", () => {
    for (const raw of ["0712345678", "0712 345 678", "+254 712 345 678", "254712345678", "712345678", "(0712) 345-678"]) {
      expect(normalizeKenyanPhone(raw)).toBe("0712345678");
    }
    expect(normalizeKenyanPhone("+254110123456")).toBe("0110123456");
  });

  it("rejects anything that is not a Kenyan mobile", () => {
    for (const raw of ["", "abc", "12345", "0212345678", "+14155550100", "07123456789", null, undefined]) {
      expect(normalizeKenyanPhone(raw)).toBeNull();
    }
  });
});
