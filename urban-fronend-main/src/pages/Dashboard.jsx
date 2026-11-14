import React, { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import StatCard from "../components/StatCard";
import { PageHeader, EmptyState, Spinner } from "../components/ui";
import { api } from "../api/api";
import {
  FiBell,
  FiX,
  FiRefreshCw,
  FiAlertCircle,
  FiTrendingUp,
  FiShoppingBag,
  FiPieChart,
  FiPackage,
  FiCreditCard,
  FiBarChart2,
} from "react-icons/fi";
import { onNotification } from "../utils/notificationBus.js";
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  CartesianGrid,
} from "recharts";

function toDateInput(date) {
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
  return local.toISOString().slice(0, 10);
}

function currentMonthRange() {
  const end = new Date();
  const start = new Date(end.getFullYear(), end.getMonth(), 1);
  return { from: toDateInput(start), to: toDateInput(end) };
}

const DEFAULT_RANGE = currentMonthRange();

export default function Dashboard() {
  const navigate = useNavigate();
  const [from, setFrom] = useState(DEFAULT_RANGE.from);
  const [to, setTo] = useState(DEFAULT_RANGE.to);
  const [showNotifications, setShowNotifications] = useState(false);
  const [notifications, setNotifications] = useState([]);

  // Backend state
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  // Dashboard stats
  const [ordersCount, setOrdersCount] = useState(0);

  const [revenue, setRevenue] = useState(0);
  const [cogs, setCogs] = useState(0);
  const [grossProfit, setGrossProfit] = useState(0);

  // Expenses + Net Profit
  const [expenses, setExpenses] = useState(0);
  const [netProfit, setNetProfit] = useState(0);

  // Now backed by real API data
  const [topSelling, setTopSelling] = useState([]);
  const [recentOrders, setRecentOrders] = useState([]);

  // Real figures for the selected period (no estimated or invented data).
  const profitData = useMemo(
    () => [
      { name: "Revenue", profit: Number(revenue || 0) },
      { name: "Cost of goods", profit: Number(cogs || 0) },
      { name: "Gross profit", profit: Number(grossProfit || 0) },
      { name: "Expenses", profit: Number(expenses || 0) },
      { name: "Net profit", profit: Number(netProfit || 0) },
    ],
    [revenue, cogs, grossProfit, expenses, netProfit]
  );

  useEffect(() => {
    loadDashboard();
    // eslint-disable-next-line
  }, []);

  // Subscribe to global notifications
  useEffect(() => {
    const unsub = onNotification((n) => {
      setNotifications((prev) => [n, ...prev]);
    });
    return unsub;
  }, []);

  // When user opens the notifications dropdown, mark all as read on the backend
  useEffect(() => {
    if (!showNotifications || notifications.length === 0) return;

    (async () => {
      try {
        await api.patch("/api/v1/notifications/read-all");
        // Optionally clear unread badge by marking items as read locally
        setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
      } catch (err) {
        console.warn("Failed to mark notifications read", err);
      }
    })();
  }, [showNotifications]);

  const handleNotificationClick = (notification) => {
    // Navigate to the lead if referenceId exists
    if (notification.referenceId) {
      navigate(`/crm/leads/${notification.referenceId}`);
      setShowNotifications(false);
    }
  };

  async function loadDashboard() {
    try {
      setLoading(true);
      setError("");

      // Basic validation
      if (new Date(to) < new Date(from)) {
        setError("To date cannot be before From date.");
        return;
      }

      // 1) Orders count
      const ordersRes = await api.get("/api/v1/orders", {
        params: { page: 0, size: 1, sort: "createdAt,desc" },
      });

      const totalOrders = ordersRes.data.totalElements || 0;
      setOrdersCount(totalOrders);

      // 2) Profit Report
      const fromDate = `${from}T00:00:00`;
      const toDate = `${to}T23:59:59`;

      const reportRes = await api.get("/api/reports/profit", {
        params: { from: fromDate, to: toDate },
      });

      const rep = reportRes.data || {};

      setRevenue(Number(rep.revenue || 0));
      setCogs(Number(rep.cogs || 0));
      setGrossProfit(Number(rep.grossProfit || 0));
      setExpenses(Number(rep.expenses || 0));
      setNetProfit(Number(rep.netProfit || 0));

      // 3) Top Selling Products
      // Backend expects plain LocalDate (yyyy-MM-dd), which `from`/`to` already are.
      const topSellingRes = await api.get("/api/v1/orders/dashboard/top-selling", {
        params: { from, to, limit: 5 },
      });

      setTopSelling(
        (topSellingRes.data || []).map((p) => ({
          id: p.productId,
          name: p.productName,
          qty: Number(p.totalQuantity || 0),
        }))
      );

      // 4) Recent Orders
      // Reuses the same orders list endpoint, just page size 3, newest first.
      // NOTE: field mapping (customerName / createdAt / totalAmount) is a best guess
      // based on convention — confirm against the real order object shape and adjust
      // if the backend uses different field names.
      const recentRes = await api.get("/api/v1/orders", {
        params: { page: 0, size: 3, sort: "createdAt,desc" },
      });

      setRecentOrders(
        (recentRes.data.content || []).map((o) => ({
          id: o.id,
          customer: o.customerName || o.customer?.name || "Unknown",
          date: o.createdAt
            ? new Date(o.createdAt).toLocaleDateString("en-KE", {
                day: "2-digit",
                month: "short",
              })
            : "—",
          amount: Number(o.totalAmount || 0),
        }))
      );
    } catch (e) {
      console.log(e);
      if (e.response?.status === 403) {
        setError("You do not have permission to view all dashboard data.");
      } else {
        setError("Failed to load dashboard data.");
      }
    } finally {
      setLoading(false);
    }
  }

  const topQty = Math.max(1, ...topSelling.map((t) => t.qty));

  function money(value) {
    return `KES ${Number(value || 0).toLocaleString()}`;
  }

  return (
    <div>
      {/* Header */}
      <PageHeader title="Dashboard" subtitle={`Performance from ${fmtDate(from)} to ${fmtDate(to)}`}>
        {/* Date filter */}
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
            onClick={loadDashboard}
            disabled={loading}
            className="flex h-9 items-center gap-2 rounded-lg bg-brand-600 px-3.5 text-sm font-semibold text-white hover:bg-brand-700"
          >
            {loading ? <Spinner className="!h-4 !w-4" /> : <FiRefreshCw size={14} />}
            Apply
          </button>
        </div>

        {/* Notification Icon */}
        <div className="relative">
          <button
            onClick={() => setShowNotifications(!showNotifications)}
            aria-label="Notifications"
            className="relative grid h-[50px] w-[50px] place-items-center rounded-xl border border-slate-200 bg-white text-slate-600 shadow-sm hover:bg-slate-50"
          >
            <FiBell size={18} />
            {notifications.length > 0 && (
              <span className="absolute -right-1.5 -top-1.5 grid h-5 min-w-5 place-items-center rounded-full bg-rose-600 px-1 text-[11px] font-semibold text-white ring-2 ring-white">
                {notifications.length}
              </span>
            )}
          </button>

          {showNotifications && (
            <div className="fixed inset-x-0 bottom-0 z-50 animate-pop-in rounded-t-2xl border border-slate-200 bg-white p-2 shadow-2xl sm:absolute sm:inset-auto sm:right-0 sm:top-full sm:mt-2 sm:w-80 sm:rounded-xl">
              <div className="flex items-center justify-between px-3 py-2">
                <h4 className="m-0 text-sm font-semibold text-slate-800">Notifications</h4>
                <button
                  onClick={() => setShowNotifications(false)}
                  aria-label="Close notifications"
                  className="grid h-7 w-7 place-items-center rounded-lg text-slate-400 hover:bg-slate-100 hover:text-slate-600"
                >
                  <FiX size={16} />
                </button>
              </div>

              {notifications.length === 0 ? (
                <EmptyState title="You're all caught up" message="New notifications will appear here." icon={FiBell} />
              ) : (
                <div className="flex max-h-72 flex-col overflow-y-auto">
                  {notifications.map((n) => (
                    <div
                      key={n.id}
                      onClick={() => {
                        handleNotificationClick(n);
                        setShowNotifications(false);
                      }}
                      className="cursor-pointer rounded-lg px-3 py-2.5 hover:bg-slate-50"
                    >
                      <div className="text-sm font-medium text-slate-800">{n.title || n.message}</div>
                      <div className="mt-0.5 text-xs text-slate-500">{n.body || n.message}</div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      </PageHeader>

      {/* Error */}
      {error && (
        <div className="mb-5 flex items-center gap-2 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-medium text-rose-700">
          <FiAlertCircle /> {error}
        </div>
      )}

      {/* Stat Cards */}
      <div className="mb-6 grid grid-cols-1 gap-4 min-[420px]:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
        <StatCard title="Revenue" value={money(revenue)} tone="green" icon={FiTrendingUp} />
        <StatCard title="Orders" value={Number(ordersCount).toLocaleString()} tone="blue" icon={FiShoppingBag} />
        <StatCard title="Gross Profit" value={money(grossProfit)} tone="indigo" icon={FiPieChart} />
        <StatCard title="COGS" value={money(cogs)} tone="orange" icon={FiPackage} />
        <StatCard title="Expenses" value={money(expenses)} tone="red" icon={FiCreditCard} />
      </div>

      {/* Main grid */}
      <div className="grid grid-cols-1 items-start gap-6 lg:grid-cols-3">
        {/* LEFT SIDE */}
        <div className="flex flex-col gap-6 lg:col-span-2">
          {/* Profit summary */}
          <div className="relative overflow-hidden rounded-2xl bg-gradient-to-br from-slate-900 via-slate-900 to-brand-900 p-6 text-white shadow-lg">
            <div className="pointer-events-none absolute -right-16 -top-16 h-56 w-56 rounded-full bg-brand-500/30 blur-3xl" />
            <div className="relative flex flex-col gap-6 sm:flex-row sm:items-end sm:justify-between">
              <div>
                <div className="text-sm font-medium text-slate-300">Gross Profit</div>
                <div className="mt-1 text-4xl font-bold tracking-tight sm:text-5xl">{money(grossProfit)}</div>
              </div>
              <div className="rounded-xl bg-white/10 px-4 py-3 backdrop-blur">
                <div className="text-xs font-medium text-slate-300">Net Profit</div>
                <div
                  className={`mt-0.5 text-xl font-semibold ${
                    netProfit < 0 ? "text-rose-300" : "text-emerald-300"
                  }`}
                >
                  {money(netProfit)}
                </div>
              </div>
            </div>
          </div>

          {/* Profit Breakdown */}
          <div className="ui-card p-6">
            <div className="mb-5 flex flex-wrap items-start justify-between gap-2">
              <div>
                <h3 className="m-0 text-base font-semibold text-slate-900">Profit Breakdown</h3>
                <p className="m-0 mt-1 text-sm text-slate-500">
                  Revenue, costs and profit for the selected period.
                </p>
              </div>
            </div>

            <div className="h-[280px] sm:h-[300px]">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={profitData} margin={{ top: 4, right: 4, left: -8, bottom: 0 }}>
                  <CartesianGrid vertical={false} stroke="#eef2f7" />
                  <XAxis dataKey="name" tickLine={false} axisLine={false} tick={{ fill: "#64748b", fontSize: 12 }} />
                  <YAxis
                    tickLine={false}
                    axisLine={false}
                    tick={{ fill: "#64748b", fontSize: 12 }}
                    tickFormatter={(v) => (Math.abs(v) >= 1000 ? `${Math.round(v / 1000)}k` : v)}
                  />
                  <Tooltip
                    cursor={{ fill: "rgba(79,70,229,0.06)" }}
                    formatter={(v) => [money(v), "Amount"]}
                    contentStyle={{
                      borderRadius: 10,
                      border: "1px solid #e5e7eb",
                      boxShadow: "0 8px 24px rgba(15,23,42,0.08)",
                      fontSize: 13,
                    }}
                  />
                  <Bar dataKey="profit" name="Amount" fill="#4f46e5" radius={[6, 6, 0, 0]} maxBarSize={44} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>
        </div>

        {/* RIGHT SIDE */}
        <div className="flex flex-col gap-6 lg:col-span-1">
          {/* Top Selling Products */}
          <div className="ui-card p-6">
            <h3 className="m-0 mb-4 text-base font-semibold text-slate-900">Top Selling Products</h3>

            {topSelling.length === 0 ? (
              <EmptyState title="No sales yet" message="No sales in this period." icon={FiBarChart2} />
            ) : (
              <div className="flex flex-col gap-4">
                {topSelling.map((p, index) => (
                  <div key={p.id}>
                    <div className="mb-1.5 flex items-center justify-between gap-3 text-sm">
                      <div className="flex min-w-0 items-center gap-2.5">
                        <span className="grid h-6 w-6 shrink-0 place-items-center rounded-md bg-slate-100 text-xs font-semibold text-slate-600">
                          {index + 1}
                        </span>
                        <span className="truncate font-medium text-slate-700">{p.name}</span>
                      </div>
                      <span className="shrink-0 font-semibold text-slate-900">{p.qty}</span>
                    </div>
                    <div className="h-1.5 overflow-hidden rounded-full bg-slate-100">
                      <div
                        className="h-full rounded-full bg-brand-500"
                        style={{ width: `${(p.qty / topQty) * 100}%` }}
                      />
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Recent Orders */}
          <div className="ui-card p-6">
            <div className="mb-4 flex items-center justify-between">
              <h3 className="m-0 text-base font-semibold text-slate-900">Recent Orders</h3>
              <button
                onClick={() => navigate("/orders")}
                className="text-sm font-medium text-brand-600 hover:text-brand-700"
              >
                View all
              </button>
            </div>

            {recentOrders.length === 0 ? (
              <EmptyState title="No recent orders" icon={FiShoppingBag} />
            ) : (
              <div className="flex flex-col divide-y divide-slate-100">
                {recentOrders.map((o) => (
                  <div key={o.id} className="flex items-center gap-3 py-3 first:pt-0 last:pb-0">
                    <span className="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-brand-50 text-xs font-semibold text-brand-700">
                      {(o.customer || "?").charAt(0).toUpperCase()}
                    </span>
                    <div className="min-w-0 flex-1">
                      <div className="truncate text-sm font-medium text-slate-800">{o.customer}</div>
                      <div className="text-xs text-slate-500">{o.date}</div>
                    </div>
                    <div className="text-sm font-semibold text-slate-900">{money(o.amount)}</div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

function fmtDate(d) {
  const date = new Date(d);
  if (Number.isNaN(date.getTime())) return d;
  return date.toLocaleDateString("en-KE", { day: "numeric", month: "short", year: "numeric" });
}
