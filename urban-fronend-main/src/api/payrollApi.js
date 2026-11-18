import { api } from "./api";

// Everyone's payroll for the period (needs the payroll-summary permission).
export async function getPayrollSummary(from, to) {
  const res = await api.get("/api/v1/payroll/summary", { params: { from, to } });
  return Array.isArray(res.data) ? res.data : [];
}

// One person's payroll; people without the summary permission may only ask for their own.
export async function getStaffPayroll(staffId, from, to) {
  const res = await api.get(`/api/v1/payroll/staff/${staffId}`, { params: { from, to } });
  return res.data;
}
