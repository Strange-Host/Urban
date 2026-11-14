// The backend returns errors as RFC 7807 problem details ({ title, detail }), not
// { message }. Reading `message` always fell through to a generic string and hid the
// real reason ("Insufficient stock", "Payment exceeds the order balance", ...).
export function getErrorDetail(error, fallback = "Something went wrong.") {
  const data = error?.response?.data;

  if (typeof data === "string" && data.trim()) return data;
  if (typeof data?.detail === "string" && data.detail.trim()) return data.detail;
  if (typeof data?.message === "string" && data.message.trim()) return data.message;

  return fallback;
}
