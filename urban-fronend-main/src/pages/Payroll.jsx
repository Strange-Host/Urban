import React, { useCallback, useEffect, useMemo, useState } from "react";
import { FiRefreshCw, FiUsers, FiDollarSign, FiMinusCircle, FiCheckCircle } from "react-icons/fi";
import StatCard from "../components/StatCard";
import { PageHeader, EmptyState, LoadingBlock } from "../components/ui";
import { alertError, tableCard } from "../components/styles";
import { useAuth } from "../auth/useAuth";
import { getPayrollSummary, getStaffPayroll } from "../api/payrollApi";
import { getErrorDetail } from "../utils/errors";
import { addDays, startOfWeek, toLocalIsoDate } from "../utils/dateRange";
import { MAX_PAYROLL_DAYS, validateRange } from "../utils/payrollRange";

function money(value) {
  return `KES ${Number(value || 0).toLocaleString()}`;
}

function thisWeek(now = new Date()) {
  return { from: toLocalIsoDate(startOfWeek(now)), to: toLocalIsoDate(now) };
}

function lastWeek(now = new Date()) {
  const monday = startOfWeek(now);
  return { from: toLocalIsoDate(addDays(monday, -7)), to: toLocalIsoDate(addDays(monday, -1)) };
}

function thisMonth(now = new Date()) {
  return { from: toLocalIsoDate(new Date(now.getFullYear(), now.getMonth(), 1)), to: toLocalIsoDate(now) };
}

const num = (v) => Number(v || 0);

export default function Payroll() {
  const { user, can } = useAuth();
  const seesEveryone = can("CAN_VIEW_PAYROLL_SUMMARY");

  const initial = useMemo(() => thisWeek(), []);
  const [from, setFrom] = useState(initial.from);
  const [to, setTo] = useState(initial.to);
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const load = useCallback(
    async (rangeFrom, rangeTo) => {
      const problem = validateRange(rangeFrom, rangeTo);
      if (problem) {
        setError(problem);
        return;
      }

      try {
        setLoading(true);
        setError("");

        if (seesEveryone) {
          setRows(await getPayrollSummary(rangeFrom, rangeTo));
        } else if (user?.id) {
          const mine = await getStaffPayroll(user.id, rangeFrom, rangeTo);
          setRows(mine ? [mine] : []);
        } else {
          setRows([]);
          setError("Your account details are missing from this session. Sign out and back in.");
        }
      } catch (e) {
        setRows([]);
        setError(getErrorDetail(e, "Failed to load payroll."));
      } finally {
        setLoading(false);
      }
    },
    [seesEveryone, user?.id]
  );

  useEffect(() => {
    load(initial.from, initial.to);
    // Load once with the default week; later loads come from the Apply button and presets.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [load]);

  function applyPreset(range) {
    setFrom(range.from);
    setTo(range.to);
    load(range.from, range.to);
  }

  const totals = useMemo(
    () =>
      rows.reduce(
        (acc, r) => ({
          paidOrders: acc.paidOrders + num(r.paidOrders),
          commission: acc.commission + num(r.commission),
          daysWorked: acc.daysWorked + num(r.daysWorked),
          dailySalary: acc.dailySalary + num(r.dailySalary),
          totalPayable: acc.totalPayable + num(r.totalPayable),
          salaryAdvances: acc.salaryAdvances + num(r.salaryAdvances),
          netPayable: acc.netPayable + num(r.netPayable),
        }),
        { paidOrders: 0, commission: 0, daysWorked: 0, dailySalary: 0, totalPayable: 0, salaryAdvances: 0, netPayable: 0 }
      ),
    [rows]
  );

  const presetBtn =
    "h-9 rounded-lg border border-slate-200 bg-white px-3 text-sm font-medium text-slate-700 hover:bg-slate-50";

  return (
    <div>
      <PageHeader
        title="Payroll"
        subtitle={seesEveryone ? "What each team member earned and is still owed" : "What you earned and are still owed"}
      >
        <div className="flex flex-wrap items-center gap-2">
          <button type="button" className={presetBtn} onClick={() => applyPreset(thisWeek())}>
            This week
          </button>
          <button type="button" className={presetBtn} onClick={() => applyPreset(lastWeek())}>
            Last week
          </button>
          <button type="button" className={presetBtn} onClick={() => applyPreset(thisMonth())}>
            This month
          </button>
        </div>

        <div className="flex flex-wrap items-center gap-2 rounded-xl border border-slate-200 bg-white p-1.5 shadow-sm">
          <input
            type="date"
            value={from}
            aria-label="From date"
            onChange={(e) => setFrom(e.target.value)}
            className="h-9 rounded-lg border border-transparent bg-slate-50 px-2.5 text-sm text-slate-700"
          />
          <span className="text-slate-400">→</span>
          <input
            type="date"
            value={to}
            aria-label="To date"
            onChange={(e) => setTo(e.target.value)}
            className="h-9 rounded-lg border border-transparent bg-slate-50 px-2.5 text-sm text-slate-700"
          />
          <button
            type="button"
            onClick={() => load(from, to)}
            disabled={loading}
            className="flex h-9 items-center gap-2 rounded-lg bg-brand-600 px-3.5 text-sm font-semibold text-white hover:bg-brand-700"
          >
            <FiRefreshCw size={14} />
            Apply
          </button>
        </div>
      </PageHeader>

      {error && (
        <div role="alert" style={{ ...alertError, marginBottom: 16 }}>
          {error}
        </div>
      )}

      <div className="mb-6 grid grid-cols-1 gap-4 min-[420px]:grid-cols-2 xl:grid-cols-4">
        <StatCard title="Commission" value={money(totals.commission)} tone="indigo" icon={FiUsers} />
        <StatCard title="Daily pay" value={money(totals.dailySalary)} tone="blue" icon={FiDollarSign} />
        <StatCard title="Advances paid" value={money(totals.salaryAdvances)} tone="orange" icon={FiMinusCircle} />
        <StatCard
          title="Still owed"
          value={money(totals.netPayable)}
          tone="green"
          icon={FiCheckCircle}
          hint={`Earned ${money(totals.totalPayable)} before advances`}
        />
      </div>

      <div style={tableCard}>
        {loading ? (
          <LoadingBlock label="Loading payroll..." />
        ) : rows.length === 0 ? (
          <EmptyState title="No payroll for this period" message="Nobody earned anything between these dates." />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[760px] text-left text-sm">
              <thead>
                <tr className="border-b border-slate-100 text-xs uppercase tracking-wide text-slate-500">
                  <th className="px-4 py-3 font-semibold">Staff</th>
                  <th className="px-4 py-3 text-right font-semibold">Paid orders</th>
                  <th className="px-4 py-3 text-right font-semibold">Commission</th>
                  <th className="px-4 py-3 text-right font-semibold">Days worked</th>
                  <th className="px-4 py-3 text-right font-semibold">Daily pay</th>
                  <th className="px-4 py-3 text-right font-semibold">Earned</th>
                  <th className="px-4 py-3 text-right font-semibold">Advances</th>
                  <th className="px-4 py-3 text-right font-semibold">Still owed</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((r) => (
                  <tr key={r.staffId} className="border-b border-slate-50">
                    <td className="px-4 py-3 font-medium text-slate-800">{r.staffName || "—"}</td>
                    <td className="px-4 py-3 text-right">{num(r.paidOrders)}</td>
                    <td className="px-4 py-3 text-right">{money(r.commission)}</td>
                    <td className="px-4 py-3 text-right">{num(r.daysWorked)}</td>
                    <td className="px-4 py-3 text-right">{money(r.dailySalary)}</td>
                    <td className="px-4 py-3 text-right">{money(r.totalPayable)}</td>
                    <td className="px-4 py-3 text-right">{money(r.salaryAdvances)}</td>
                    <td
                      className={`px-4 py-3 text-right font-semibold ${
                        num(r.netPayable) < 0 ? "text-rose-600" : "text-slate-900"
                      }`}
                    >
                      {money(r.netPayable)}
                    </td>
                  </tr>
                ))}
              </tbody>
              {rows.length > 1 && (
                <tfoot>
                  <tr className="bg-slate-50 font-semibold text-slate-900">
                    <td className="px-4 py-3">Total</td>
                    <td className="px-4 py-3 text-right">{totals.paidOrders}</td>
                    <td className="px-4 py-3 text-right">{money(totals.commission)}</td>
                    <td className="px-4 py-3 text-right">{totals.daysWorked}</td>
                    <td className="px-4 py-3 text-right">{money(totals.dailySalary)}</td>
                    <td className="px-4 py-3 text-right">{money(totals.totalPayable)}</td>
                    <td className="px-4 py-3 text-right">{money(totals.salaryAdvances)}</td>
                    <td className="px-4 py-3 text-right">{money(totals.netPayable)}</td>
                  </tr>
                </tfoot>
              )}
            </table>
          </div>
        )}
      </div>

      <p className="mt-4 text-xs text-slate-500">
        Weeks run Monday to Sunday. The commission rate for a week depends on that whole week&apos;s
        results, even when you pick only some of its days. A negative &ldquo;still owed&rdquo; means
        an advance was larger than what was earned in the period. Ranges are limited to{" "}
        {MAX_PAYROLL_DAYS} days.
      </p>
    </div>
  );
}
