import { describe, expect, it } from "vitest";
import { isAcceptablePassword } from "./passwordRules";

describe("isAcceptablePassword", () => {
  it("accepts long passwords with a letter and a number", () => {
    expect(isAcceptablePassword("Sunrise2026")).toBe(true);
  });

  it("rejects short, letters-only, digits-only and blank passwords", () => {
    expect(isAcceptablePassword("abc12")).toBe(false);
    expect(isAcceptablePassword("onlyletters")).toBe(false);
    expect(isAcceptablePassword("12345678")).toBe(false);
    expect(isAcceptablePassword("        ")).toBe(false);
    expect(isAcceptablePassword(undefined)).toBe(false);
  });
});
