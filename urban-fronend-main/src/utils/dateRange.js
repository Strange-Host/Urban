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

// Whole-day boundaries for APIs that take a date-time. Ending a day at 23:59:59 dropped every
// record created in that last second; the server treats the end as inclusive to the microsecond.
export function startOfDayIso(date) {
  return `${date}T00:00:00`;
}

export function endOfDayIso(date) {
  return `${date}T23:59:59.999999`;
}

// Payroll weeks run Monday to Sunday, matching the server.
export function startOfWeek(date = new Date()) {
  const d = new Date(date.getFullYear(), date.getMonth(), date.getDate());
  const sinceMonday = (d.getDay() + 6) % 7; // Sunday is 0 in JS
  d.setDate(d.getDate() - sinceMonday);
  return d;
}

export function addDays(date, days) {
  const d = new Date(date.getFullYear(), date.getMonth(), date.getDate());
  d.setDate(d.getDate() + days);
  return d;
}

// Inclusive number of calendar days between two YYYY-MM-DD strings.
export function daysInRange(from, to) {
  const [fy, fm, fd] = from.split("-").map(Number);
  const [ty, tm, td] = to.split("-").map(Number);
  const ms = Date.UTC(ty, tm - 1, td) - Date.UTC(fy, fm - 1, fd);
  return Math.round(ms / 86400000) + 1;
}
