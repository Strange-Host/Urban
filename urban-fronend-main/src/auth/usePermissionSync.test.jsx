import { renderHook } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("../api/api", () => ({ refreshSessionQuietly: vi.fn().mockResolvedValue(true) }));

import { refreshSessionQuietly } from "../api/api";
import { SYNC_INTERVAL_MS, usePermissionSync } from "./usePermissionSync";

describe("usePermissionSync", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    refreshSessionQuietly.mockClear();
    localStorage.clear();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("refreshes on the interval while a session exists", () => {
    localStorage.setItem("refreshToken", "r");
    renderHook(() => usePermissionSync());

    vi.advanceTimersByTime(SYNC_INTERVAL_MS + 1000);

    expect(refreshSessionQuietly).toHaveBeenCalledTimes(1);
  });

  it("does nothing when nobody is signed in", () => {
    renderHook(() => usePermissionSync());

    vi.advanceTimersByTime(SYNC_INTERVAL_MS * 2);

    expect(refreshSessionQuietly).not.toHaveBeenCalled();
  });

  it("stops refreshing after unmount", () => {
    localStorage.setItem("refreshToken", "r");
    const { unmount } = renderHook(() => usePermissionSync());
    unmount();

    vi.advanceTimersByTime(SYNC_INTERVAL_MS * 2);

    expect(refreshSessionQuietly).not.toHaveBeenCalled();
  });
});
