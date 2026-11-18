import { describe, expect, it } from "vitest";
import { canAccessPath } from "./routeAccess";

const canOnly = (...granted) => (permission) => granted.includes(permission);
const staff = { role: "STAFF" };
const admin = { role: "ADMIN" };

describe("canAccessPath", () => {
  it("always allows the dashboard and pages that are not in the menu", () => {
    expect(canAccessPath("/dashboard", canOnly(), staff)).toBe(true);
    expect(canAccessPath("/staff-details", canOnly(), staff)).toBe(true);
    expect(canAccessPath("/something-else", canOnly(), staff)).toBe(true);
  });

  it("blocks a page whose permission the user lacks, including its sub-pages", () => {
    expect(canAccessPath("/crm", canOnly(), staff)).toBe(false);
    expect(canAccessPath("/crm/leads/5", canOnly(), staff)).toBe(false);
    expect(canAccessPath("/crm/leads/5", canOnly("CAN_VIEW_LEADS"), staff)).toBe(true);
    expect(canAccessPath("/crm/", canOnly("CAN_VIEW_LEADS"), staff)).toBe(true);
  });

  it("does not confuse /staff with /staff-logs or /staff-details", () => {
    expect(canAccessPath("/staff", canOnly("CAN_VIEW_STAFF_LOGS"), staff)).toBe(false);
    expect(canAccessPath("/staff-logs", canOnly("CAN_VIEW_STAFF_LOGS"), staff)).toBe(true);
  });

  it("honours admin-only pages", () => {
    expect(canAccessPath("/settings", canOnly(), staff)).toBe(false);
    expect(canAccessPath("/settings", canOnly(), admin)).toBe(true);
  });

  it("uses the create permission for the new-requisition page", () => {
    expect(canAccessPath("/requisitions/new", canOnly("CAN_VIEW_REQUISITIONS"), staff)).toBe(false);
    expect(canAccessPath("/requisitions/new", canOnly("CAN_CREATE_REQUISITIONS"), staff)).toBe(true);
    expect(canAccessPath("/requisitions", canOnly("CAN_VIEW_REQUISITIONS"), staff)).toBe(true);
  });

  it("lets either payroll permission open the payroll page", () => {
    expect(canAccessPath("/payroll", canOnly(), staff)).toBe(false);
    expect(canAccessPath("/payroll", canOnly("CAN_VIEW_OWN_PAYROLL"), staff)).toBe(true);
    expect(canAccessPath("/payroll", canOnly("CAN_VIEW_PAYROLL_SUMMARY"), staff)).toBe(true);
  });
});
