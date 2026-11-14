import { afterEach, describe, expect, it, vi } from "vitest";
import { getMpesaPaybill } from "./settings";

describe("getMpesaPaybill", () => {
  afterEach(() => vi.unstubAllEnvs());

  it("returns the configured numeric paybill", () => {
    vi.stubEnv("VITE_MPESA_PAYBILL", " 522533 ");
    expect(getMpesaPaybill()).toBe("522533");
  });

  it("returns an empty string when unset or malformed", () => {
    vi.stubEnv("VITE_MPESA_PAYBILL", "");
    expect(getMpesaPaybill()).toBe("");
    vi.stubEnv("VITE_MPESA_PAYBILL", "<b>1</b>");
    expect(getMpesaPaybill()).toBe("");
  });
});
