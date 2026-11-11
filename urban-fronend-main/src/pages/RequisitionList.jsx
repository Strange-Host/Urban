import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  getRequisitions,
  approveRequisition,
  rejectRequisition,
  markPaidRequisition,
} from "../api/requisitionApi";
import { FiPlus, FiCheck, FiX, FiDollarSign, FiClipboard, FiChevronLeft, FiChevronRight } from "react-icons/fi";
import { PageHeader, EmptyState, LoadingBlock } from "../components/ui";
import { primaryBtn, pageBtnStyle, tableCard, alertError } from "../components/styles";

export default function RequisitionList() {
  const navigate = useNavigate();
  const [requisitions, setRequisitions] = useState([]);
  const [page, setPage] = useState(0);
  const [size] = useState(10);
  const [pageData, setPageData] = useState(null);
  const [statusFilter, setStatusFilter] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [isMobile, setIsMobile] = useState(false);

  useEffect(() => {
    const handleResize = () => setIsMobile(window.innerWidth <= 900);
    handleResize();
    window.addEventListener("resize", handleResize);
    return () => window.removeEventListener("resize", handleResize);
  }, []);

  useEffect(() => {
    loadRequisitions();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, statusFilter]);

  async function loadRequisitions() {
    try {
      setLoading(true);
      setError("");
      const res = await getRequisitions({
        page,
        size,
        sort: "createdAt,desc",
        status: statusFilter || undefined,
      });
      setPageData(res);
      setRequisitions(res.content || []);
    } catch (e) {
      console.error(e);
      setError("Failed to load requisitions. Check backend and token.");
    } finally {
      setLoading(false);
    }
  }

  async function handleApprove(id) {
    if (!window.confirm("Approve this requisition?")) return;
    try {
      setLoading(true);
      await approveRequisition(id);
      await loadRequisitions();
      alert("Requisition approved.");
    } catch (e) {
      console.error(e);
      alert("Failed to approve requisition.");
    } finally {
      setLoading(false);
    }
  }

  async function handleReject(id) {
    const reason = window.prompt("Enter rejection reason:");
    if (!reason) return;
    try {
      setLoading(true);
      await rejectRequisition(id, reason);
      await loadRequisitions();
      alert("Requisition rejected.");
    } catch (e) {
      console.error(e);
      alert("Failed to reject requisition.");
    } finally {
      setLoading(false);
    }
  }

  async function handleMarkPaid(id) {
    if (!window.confirm("Mark this requisition as paid?")) return;
    try {
      setLoading(true);
      await markPaidRequisition(id);
      await loadRequisitions();
      alert("Requisition marked as paid.");
    } catch (e) {
      console.error(e);
      alert("Failed to mark requisition as paid.");
    } finally {
      setLoading(false);
    }
  }

  function formatMoney(value) {
    return `KES ${Number(value || 0).toLocaleString()}`;
  }

  function niceStatus(status) {
    return String(status || "")
      .replaceAll("_", " ")
      .toLowerCase()
      .replace(/\b\w/g, (c) => c.toUpperCase());
  }

  function getStatusBadgeStyle(status) {
    const normalized = String(status || "").toUpperCase();
    if (normalized === "REJECTED") {
      return { background: "#fee2e2", color: "#991b1b" };
    }
    if (normalized === "PAID") {
      return { background: "#dcfce7", color: "#166534" };
    }
    if (normalized === "APPROVED") {
      return { background: "#dbeafe", color: "#1d4ed8" };
    }
    return { background: "#fef3c7", color: "#92400e" };
  }

  const who = (u) => u?.name || u?.email || "—";
  const day = (d) => (d ? new Date(d).toLocaleDateString("en-KE", { day: "numeric", month: "short", year: "numeric" }) : "—");
  const typeLabel = (t) => niceStatus(t) || "—";

  function renderActions(req) {
    if (req.status === "PENDING") {
      return (
        <>
          <button onClick={() => handleApprove(req.id)} style={approveBtn}>
            <FiCheck size={13} /> Approve
          </button>
          <button onClick={() => handleReject(req.id)} style={rejectBtn}>
            <FiX size={13} /> Reject
          </button>
        </>
      );
    }
    if (req.status === "APPROVED") {
      return (
        <button onClick={() => handleMarkPaid(req.id)} style={paidBtn}>
          <FiDollarSign size={13} /> Mark Paid
        </button>
      );
    }
    return <span className="text-xs text-slate-400">No actions</span>;
  }

  return (
    <div>
      <PageHeader title="Requisitions" subtitle="Approve, reject and settle requisitions.">
        <button onClick={() => navigate("/requisitions/new")} style={primaryBtn}>
          <FiPlus size={16} /> New Requisition
        </button>
      </PageHeader>

      {/* Status filter */}
      <div className="mb-4 flex flex-wrap items-center gap-2">
        {[
          ["", "All"],
          ["PENDING", "Pending"],
          ["APPROVED", "Approved"],
          ["REJECTED", "Rejected"],
          ["PAID", "Paid"],
        ].map(([value, label]) => {
          const active = statusFilter === value;
          return (
            <button
              key={value || "all"}
              onClick={() => {
                setPage(0);
                setStatusFilter(value);
              }}
              className={`rounded-full border px-3.5 py-1.5 text-sm font-medium ${
                active
                  ? "border-brand-600 bg-brand-600 text-white"
                  : "border-slate-200 bg-white text-slate-600 hover:border-slate-300 hover:text-slate-800"
              }`}
            >
              {label}
            </button>
          );
        })}
      </div>

      <div style={tableCard}>
        {loading && <LoadingBlock label="Loading requisitions..." />}
        {error && !loading && <div style={{ ...alertError, margin: 16 }}>{error}</div>}

        {!loading && !error && requisitions.length === 0 && (
          <EmptyState
            icon={FiClipboard}
            title="No requisitions found"
            message={statusFilter ? "Try a different status filter." : "New requisitions will appear here."}
          />
        )}

        {!loading && requisitions.length > 0 && !isMobile && (
          <div className="overflow-x-auto">
            <table className="ui-table">
              <thead>
                <tr>
                  <th>Title</th>
                  <th>Type</th>
                  <th>Requested By</th>
                  <th>Amount</th>
                  <th>Status</th>
                  <th>Approved By</th>
                  <th>Paid By</th>
                  <th>Paid At</th>
                  <th>Requested</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {requisitions.map((req) => (
                  <tr key={req.id}>
                    <td className="font-medium text-slate-900">{req.title}</td>
                    <td className="whitespace-nowrap text-slate-600">{typeLabel(req.requisitionType)}</td>
                    <td className="whitespace-nowrap">{who(req.requestedBy)}</td>
                    <td className="whitespace-nowrap font-semibold">{formatMoney(req.amountRequested)}</td>
                    <td>
                      <span style={{ ...statusTagStyle, ...getStatusBadgeStyle(req.status) }}>{niceStatus(req.status)}</span>
                    </td>
                    <td className="whitespace-nowrap text-slate-600">{who(req.approvedBy)}</td>
                    <td className="whitespace-nowrap text-slate-600">{who(req.paidBy)}</td>
                    <td className="whitespace-nowrap text-slate-500">{day(req.paidAt)}</td>
                    <td className="whitespace-nowrap text-slate-500">{day(req.createdAt)}</td>
                    <td>
                      <div className="flex flex-nowrap gap-2">
                        {renderActions(req)}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {!loading &&
          isMobile &&
          requisitions.map((req) => (
            <div key={req.id} className="border-b border-slate-100 p-4">
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0">
                  <div className="font-semibold text-slate-900">{req.title}</div>
                  <div className="mt-0.5 text-xs text-slate-500">{typeLabel(req.requisitionType)}</div>
                </div>
                <span style={{ ...statusTagStyle, ...getStatusBadgeStyle(req.status) }}>{niceStatus(req.status)}</span>
              </div>

              <div className="mt-3 text-xl font-bold tracking-tight text-slate-900">
                {formatMoney(req.amountRequested)}
              </div>

              <dl className="mt-3 grid grid-cols-2 gap-x-4 gap-y-2 text-sm">
                {[
                  ["Requested by", who(req.requestedBy)],
                  ["Requested", day(req.createdAt)],
                  ["Approved by", who(req.approvedBy)],
                  ["Paid by", who(req.paidBy)],
                  ["Paid at", day(req.paidAt)],
                ].map(([k, v]) => (
                  <div key={k} className="min-w-0">
                    <dt className="text-xs text-slate-500">{k}</dt>
                    <dd className="m-0 truncate text-slate-800">{v}</dd>
                  </div>
                ))}
              </dl>

              <div className="mt-4 flex flex-wrap gap-2 [&>button]:flex-1">
                {renderActions(req)}
              </div>
            </div>
          ))}

        {pageData && (
          <div className="flex flex-wrap items-center justify-between gap-3 px-4 py-3 text-sm text-slate-500">
            <div>
              Showing <span className="font-medium text-slate-700">{requisitions.length}</span> of{" "}
              <span className="font-medium text-slate-700">{pageData.totalElements}</span>
            </div>
            <div className="flex items-center gap-2">
              <button disabled={page === 0} onClick={() => setPage((p) => Math.max(0, p - 1))} style={pageBtnStyle}>
                <FiChevronLeft size={14} /> Prev
              </button>
              <span className="px-1 font-medium text-slate-700">
                {page + 1} / {pageData.totalPages || 1}
              </span>
              <button disabled={pageData.last} onClick={() => setPage((p) => p + 1)} style={pageBtnStyle}>
                Next <FiChevronRight size={14} />
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

const statusTagStyle = {
  display: "inline-flex",
  padding: "3px 10px",
  borderRadius: 999,
  fontWeight: 600,
  fontSize: 12,
  whiteSpace: "nowrap",
};

const smallBtn = {
  display: "inline-flex",
  alignItems: "center",
  justifyContent: "center",
  gap: 5,
  borderRadius: 8,
  padding: "6px 10px",
  fontWeight: 600,
  fontSize: 12.5,
  whiteSpace: "nowrap",
};

const approveBtn = { ...smallBtn, border: "1px solid #bbf7d0", background: "#f0fdf4", color: "#15803d" };
const rejectBtn = { ...smallBtn, border: "1px solid #fecaca", background: "#fef2f2", color: "#b91c1c" };
const paidBtn = { ...smallBtn, border: "1px solid transparent", background: "var(--brand)", color: "#fff" };
