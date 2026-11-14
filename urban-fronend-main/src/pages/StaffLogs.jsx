import React, { useMemo, useState, useEffect } from "react";
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
} from "recharts";
import {
  FiActivity,
  FiPlusCircle,
  FiEdit3,
  FiTrash2,
  FiUsers,
  FiRefreshCw,
  FiChevronLeft,
  FiChevronRight,
} from "react-icons/fi";
import { api } from "../api/api";
import StatCard from "../components/StatCard";
import { defaultRange } from "../utils/dateRange";

// The backend returns at most this many (newest) logs per request.
const BACKEND_LOG_LIMIT = 500;
import { PageHeader, EmptyState, LoadingBlock } from "../components/ui";
import { pageBtnStyle, alertError } from "../components/styles";

export default function StaffLogs() {
  const [from, setFrom] = useState(() => defaultRange(30).from);
  const [to, setTo] = useState(() => defaultRange(30).to);

  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [page, setPage] = useState(1);
  const ITEMS_PER_PAGE = 20;

  const fetchLogs = async () => {
    try {
      setLoading(true);
      setError("");

      // Backend expects LocalDateTime (yyyy-MM-dd'T'HH:mm:ss),
      // but our date inputs only give yyyy-MM-dd. Convert here
      // so we don't have to touch the backend controller.
      const fromDateTime = `${from}T00:00:00`;
      const toDateTime = `${to}T23:59:59`;

      const res = await api.get(`/api/v1/staff-logs`, {
        params: { from: fromDateTime, to: toDateTime },
      });
      setLogs(res.data);
      setPage(1);
    } catch (err) {
      console.error("Failed to fetch logs", err);
      setLogs([]);
      setError(
        err.response?.status === 403
          ? "You do not have permission to view staff logs."
          : "Failed to load activity logs. Check the backend and try again."
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLogs();
  }, [from, to]);

  // 🔃 Newest first
  const sortedLogs = useMemo(() => {
    return [...logs].sort(
      (a, b) => new Date(b.timestamp) - new Date(a.timestamp)
    );
  }, [logs]);

  // 📊 Stats
  const stats = useMemo(() => {
    const total = logs.length;

    const creates = logs.filter((l) => l.action === "CREATE").length;
    const updates = logs.filter((l) => l.action === "UPDATE").length;
    const deletes = logs.filter((l) => l.action === "DELETE").length;

    return [
      { title: "Total Actions", value: total, tone: "blue" },
      { title: "Creates", value: creates, tone: "green" },
      { title: "Updates", value: updates, tone: "orange" },
      { title: "Deletes", value: deletes, tone: "red" },
      {
        title: "Active Staff",
        value: new Set(logs.map((l) => l.staffId)).size,
        tone: "greenSoft",
      },
    ];
  }, [logs]);

  // 📈 Chart data
  const chartData = useMemo(() => {
    const map = {};

    logs.forEach((log) => {
      const date = log.timestamp.split("T")[0];

      if (!map[date]) {
        map[date] = { name: date, actions: 0 };
      }

      map[date].actions++;
    });

    return Object.values(map);
  }, [logs]);

  const actionColor = (action) => {
    if (action === "CREATE") return "#16a34a";
    if (action === "UPDATE") return "#d97706";
    if (action === "DELETE") return "#dc2626";
    if (action === "CONVERT") return "#7c3aed";
    return "#475569";
  };

  // 📄 Pagination (newest logs first)
  const totalPages = Math.ceil(sortedLogs.length / ITEMS_PER_PAGE);
  const startIdx = (page - 1) * ITEMS_PER_PAGE;
  const endIdx = startIdx + ITEMS_PER_PAGE;
  const paginatedLogs = sortedLogs.slice(startIdx, endIdx);

  // Sliding window of page buttons so pages beyond the first five stay reachable.
  const pageWindow = useMemo(() => {
    const windowSize = 5;
    const start = Math.max(1, Math.min(page - 2, totalPages - windowSize + 1));
    const end = Math.min(totalPages, start + windowSize - 1);
    return Array.from({ length: Math.max(0, end - start + 1) }, (_, i) => start + i);
  }, [page, totalPages]);

  const statIcons = {
    "Total Actions": FiActivity,
    Creates: FiPlusCircle,
    Updates: FiEdit3,
    Deletes: FiTrash2,
    "Active Staff": FiUsers,
  };
  const statTones = { blue: "blue", green: "green", orange: "orange", red: "red", greenSoft: "indigo" };

  const actionBadge = (action) => ({
    display: "inline-flex",
    padding: "2px 9px",
    borderRadius: 999,
    fontSize: 11.5,
    fontWeight: 600,
    letterSpacing: "0.02em",
    background: actionColor(action) + "1a",
    color: actionColor(action),
    whiteSpace: "nowrap",
  });

  const cols = "minmax(140px,1fr) 110px minmax(100px,0.8fr) minmax(200px,2.2fr) 180px";

  return (
    <div>
      {/* Header */}
      <PageHeader title="Staff Activity Logs" subtitle="Every create, update and delete made by your team">
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
            onClick={fetchLogs}
            className="flex h-9 items-center gap-2 rounded-lg bg-brand-600 px-3.5 text-sm font-semibold text-white hover:bg-brand-700"
          >
            <FiRefreshCw size={14} /> Apply
          </button>
        </div>
      </PageHeader>

      {logs.length >= BACKEND_LOG_LIMIT && (
        <div style={{ ...alertError, marginBottom: 16 }}>
          Showing only the newest {BACKEND_LOG_LIMIT} actions in this range. Narrow the dates
          to see older activity; the totals below cover these {BACKEND_LOG_LIMIT} only.
        </div>
      )}

      {/* Stats */}
      <div className="mb-6 grid grid-cols-1 gap-4 min-[420px]:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
        {stats.map((s) => (
          <StatCard
            key={s.title}
            title={s.title}
            value={Number(s.value).toLocaleString()}
            tone={statTones[s.tone] || "slate"}
            icon={statIcons[s.title]}
          />
        ))}
      </div>

      {/* Chart */}
      <div className="ui-card mb-6 p-6">
        <h3 className="m-0 mb-4 text-base font-semibold text-slate-900">Actions per day</h3>
        <div className="h-64">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={chartData} margin={{ top: 4, right: 4, left: -16, bottom: 0 }}>
              <CartesianGrid vertical={false} stroke="#eef2f7" />
              <XAxis dataKey="name" tickLine={false} axisLine={false} tick={{ fill: "#64748b", fontSize: 12 }} />
              <YAxis allowDecimals={false} tickLine={false} axisLine={false} tick={{ fill: "#64748b", fontSize: 12 }} />
              <Tooltip
                cursor={{ fill: "rgba(79,70,229,0.06)" }}
                contentStyle={{
                  borderRadius: 10,
                  border: "1px solid #e5e7eb",
                  boxShadow: "0 8px 24px rgba(15,23,42,0.08)",
                  fontSize: 13,
                }}
              />
              <Bar dataKey="actions" fill="#4f46e5" radius={[6, 6, 0, 0]} maxBarSize={40} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Activity Table */}
      <div className="ui-card overflow-hidden">
        {loading ? (
          <LoadingBlock label="Loading activity..." />
        ) : error ? (
          <div style={{ ...alertError, margin: 16 }}>{error}</div>
        ) : sortedLogs.length === 0 ? (
          <EmptyState icon={FiActivity} title="No activity found" message="Try widening the date range." />
        ) : (
          <>
            <div className="hidden overflow-x-auto sm:block">
              <div className="min-w-[760px]">
                <div
                  className="desktop-header grid gap-4 bg-slate-50 px-4 py-3"
                  style={{ gridTemplateColumns: cols }}
                >
                  <div>Staff</div>
                  <div>Action</div>
                  <div>Entity</div>
                  <div>Description</div>
                  <div className="text-right">Timestamp</div>
                </div>
                {paginatedLogs.map((log, i) => (
                  <div
                    key={i}
                    className="ui-row grid items-center gap-4 border-b border-slate-100 px-4 py-3 text-sm last:border-b-0"
                    style={{ gridTemplateColumns: cols }}
                  >
                    <div className="truncate font-medium text-slate-800">{log.staffName || "Unknown"}</div>
                    <div>
                      <span style={actionBadge(log.action)}>{log.action}</span>
                    </div>
                    <div className="truncate text-slate-600">{log.entity}</div>
                    <div className="truncate text-slate-500" title={log.description}>
                      {log.description}
                    </div>
                    <div className="whitespace-nowrap text-right text-slate-500">
                      {new Date(log.timestamp).toLocaleString()}
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Mobile view */}
            <div className="divide-y divide-slate-100 sm:hidden">
              {paginatedLogs.map((log, i) => (
                <div key={i} className="p-4">
                  <div className="flex items-start justify-between gap-2">
                    <span className="font-medium text-slate-800">{log.staffName || "Unknown"}</span>
                    <span style={actionBadge(log.action)}>{log.action}</span>
                  </div>
                  <div className="mt-1 text-sm text-slate-600">
                    <span className="font-medium text-slate-700">{log.entity}</span>
                    {log.description ? ` · ${log.description}` : ""}
                  </div>
                  <div className="mt-1 text-xs text-slate-400">{new Date(log.timestamp).toLocaleString()}</div>
                </div>
              ))}
            </div>

            {/* Pagination */}
            <div className="flex flex-col gap-3 border-t border-slate-100 px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
              <div className="text-sm text-slate-500">
                Showing <span className="font-medium text-slate-700">{startIdx + 1}</span>–
                <span className="font-medium text-slate-700">{Math.min(endIdx, sortedLogs.length)}</span> of{" "}
                <span className="font-medium text-slate-700">{sortedLogs.length}</span>
              </div>
              <div className="flex items-center justify-between gap-1.5 sm:justify-end">
                <button
                  onClick={() => setPage(Math.max(1, page - 1))}
                  disabled={page === 1}
                  style={pageBtnStyle}
                >
                  <FiChevronLeft size={14} /> Prev
                </button>

                <div className="hidden items-center gap-1 sm:flex">
                  {pageWindow[0] > 1 && (
                    <span className="px-1 text-xs text-slate-400">…</span>
                  )}
                  {pageWindow.map((pageNum) => (
                    <button
                      key={pageNum}
                      onClick={() => setPage(pageNum)}
                      className={`h-8 w-8 rounded-lg text-sm font-medium ${
                        page === pageNum
                          ? "bg-brand-600 text-white"
                          : "border border-slate-200 bg-white text-slate-600 hover:bg-slate-50"
                      }`}
                    >
                      {pageNum}
                    </button>
                  ))}
                  {pageWindow[pageWindow.length - 1] < totalPages && (
                    <span className="px-1 text-xs text-slate-400">…</span>
                  )}
                </div>

                <div className="px-2 text-sm text-slate-600 sm:hidden">
                  {page} / {totalPages}
                </div>

                <button
                  onClick={() => setPage(Math.min(totalPages, page + 1))}
                  disabled={page === totalPages}
                  style={pageBtnStyle}
                >
                  Next <FiChevronRight size={14} />
                </button>
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
