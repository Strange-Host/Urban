import React, { useEffect, useMemo, useState } from "react";
import { FiCreditCard, FiChevronLeft, FiChevronRight } from "react-icons/fi";
import { api } from "../api/api"; // adjust path if needed
import { PageHeader, SearchInput, EmptyState, LoadingBlock } from "../components/ui";
import { tableCard, alertError, pageBtnStyle } from "../components/styles";

export default function Payments() {
  const [activeTab, setActiveTab] = useState("All Payments");
  const [search, setSearch] = useState("");

  // Backend state
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [pageData, setPageData] = useState(null);

  // Pageable
  const [page, setPage] = useState(0);
  const size = 10;

  const tabs = ["All Payments", "M-Pesa", "Cash", "Bank"];
  const tabMethod = { "M-Pesa": "MPESA", Cash: "CASH", Bank: "BANK" }[activeTab];

  useEffect(() => {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => {
      loadPayments(page, tabMethod, search.trim(), controller.signal);
    }, search.trim() ? 300 : 0);

    return () => {
      clearTimeout(timeoutId);
      controller.abort();
    };
    // eslint-disable-next-line
  }, [page, activeTab, search]);

  async function loadPayments(requestPage = page, method, query = "", signal) {
    try {
      setLoading(true);
      setError("");

      const res = await api.get("/api/v1/payments", {
        params: {
          page: requestPage,
          size,
          sort: "paymentDate,desc",
          method: method || undefined,
          query: query || undefined,
        },
        signal,
      });

      if (signal?.aborted) return;
      setPageData(res.data);
    } catch (e) {
      if (signal?.aborted || e.code === "ERR_CANCELED") return;
      console.log(e);
      setError("Failed to load payments. Check backend, token, and CORS.");
    } finally {
      if (!signal?.aborted) setLoading(false);
    }
  }

  // Backend payments
  const payments = pageData?.content || [];

  // Convert backend PaymentResponse -> UI row format (UI expects strings)
  const normalized = useMemo(() => {
    return payments.map((p) => {
      return {
        id: p.transactionRef || String(p.id).slice(0, 8) + "...",
        customer: p.payerName || p.payerPhone || "-",
        method: mapMethod(p.method),
        amount: Number(p.amount || 0),
        status: mapStatus(p.status),
        date: formatDate(p.paymentDate || p.createdAt),
      };
    });
    // eslint-disable-next-line
  }, [payments]);

  function money(value) {
    return `KES ${value.toLocaleString()}`;
  }

  function statusStyle(status) {
    if (status === "Paid") return { color: "#166534", bg: "#dcfce7" };
    if (status === "Pending") return { color: "#92400e", bg: "#fef3c7" };
    if (status === "Refunded") return { color: "#1e40af", bg: "#dbeafe" };
    if (status === "Failed") return { color: "#991b1b", bg: "#fee2e2" };
    return { color: "#374151", bg: "#f3f4f6" };
  }

  function methodBadge(method) {
    const map = {
      "M-Pesa": { bg: "#ecfdf5", color: "#166534" },
      Cash: { bg: "#fff7ed", color: "#9a3412" },
      Bank: { bg: "#f3f4f6", color: "#374151" },
    };
    return map[method] || { bg: "#f3f4f6", color: "#374151" };
  }

  // ---- MAPPERS ----
  function mapMethod(method) {
    if (!method) return "Cash";
    if (method === "MPESA") return "M-Pesa";
    if (method === "CASH") return "Cash";
    if (method === "BANK") return "Bank";
    return String(method);
  }

  function mapStatus(status) {
    if (!status) return "Pending";

    if (status === "SUCCESS" || status === "PAID") return "Paid";

    if (status === "PENDING") return "Pending";

    if (status === "REFUNDED") return "Refunded";

    if (status === "FAILED") return "Failed";

    return "Pending";
  }

  function formatDate(iso) {
    if (!iso) return "-";
    const d = new Date(iso);
    return d.toLocaleDateString();
  }

  const badge = (c) => ({
    background: c.bg,
    color: c.color,
    padding: "3px 10px",
    borderRadius: 999,
    fontWeight: 600,
    fontSize: 12,
    display: "inline-block",
    whiteSpace: "nowrap",
  });

  const cols = "1.1fr 1.6fr 1fr 1.1fr 1fr 1fr";

  return (
    <div>
      {/* Header */}
      <PageHeader title="Payments" subtitle="Track incoming payments across all channels" />

      {/* Tabs + Search */}
      <div className="mb-4 flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
        <div className="flex w-full gap-1 overflow-x-auto rounded-xl bg-[#eef0f5] p-1 md:w-auto">
          {tabs.map((tab) => {
            const active = activeTab === tab;
            return (
              <button
                key={tab}
                onClick={() => {
                  setPage(0);
                  setActiveTab(tab);
                }}
                className={`whitespace-nowrap rounded-[9px] px-3.5 py-2 text-sm font-semibold ${
                  active ? "bg-white text-slate-900 shadow-sm" : "text-slate-500 hover:text-slate-700"
                }`}
              >
                {tab}
              </button>
            );
          })}
        </div>

        <SearchInput
          value={search}
          onChange={(e) => {
            setPage(0);
            setSearch(e.target.value);
          }}
          placeholder="Search by transaction ID, customer, phone..."
          className="w-full md:w-80"
        />
      </div>

      {/* Payments */}
      <div style={tableCard}>
        {/* Desktop table header */}
        <div
          className="desktop-header hidden md:grid"
          style={{
            gridTemplateColumns: cols,
            gap: 10,
            padding: "12px 16px",
            background: "var(--surface-muted)",
          }}
        >
          <div>Transaction ID</div>
          <div>Customer</div>
          <div>Method</div>
          <div>Amount</div>
          <div>Status</div>
          <div>Date</div>
        </div>

        {/* Loading / Error */}
        {loading && <LoadingBlock label="Loading payments..." />}
        {error && <div style={{ ...alertError, margin: 16 }}>{error}</div>}

        {/* Rows */}
        {!loading &&
          !error &&
          normalized.map((p) => {
            const s = statusStyle(p.status);
            const m = methodBadge(p.method);

            return (
              <React.Fragment key={p.id}>
                {/* Desktop row */}
                <div
                  className="ui-row hidden items-center border-b border-slate-100 text-sm md:grid"
                  style={{ gridTemplateColumns: cols, gap: 10, padding: "13px 16px" }}
                >
                  <div className="truncate font-mono text-[13px] font-medium text-slate-700">{p.id}</div>
                  <div className="truncate font-medium text-slate-800">{p.customer}</div>
                  <div>
                    <span style={badge(m)}>{p.method}</span>
                  </div>
                  <div className="font-semibold text-slate-900">{money(p.amount)}</div>
                  <div>
                    <span style={badge(s)}>{p.status}</span>
                  </div>
                  <div className="text-slate-500">{p.date}</div>
                </div>

                {/* Mobile card */}
                <div className="border-b border-slate-100 p-4 md:hidden">
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <div className="truncate font-medium text-slate-900">{p.customer}</div>
                      <div className="mt-0.5 truncate font-mono text-xs text-slate-500">{p.id}</div>
                    </div>
                    <div className="shrink-0 text-right">
                      <div className="font-semibold text-slate-900">{money(p.amount)}</div>
                      <div className="mt-0.5 text-xs text-slate-500">{p.date}</div>
                    </div>
                  </div>
                  <div className="mt-3 flex flex-wrap gap-2">
                    <span style={badge(m)}>{p.method}</span>
                    <span style={badge(s)}>{p.status}</span>
                  </div>
                </div>
              </React.Fragment>
            );
          })}

        {/* Empty */}
        {!loading && !error && pageData && normalized.length === 0 && (
          <EmptyState
            icon={FiCreditCard}
            title="No payments found"
            message={payments.length ? "Try a different tab or search term." : "Payments will show up here once received."}
          />
        )}

        {/* Footer (pageable) */}
        {pageData && (
          <div className="flex flex-wrap items-center justify-between gap-3 bg-white px-4 py-3 text-sm text-slate-500">
            <div>
              Showing <span className="font-medium text-slate-700">{normalized.length}</span> of{" "}
              <span className="font-medium text-slate-700">{pageData.totalElements}</span> payments
            </div>

            <div className="flex items-center gap-2">
              <button
                style={pageBtnStyle}
                disabled={page === 0}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
              >
                <FiChevronLeft size={14} /> Prev
              </button>
              <span className="px-1 font-medium text-slate-700">
                {page + 1} / {pageData.totalPages || 1}
              </span>
              <button style={pageBtnStyle} disabled={pageData.last} onClick={() => setPage((p) => p + 1)}>
                Next <FiChevronRight size={14} />
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
