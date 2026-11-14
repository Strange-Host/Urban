// Local (not UTC) YYYY-MM-DD, so "today" is the user's today.
export function toLocalIsoDate(date) {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

// Rolling default range ending today, e.g. the last 30 days.
export function defaultRange(days = 30, now = new Date()) {
  const start = new Date(now);
  start.setDate(start.getDate() - days);
  return { from: toLocalIsoDate(start), to: toLocalIsoDate(now) };
}
