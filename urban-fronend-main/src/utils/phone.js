/**
 * Canonical Kenyan mobile number (07XXXXXXXX / 01XXXXXXXX) from any common spelling:
 * 0712 345 678, +254 712 345 678, 254712345678, 712345678. Returns null if it is not one.
 * Mirrors the server's PhoneNumbers.normalize so both sides agree.
 */
export function normalizeKenyanPhone(raw) {
  const compact = String(raw ?? "").trim().replace(/[\s\-().]/g, "");
  const digits = compact.startsWith("+") ? compact.slice(1) : compact;
  if (!/^\d+$/.test(digits)) return null;

  let nine = null;
  if (digits.startsWith("254") && digits.length === 12) nine = digits.slice(3);
  else if (digits.startsWith("0") && digits.length === 10) nine = digits.slice(1);
  else if (digits.length === 9) nine = digits;

  return nine && /^[17]/.test(nine) ? `0${nine}` : null;
}
