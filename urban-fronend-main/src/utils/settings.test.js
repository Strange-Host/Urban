import { beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("../api/settingsApi", () => ({ fetchSettings: vi.fn() }));

import { fetchSettings } from "../api/settingsApi";
import { loadMpesaPaybill, resetSettingsCache, setCachedMpesaPaybill } from "./settings";

describe("loadMpesaPaybill", () => {
  beforeEach(() => {
    resetSettingsCache();
    fetchSettings.mockReset();
  });

  it("returns the paybill configured on the server and caches it", async () => {
    fetchSettings.mockResolvedValue({ mpesaPaybill: " 522533 " });

    expect(await loadMpesaPaybill()).toBe("522533");
    expect(await loadMpesaPaybill()).toBe("522533");
    expect(fetchSettings).toHaveBeenCalledTimes(1);
  });

  it("returns an empty string (no guessed default) when none is set", async () => {
    fetchSettings.mockResolvedValue({ mpesaPaybill: "" });

    expect(await loadMpesaPaybill()).toBe("");
  });

  it("returns an empty string when settings cannot be loaded and retries next time", async () => {
    fetchSettings.mockRejectedValueOnce(new Error("offline"));
    fetchSettings.mockResolvedValueOnce({ mpesaPaybill: "522533" });

    expect(await loadMpesaPaybill()).toBe("");
    expect(await loadMpesaPaybill()).toBe("522533");
  });

  it("uses a value pushed after the admin saves", async () => {
    setCachedMpesaPaybill("700700");

    expect(await loadMpesaPaybill()).toBe("700700");
    expect(fetchSettings).not.toHaveBeenCalled();
  });
});
