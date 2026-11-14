import { useEffect, useState } from "react";
import { fetchSettings } from "../api/settingsApi";

// Shop settings are edited by an admin on the Settings page and stored on the
// server, so every device prints the same paybill. The value is cached for the
// session; nothing here is hard-coded because a wrong number on a receipt sends
// customer money to the wrong place.
let cachedPaybill = null;
let inflight = null;

const listeners = new Set();

function publish(value) {
  cachedPaybill = value;
  listeners.forEach((listener) => listener(value));
}

/** Resolves to the paybill, or "" if none is configured or settings can't be loaded. */
export async function loadMpesaPaybill() {
  if (cachedPaybill !== null) return cachedPaybill;

  if (!inflight) {
    inflight = fetchSettings()
      .then((settings) => String(settings?.mpesaPaybill ?? "").trim())
      .catch(() => "")
      .then((value) => {
        inflight = null;
        // Do not cache a failed load; try again next time.
        return value;
      });
  }

  const value = await inflight;
  if (value) cachedPaybill = value;
  return value;
}

/** Call after the admin saves a new value so open screens update. */
export function setCachedMpesaPaybill(value) {
  publish(String(value ?? "").trim());
}

export function resetSettingsCache() {
  cachedPaybill = null;
  inflight = null;
}

export function useMpesaPaybill() {
  const [paybill, setPaybill] = useState(cachedPaybill ?? "");

  useEffect(() => {
    let active = true;
    listeners.add(setPaybill);
    loadMpesaPaybill().then((value) => {
      if (active) setPaybill(value);
    });
    return () => {
      active = false;
      listeners.delete(setPaybill);
    };
  }, []);

  return paybill;
}
