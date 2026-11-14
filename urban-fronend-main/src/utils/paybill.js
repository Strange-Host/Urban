/** Empty is valid (it clears the setting); otherwise 4-10 digits, same as the server. */
export function isValidPaybill(value) {
  const trimmed = String(value ?? "").trim();
  return trimmed === "" || /^\d{4,10}$/.test(trimmed);
}
