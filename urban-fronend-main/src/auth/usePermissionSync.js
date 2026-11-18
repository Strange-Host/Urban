import { useEffect } from "react";
import { refreshSessionQuietly } from "../api/api";

export const SYNC_INTERVAL_MS = 5 * 60 * 1000;
export const MIN_GAP_MS = 60 * 1000;

function hasSession() {
  const refresh = localStorage.getItem("refreshToken");
  return Boolean(refresh) && refresh !== "undefined" && refresh !== "null";
}

/**
 * Keeps the access token (and so the permissions the UI reads from it) current: refreshes every
 * few minutes while the tab is open, and when the tab becomes visible again after a pause.
 */
export function usePermissionSync() {
  useEffect(() => {
    let lastSync = Date.now();

    const sync = async () => {
      if (!hasSession() || Date.now() - lastSync < MIN_GAP_MS) return;
      lastSync = Date.now();
      await refreshSessionQuietly();
    };

    const timer = setInterval(() => {
      if (document.visibilityState === "visible") sync();
    }, SYNC_INTERVAL_MS);

    const onVisible = () => {
      if (document.visibilityState === "visible") sync();
    };
    document.addEventListener("visibilitychange", onVisible);

    return () => {
      clearInterval(timer);
      document.removeEventListener("visibilitychange", onVisible);
    };
  }, []);
}
