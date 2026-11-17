import { describe, expect, it } from "vitest";
import { batchLabel } from "./stickerBatches";

describe("batchLabel", () => {
  it("shows product, sticker count and date", () => {
    const label = batchLabel({ productName: "Leather Wallet", units: 24, createdAt: "2026-11-14T09:30:00" });

    expect(label).toContain("Leather Wallet");
    expect(label).toContain("24 stickers");
    expect(label).toMatch(/14/);
  });

  it("uses the singular and can leave the product out", () => {
    expect(batchLabel({ productName: "Wallet", units: 1, createdAt: null }, false)).toBe("1 sticker");
  });
});
