import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("../api/payrollApi", () => ({
  getPayrollSummary: vi.fn(),
  getStaffPayroll: vi.fn(),
}));

const auth = { user: { id: "u1", role: "ADMIN" }, can: () => true };
vi.mock("../auth/useAuth", () => ({ useAuth: () => auth }));

import { getPayrollSummary, getStaffPayroll } from "../api/payrollApi";
import Payroll from "./Payroll";
import { MAX_PAYROLL_DAYS, validateRange } from "../utils/payrollRange";

const row = (id, name, over = {}) => ({
  staffId: id,
  staffName: name,
  paidOrders: 4,
  commission: 200,
  daysWorked: 5,
  dailySalary: 1500,
  totalPayable: 1700,
  salaryAdvances: 500,
  netPayable: 1200,
  ...over,
});

describe("validateRange", () => {
  it("accepts a normal range and the 92 day limit", () => {
    expect(validateRange("2026-11-16", "2026-11-22")).toBe("");
    expect(validateRange("2026-01-01", "2026-04-02")).toBe(""); // exactly 92 days
  });

  it("rejects missing, reversed and over-long ranges", () => {
    expect(validateRange("", "2026-11-22")).toMatch(/both/i);
    expect(validateRange("2026-11-22", "2026-11-16")).toMatch(/before/i);
    expect(validateRange("2026-01-01", "2026-04-03")).toMatch(String(MAX_PAYROLL_DAYS));
  });
});

describe("Payroll page", () => {
  beforeEach(() => {
    getPayrollSummary.mockReset();
    getStaffPayroll.mockReset();
    auth.can = () => true;
  });

  it("shows every staff member with advances taken off what is still owed", async () => {
    getPayrollSummary.mockResolvedValue([
      row("a", "Jane Doe"),
      row("b", "John Roe", { totalPayable: 360, salaryAdvances: 0, netPayable: 360 }),
    ]);

    render(<Payroll />);

    await waitFor(() => expect(screen.getByText("Jane Doe")).toBeTruthy());
    expect(screen.getByText("John Roe")).toBeTruthy();
    // totals row: 1200 + 360 still owed
    expect(screen.getAllByText("KES 1,560").length).toBeGreaterThan(0);
  });

  it("marks a negative balance (advance larger than earnings)", async () => {
    getPayrollSummary.mockResolvedValue([
      row("a", "Jane Doe", { totalPayable: 30, salaryAdvances: 1000, netPayable: -970 }),
    ]);

    render(<Payroll />);

    const owed = await screen.findAllByText("KES -970");
    expect(owed.some((el) => el.className.includes("text-rose-600"))).toBe(true);
  });

  it("loads only the signed-in person's payroll without the summary permission", async () => {
    auth.can = (p) => p === "CAN_VIEW_OWN_PAYROLL";
    getStaffPayroll.mockResolvedValue(row("u1", "Me Myself"));

    render(<Payroll />);

    await waitFor(() => expect(screen.getByText("Me Myself")).toBeTruthy());
    expect(getPayrollSummary).not.toHaveBeenCalled();
    expect(getStaffPayroll).toHaveBeenCalledWith("u1", expect.any(String), expect.any(String));
  });

  it("explains a too-long range without calling the server", async () => {
    getPayrollSummary.mockResolvedValue([]);
    render(<Payroll />);
    await waitFor(() => expect(getPayrollSummary).toHaveBeenCalledTimes(1));

    fireEvent.change(screen.getByLabelText("From date"), { target: { value: "2026-01-01" } });
    fireEvent.change(screen.getByLabelText("To date"), { target: { value: "2026-12-31" } });
    fireEvent.click(screen.getByRole("button", { name: /apply/i }));

    expect((await screen.findByRole("alert")).textContent).toMatch(/at most 92 days/);
    expect(getPayrollSummary).toHaveBeenCalledTimes(1);
  });

  it("shows the server's reason when loading fails", async () => {
    getPayrollSummary.mockRejectedValue({
      response: { status: 400, data: { detail: "Choose a range of at most 92 days (you asked for 100)" } },
    });

    render(<Payroll />);

    expect((await screen.findByRole("alert")).textContent).toMatch(/you asked for 100/);
  });
});
