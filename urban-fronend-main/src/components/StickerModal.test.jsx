import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("../api/stickersApi", async () => {
  const actual = await vi.importActual("../api/stickersApi");
  return { ...actual, fetchStickerBatches: vi.fn(), reprintBatch: vi.fn() };
});

import { fetchStickerBatches, reprintBatch } from "../api/stickersApi";
import StickerModal from "./StickerModal";

const batches = [
  { batchId: "b-new", productId: "p1", productName: "Leather Wallet", units: 24, createdAt: "2026-11-14T09:30:00" },
  { batchId: "b-old", productId: "p1", productName: "Leather Wallet", units: 10, createdAt: "2026-11-01T08:00:00" },
];

describe("StickerModal reprint", () => {
  beforeEach(() => {
    fetchStickerBatches.mockReset();
    reprintBatch.mockReset();
    URL.createObjectURL = vi.fn(() => "blob:x");
    URL.revokeObjectURL = vi.fn();
    window.open = vi.fn();
  });

  it("lets you pick a recent batch instead of typing its id", async () => {
    fetchStickerBatches.mockResolvedValue(batches);
    reprintBatch.mockResolvedValue({ blob: new Blob(["x"]), batchId: "b-old" });

    render(<StickerModal mode="reprint" product={{ id: "p1", name: "Leather Wallet" }} onClose={() => {}} />);

    const select = await screen.findByLabelText("Printed batch");
    expect(fetchStickerBatches).toHaveBeenCalledWith("p1");
    expect(select.querySelectorAll("option").length).toBe(3); // placeholder + 2 batches

    fireEvent.change(select, { target: { value: "b-old" } });
    fireEvent.click(screen.getByRole("button", { name: /reprint/i }));

    await waitFor(() => expect(reprintBatch).toHaveBeenCalledWith("b-old", expect.anything()));
  });

  it("still works with a pasted id when no batches can be listed", async () => {
    fetchStickerBatches.mockRejectedValue(new Error("nope"));

    render(<StickerModal mode="reprint" product={null} onClose={() => {}} />);

    await waitFor(() => expect(fetchStickerBatches).toHaveBeenCalled());
    expect(screen.queryByLabelText("Printed batch")).toBeNull();
    expect(screen.getByPlaceholderText("…or paste a batch ID")).toBeTruthy();
  });
});
