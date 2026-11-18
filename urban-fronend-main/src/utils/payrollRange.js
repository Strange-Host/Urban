import { daysInRange } from "./dateRange";

// Same limit the server enforces; checking here gives a clear message without a round trip.
export const MAX_PAYROLL_DAYS = 92;

// Returns an explanation of what is wrong with the range, or "" when it is fine.
export function validateRange(from, to) {
  if (!from || !to) return "Pick both a start and an end date.";
  if (to < from) return "The end date cannot be before the start date.";
  const days = daysInRange(from, to);
  if (days > MAX_PAYROLL_DAYS) {
    return `Choose a range of at most ${MAX_PAYROLL_DAYS} days (you chose ${days}).`;
  }
  return "";
}
