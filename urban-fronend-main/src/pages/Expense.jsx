import React, { useEffect, useMemo, useState } from "react";
import { FiPlus, FiRefreshCw, FiDollarSign, FiFileText } from "react-icons/fi";
import { api } from "../api/api";
import StatCard from "../components/StatCard";
import { PageHeader, EmptyState, LoadingBlock } from "../components/ui";
import {
  inputStyle,
  labelStyle,
  primaryBtn,
  secondaryBtn,
  modalOverlay,
  modalBox,
  modalTitle,
  modalFooter,
  tableCard,
  alertError,
  badgeStyle,
} from "../components/styles";

export default function Expenses() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  // list
  const [expenses, setExpenses] = useState([]);

  // modal
  const [isAddOpen, setIsAddOpen] = useState(false);

  // form (matches backend)
  const [form, setForm] = useState({
    expenseType: "MISC",
    amount: "",
    description: "",
    paymentMethod: "CASH",
    referenceCode: "",
  });

  const totalExpenses = useMemo(() => {
    return expenses.reduce((sum, e) => sum + Number(e.amount || 0), 0);
  }, [expenses]);

  useEffect(() => {
    loadExpenses();
    // eslint-disable-next-line
  }, []);

  async function loadExpenses() {
    try {
      setLoading(true);
      setError("");

      // If your backend has pagination, update this
      const res = await api.get("/api/v1/expenses", {
        params: { page: 0, size: 200, sort: "createdAt,desc" },
      });

      // supports both Page and List response
      const data = res.data?.content || res.data || [];
      setExpenses(data);
    } catch (e) {
      console.log(e);
      setError("Failed to load expenses.");
    } finally {
      setLoading(false);
    }
  }

  async function addExpense() {
    try {
      setLoading(true);
      setError("");

      if (!form.amount || Number(form.amount) <= 0) {
        setError("Amount must be greater than 0.");
        return;
      }

      await api.post("/api/v1/expenses", {
        expenseType: form.expenseType,
        amount: Number(form.amount),
        description: form.description,
        paymentMethod: form.paymentMethod,
        referenceCode: form.referenceCode,
      });

      setIsAddOpen(false);

      setForm({
        expenseType: "MISC",
        amount: "",
        description: "",
        paymentMethod: "CASH",
        referenceCode: "",
      });

      await loadExpenses();
    } catch (e) {
      console.log(e);
      setError("Failed to add expense.");
    } finally {
      setLoading(false);
    }
  }

  function money(value) {
    return `KES ${Number(value || 0).toLocaleString()}`;
  }

  const label = (v) => (v ? String(v).charAt(0) + String(v).slice(1).toLowerCase() : "-");

  return (
    <div>
      <PageHeader title="Expenses" subtitle="Record and review business expenses">
        <button onClick={loadExpenses} disabled={loading} style={secondaryBtn}>
          <FiRefreshCw size={15} /> Refresh
        </button>
        <button onClick={() => setIsAddOpen(true)} style={primaryBtn}>
          <FiPlus size={16} /> Add Expense
        </button>
      </PageHeader>

      {/* Error */}
      {error && !isAddOpen && <div style={{ ...alertError, marginBottom: 16 }}>{error}</div>}

      {/* Summary */}
      <div className="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <StatCard title="Total Expenses" value={money(totalExpenses)} tone="red" icon={FiDollarSign} />
        <StatCard title="Records" value={expenses.length.toLocaleString()} tone="slate" icon={FiFileText} />
      </div>

      {/* List */}
      <div style={tableCard}>
        <div className="flex items-center justify-between border-b border-slate-100 px-5 py-4">
          <h3 className="m-0 text-base font-semibold text-slate-900">Expenses List</h3>
        </div>

        {loading && expenses.length === 0 ? (
          <LoadingBlock label="Loading expenses..." />
        ) : expenses.length === 0 ? (
          <EmptyState icon={FiDollarSign} title="No expenses recorded" message="Add an expense to start tracking spend." />
        ) : (
          <>
            {/* Desktop table */}
            <div className="hidden overflow-x-auto md:block">
              <table className="ui-table">
                <thead>
                  <tr>
                    <th>Type</th>
                    <th>Amount</th>
                    <th>Payment</th>
                    <th>Reference</th>
                    <th>Description</th>
                  </tr>
                </thead>
                <tbody>
                  {expenses.map((e) => (
                    <tr key={e.id}>
                      <td>
                        <span style={badgeStyle("indigo")}>{label(e.expenseType)}</span>
                      </td>
                      <td style={{ fontWeight: 600 }}>{money(e.amount)}</td>
                      <td>
                        <span style={badgeStyle("gray")}>{e.paymentMethod}</span>
                      </td>
                      <td className="font-mono text-[13px] text-slate-500">{e.referenceCode || "—"}</td>
                      <td className="text-slate-500">{e.description || "—"}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Mobile cards */}
            <div className="divide-y divide-slate-100 md:hidden">
              {expenses.map((e) => (
                <div key={`${e.id}-mobile`} className="p-4">
                  <div className="flex items-center justify-between gap-3">
                    <span style={badgeStyle("indigo")}>{label(e.expenseType)}</span>
                    <span className="font-semibold text-slate-900">{money(e.amount)}</span>
                  </div>
                  <div className="mt-2 flex flex-wrap items-center gap-x-3 gap-y-1 text-sm text-slate-500">
                    <span>{e.paymentMethod}</span>
                    {e.referenceCode && <span className="font-mono text-xs">{e.referenceCode}</span>}
                  </div>
                  {e.description && <div className="mt-1 text-sm text-slate-600">{e.description}</div>}
                </div>
              ))}
            </div>
          </>
        )}
      </div>

      {/* Add Expense Modal */}
      {isAddOpen && (
        <div style={modalOverlay} onClick={() => setIsAddOpen(false)}>
          <div style={modalBox} onClick={(ev) => ev.stopPropagation()}>
            <h3 style={modalTitle}>Add Expense</h3>

            {error && <div style={{ ...alertError, marginBottom: 14 }}>{error}</div>}

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div>
                <label style={labelStyle}>Type</label>
                <select
                  value={form.expenseType}
                  onChange={(e) => setForm({ ...form, expenseType: e.target.value })}
                  style={inputStyle}
                >
                  <option value="MARKETING">Marketing</option>
                  <option value="FACILITATION">Facilitation</option>
                  <option value="RENT">Rent</option>
                  <option value="SHOP_SUPPLIES">Shop supplies</option>
                  <option value="TRANSPORT">Transport</option>
                  <option value="SALARY">Salary</option>
                  <option value="UTILITIES">Utilities</option>
                  <option value="MISC">Miscellaneous</option>
                </select>
              </div>

              <div>
                <label style={labelStyle}>Amount (KES)</label>
                <input
                  type="number"
                  placeholder="0"
                  value={form.amount}
                  onChange={(e) => setForm({ ...form, amount: e.target.value })}
                  style={inputStyle}
                />
              </div>

              <div>
                <label style={labelStyle}>Payment method</label>
                <select
                  value={form.paymentMethod}
                  onChange={(e) => setForm({ ...form, paymentMethod: e.target.value })}
                  style={inputStyle}
                >
                  <option value="CASH">Cash</option>
                  <option value="MPESA">M-Pesa</option>
                  <option value="BANK">Bank</option>
                </select>
              </div>

              <div>
                <label style={labelStyle}>Reference code</label>
                <input
                  placeholder="Optional"
                  value={form.referenceCode}
                  onChange={(e) => setForm({ ...form, referenceCode: e.target.value })}
                  style={inputStyle}
                />
              </div>

              <div className="sm:col-span-2">
                <label style={labelStyle}>Description</label>
                <textarea
                  placeholder="Optional"
                  value={form.description}
                  onChange={(e) => setForm({ ...form, description: e.target.value })}
                  rows={3}
                  style={{ ...inputStyle, resize: "none" }}
                />
              </div>
            </div>

            <div style={modalFooter}>
              <button onClick={() => setIsAddOpen(false)} style={secondaryBtn}>
                Cancel
              </button>
              <button onClick={addExpense} disabled={loading} style={primaryBtn}>
                {loading ? "Saving..." : "Save Expense"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
