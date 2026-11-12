import React, { useEffect, useRef, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import {
  LineChart, Line, XAxis, YAxis, Tooltip,
  ResponsiveContainer, CartesianGrid, Legend,
} from "recharts";
import { api } from "../api/api";
import { useAuth } from "../auth/useAuth";
import { FiArrowLeft, FiPrinter, FiCalendar, FiMail, FiUserX } from "react-icons/fi";
import { LoadingBlock, EmptyState } from "../components/ui";

export default function StaffDetailsPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user, isAdmin, isManager } = useAuth();

  const [staff, setStaff] = useState(null);
  const [summary, setSummary] = useState(null);
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);

  const canViewOthers = isAdmin() || isManager();
  const targetId = canViewOthers ? id : user?.id;

  // Today's date range auto-computed
  const todayStart = new Date();
  todayStart.setHours(0, 0, 0, 0);
  const todayEnd = new Date();
  todayEnd.setHours(23, 59, 59, 999);

  useEffect(() => {
    async function loadData() {
      try {
        setLoading(true);

        const [staffRes, summaryRes, ordersRes] = await Promise.all([
          api.get(`/api/v1/auth/user/${targetId}`),
          api.get(`/api/v1/orders/staff/${targetId}/summary`),
          api.get(`/api/v1/orders`, {
            params: {
              page: 0,
              size: 10,
              sort: "createdAt,desc",
              staffId: targetId,
            }
          }),
        ]);

        setStaff(staffRes.data);
        setSummary(summaryRes.data);
        setOrders(ordersRes.data.content || []);
      } catch (err) {
        console.error("Error loading staff details", err);
      } finally {
        setLoading(false);
      }
    }

    if (targetId) loadData();
  }, [targetId]);

  if (loading) return <LoadingBlock label="Loading staff details..." />;

  if (!staff) return (
    <div className="ui-card">
      <EmptyState icon={FiUserX} title="Staff not found" message="This staff member may have been removed." />
    </div>
  );

  const fullName = `${staff.firstName || ""} ${staff.lastName || ""}`.trim();
  const initials = fullName.split(" ").map(n => n[0]).join("").slice(0, 2).toUpperCase();
  const role = staff.role || "Staff";
  const joinedDate = staff.createdAt ? new Date(staff.createdAt).toLocaleDateString() : "N/A";
  const assignableRoles = ["STAFF", "CASHIER"];
  const canManageAssignments = isAdmin() && assignableRoles.includes(String(role).toUpperCase());

  const chartData = [
    { date: "Total", revenue: summary?.totalRevenue || 0, profit: summary?.totalProfit || 0 },
    { date: "Today",  revenue: summary?.revenueToday  || 0, profit: summary?.profitToday  || 0 },
  ];

  return (
    <div>
      <div>

        {/* TOP BAR */}
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 mb-6">
          <div>
            <p className="m-0 mb-1 text-sm font-medium text-slate-400">
              {canViewOthers ? "Staff" : "My Dashboard"}
            </p>
            <h1 className="m-0 text-2xl font-bold tracking-tight text-slate-900">{fullName}</h1>
          </div>
          <div className="flex gap-3 flex-wrap">
            {canViewOthers && (
              <button
                onClick={() => navigate("/staff")}
                className="inline-flex items-center gap-2 rounded-[10px] border border-slate-200 bg-white px-4 py-2.5 text-sm font-semibold text-slate-700 hover:bg-slate-50"
              >
                <FiArrowLeft size={15} /> Back to staff
              </button>
            )}
            <button
              onClick={() => window.print()}
              className="inline-flex items-center gap-2 rounded-[10px] bg-brand-600 px-4 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-brand-700"
            >
              <FiPrinter size={15} /> Print report
            </button>
          </div>
        </div>

        {/* PROFILE CARD */}
        <div className="ui-card p-6 mb-6">
          <div className="flex flex-col md:flex-row md:items-center gap-6">
            <div className="flex items-center gap-4">
              <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-brand-500 to-brand-700 flex items-center justify-center text-white text-xl font-bold flex-shrink-0 shadow-lg shadow-brand-600/20">
                {initials}
              </div>
              <div>
                <h3 className="m-0 text-lg font-semibold text-slate-900">{fullName}</h3>
                <div className="mt-1.5 flex flex-wrap items-center gap-2">
                  <span className="rounded-full bg-brand-50 px-2.5 py-0.5 text-xs font-semibold text-brand-700">
                    {String(role).charAt(0) + String(role).slice(1).toLowerCase()}
                  </span>
                  <span className="rounded-full bg-emerald-50 px-2.5 py-0.5 text-xs font-semibold text-emerald-700">Active</span>
                </div>
                <div className="mt-2 flex flex-wrap gap-x-4 gap-y-1 text-xs text-slate-500">
                  <span className="inline-flex items-center gap-1.5"><FiMail size={12} /> {staff.email}</span>
                  <span className="inline-flex items-center gap-1.5"><FiCalendar size={12} /> Joined {joinedDate}</span>
                </div>
              </div>
            </div>

            {/* TODAY BADGE */}
            <div className="md:ml-auto">
              <div className="inline-flex items-center gap-2 bg-brand-50 border border-brand-100 px-4 py-2 rounded-xl">
                <div className="w-2 h-2 rounded-full bg-brand-500 animate-pulse"></div>
                <span className="text-brand-700 text-sm font-medium">
                  Today — {new Date().toLocaleDateString("en-KE", { weekday: "long", day: "numeric", month: "long" })}
                </span>
              </div>
            </div>
          </div>
        </div>

        {/* PRODUCTS HANDLED */}
        {canManageAssignments && (
          <ProductsHandledCard targetId={targetId} staffName={fullName} />
        )}

        {/* TODAY STATS ROW */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
          <TodayStat
            label="Orders today"
            value={summary?.ordersToday ?? 0}
            raw
          />
          <TodayStat
            label="Revenue today"
            value={summary?.revenueToday || 0}
            money
            color="blue"
          />
          <TodayStat
            label="Profit today"
            value={summary?.profitToday || 0}
            money
            color="green"
          />
          <TodayStat
            label="Commission today"
            value={summary?.commissionToday || 0}
            money
            color="purple"
          />
        </div>

        {/* ALL TIME STATS ROW */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
          <AllTimeStat
            label="Total orders"
            value={summary?.totalOrders ?? 0}
            raw
          />
          <AllTimeStat
            label="Total revenue"
            value={summary?.totalRevenue || 0}
            money
            color="blue"
          />
          <AllTimeStat
            label="Total commission"
            value={summary?.totalCommission || 0}
            money
            color="purple"
          />
          <AllTimeStat
            label="Avg. order value"
            value={summary?.averageOrderValue || 0}
            money
            color="amber"
          />
        </div>

        {/* CHART */}
        <div className="ui-card p-6 mb-6">
          <h3 className="m-0 mb-4 text-base font-semibold text-slate-900">Revenue vs profit</h3>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={chartData}>
                <CartesianGrid vertical={false} stroke="#eef2f7" />
                <XAxis dataKey="date" tick={{ fontSize: 12 }} />
                <YAxis tick={{ fontSize: 12 }} tickFormatter={v => `${(v/1000).toFixed(0)}k`} />
                <Tooltip formatter={val => `Ksh ${Number(val).toLocaleString()}`} />
                <Legend />
                <Line type="monotone" dataKey="revenue" name="Revenue" stroke="#4f46e5" strokeWidth={2.5} dot={{ r: 4 }} />
                <Line type="monotone" dataKey="profit"  name="Profit"  stroke="#16a34a" strokeWidth={2.5} dot={{ r: 4 }} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* RECENT ORDERS */}
        <div className="ui-card p-6 mb-6">
          <div className="flex items-center justify-between mb-4">
            <h3 className="m-0 text-base font-semibold text-slate-900">Recent orders</h3>
            <span className="text-xs text-slate-400">{orders.length} shown</span>
          </div>

          {orders.length === 0 ? (
            <div className="text-center py-8 text-slate-400 text-sm">
              No orders found for this staff member.
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-full text-sm">
                <thead>
                  <tr className="border-b border-slate-100">
                    <th className="pb-3 text-left text-[11.5px] uppercase tracking-wider text-slate-500 font-semibold">Customer</th>
                    <th className="pb-3 text-left text-[11.5px] uppercase tracking-wider text-slate-500 font-semibold">Product</th>
                    <th className="pb-3 text-left text-[11.5px] uppercase tracking-wider text-slate-500 font-semibold">Amount</th>
                    <th className="pb-3 text-left text-[11.5px] uppercase tracking-wider text-slate-500 font-semibold">Status</th>
                    <th className="pb-3 text-left text-[11.5px] uppercase tracking-wider text-slate-500 font-semibold">Date</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-50">
                  {orders.map((o) => (
                    <tr key={o.id} className="hover:bg-slate-50 transition-colors">
                      <td className="py-3 font-medium text-slate-800">{o.customerName || "-"}</td>
                      <td className="py-3 text-slate-500">{o.productName || "-"}</td>
                      <td className="py-3 font-semibold text-slate-800">
                        Ksh {Number(o.totalAmount || 0).toLocaleString()}
                      </td>
                      <td className="py-3">
                        <StatusBadge status={o.orderStatus} />
                      </td>
                      <td className="py-3 text-slate-400 text-xs">
                        {o.createdAt ? String(o.createdAt).slice(0, 10) : "-"}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* PAYROLL SUMMARY */}
        <div className="ui-card p-6">
          <h3 className="m-0 mb-4 text-base font-semibold text-slate-900">Payroll summary</h3>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4">
            <div className="bg-slate-50 rounded-xl p-4">
              <p className="text-xs text-slate-400 mb-1">Total commission earned</p>
              <p className="text-lg font-bold text-purple-600">
                Ksh {(summary?.totalCommission || 0).toLocaleString()}
              </p>
            </div>
            <div className="bg-slate-50 rounded-xl p-4">
              <p className="text-xs text-slate-400 mb-1">Total orders closed</p>
              <p className="text-lg font-bold text-slate-800">
                {summary?.totalOrders ?? 0}
              </p>
            </div>
            <div className="bg-slate-50 rounded-xl p-4">
              <p className="text-xs text-slate-400 mb-1">Avg. order value</p>
              <p className="text-lg font-bold text-slate-800">
                Ksh {(summary?.averageOrderValue || 0).toLocaleString()}
              </p>
            </div>
          </div>
          <div className="bg-green-50 border border-green-100 rounded-xl p-4 flex items-center justify-between">
            <div>
              <p className="text-green-700 text-sm font-medium">Total payable</p>
              <p className="text-xs text-green-500 mt-0.5">Based on commission earned</p>
            </div>
            <p className="text-2xl font-bold text-green-700">
              Ksh {(summary?.totalCommission || 0).toLocaleString()}
            </p>
          </div>
        </div>

      </div>
    </div>
  );
}

// ── Products handled card ────────────────────────────────────
function normalizeId(value) {
  return value == null ? null : String(value);
}

function normalizeAssignments(rawAssignments) {
  const list = Array.isArray(rawAssignments)
    ? rawAssignments
    : Array.isArray(rawAssignments?.content)
      ? rawAssignments.content
      : Array.isArray(rawAssignments?.data)
        ? rawAssignments.data
        : Array.isArray(rawAssignments?.items)
          ? rawAssignments.items
          : Array.isArray(rawAssignments?.assignments)
            ? rawAssignments.assignments
            : [];

  return list.map((assignment) => {
    const product = assignment?.product ?? {};
    const staff = assignment?.staff ?? {};
    const productId = assignment?.productId ?? product?.id ?? assignment?.id ?? product?.productId;
    const staffId = assignment?.staffId ?? staff?.id ?? assignment?.userId ?? assignment?.assignedStaffId ?? assignment?.assignedTo?.id;
    const staffName = assignment?.staffName ?? [staff?.firstName, staff?.lastName].filter(Boolean).join(" ") ?? staff?.name ?? "";

    return {
      productId: productId != null ? normalizeId(productId) : null,
      staffId: staffId != null ? normalizeId(staffId) : null,
      staffName,
    };
  });
}

function ProductsHandledCard({ targetId, staffName }) {
  const [products, setProducts] = useState([]);
  const [assignments, setAssignments] = useState([]); // [{ productId, staffId, staffName }]
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [pendingIds, setPendingIds] = useState(new Set());
  const [selectedProductIds, setSelectedProductIds] = useState(new Set());
  const selectedProductIdsRef = useRef(new Set());
  const [hasChanges, setHasChanges] = useState(false);
  const [saving, setSaving] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");

  // Pagination state
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const pageSize = 30;

  // Assignments are loaded once (needed in full for diffing on save)
  useEffect(() => {
    let cancelled = false;

    async function loadAssignments() {
      try {
        const assignmentsRes = await api.get(`/api/v1/products/staff-assignments`);
        if (cancelled) return;

        const normalizedAssignments = normalizeAssignments(assignmentsRes.data);
        const normalizedTargetId = normalizeId(targetId);
        const initialSelectedIds = new Set(
          normalizedAssignments
            .filter((assignment) => normalizeId(assignment.staffId) === normalizedTargetId)
            .map((assignment) => normalizeId(assignment.productId))
        );

        setAssignments(normalizedAssignments);
        setSelectedProductIds(initialSelectedIds);
        selectedProductIdsRef.current = initialSelectedIds;
        setHasChanges(false);
      } catch (err) {
        console.error("Error loading product assignments", err);
        if (!cancelled) setError("Couldn't load products.");
      }
    }

    loadAssignments();
    return () => { cancelled = true; };
  }, [targetId]);

  // Products are loaded per page, refetched on page or search change
  useEffect(() => {
    let cancelled = false;

    async function loadProducts() {
      try {
        setLoading(true);
        setError(null);

        const productsRes = await api.get(`/api/v1/products`, {
          params: {
            page,
            size: pageSize,
            ...(searchTerm.trim() ? { query: searchTerm.trim() } : {}),
          },
        });

        if (cancelled) return;

        const data = productsRes.data;
        setProducts(data.content || data || []);
        setTotalPages(data.totalPages ?? 0);
        setTotalElements(data.totalElements ?? (data.content || data || []).length);
      } catch (err) {
        console.error("Error loading products", err);
        if (!cancelled) setError("Couldn't load products.");
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    loadProducts();
    return () => { cancelled = true; };
  }, [page, searchTerm]);

  // Reset to page 1 whenever the search term changes
  useEffect(() => {
    setPage(0);
  }, [searchTerm]);

  useEffect(() => {
    selectedProductIdsRef.current = selectedProductIds;
  }, [selectedProductIds]);

  const assignmentByProductId = new Map(
    assignments.map(a => [normalizeId(a.productId), a])
  );

  const normalizedSearch = searchTerm.trim().toLowerCase();
  const filteredProducts = normalizedSearch
    ? products.filter((product) =>
        [product.name, product.sku].some((value) =>
          String(value || "").toLowerCase().includes(normalizedSearch)
        )
      )
    : products;

  function toggleProduct(productId) {
    const normalizedProductId = normalizeId(productId);
    setSelectedProductIds(prev => {
      const next = new Set(prev);
      if (next.has(normalizedProductId)) {
        next.delete(normalizedProductId);
      } else {
        next.add(normalizedProductId);
      }
      selectedProductIdsRef.current = next;
      return next;
    });
    setHasChanges(true);
  }

  async function saveAssignments() {
    if (!hasChanges) return;

    setSaving(true);
    setError(null);

    try {
      const normalizedTargetId = normalizeId(targetId);
      const currentAssignedIds = new Set(
        assignments
          .filter((assignment) => normalizeId(assignment.staffId) === normalizedTargetId)
          .map((assignment) => normalizeId(assignment.productId))
      );

      // Only diffs the products currently loaded on this page —
      // toggles only ever happen on visible checkboxes.
      const operations = [];
      const latestSelectedProductIds = selectedProductIdsRef.current;
      for (const productId of products.map((product) => normalizeId(product.id))) {
        const wasAssigned = currentAssignedIds.has(productId);
        const willBeAssigned = latestSelectedProductIds.has(productId);

        if (wasAssigned && !willBeAssigned) {
          operations.push(api.delete(`/api/v1/products/${productId}/assign-staff`));
        } else if (!wasAssigned && willBeAssigned) {
          operations.push(api.post(`/api/v1/products/${productId}/assign-staff`, { staffId: targetId }));
        }
      }

      await Promise.all(operations);

      const refreshedAssignmentsRes = await api.get(`/api/v1/products/staff-assignments`);
      const refreshedAssignments = normalizeAssignments(refreshedAssignmentsRes.data);
      const refreshedSelectedIds = new Set(
        refreshedAssignments
          .filter((assignment) => normalizeId(assignment.staffId) === normalizedTargetId)
          .map((assignment) => normalizeId(assignment.productId))
      );

      setAssignments(refreshedAssignments);
      setSelectedProductIds(refreshedSelectedIds);
      selectedProductIdsRef.current = refreshedSelectedIds;
      setHasChanges(false);
    } catch (err) {
      console.error("Error saving product assignments", err);
      setError("Couldn't save product assignments. Please try again.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="ui-card p-6 mb-6">
      <div className="flex items-center justify-between mb-4">
        <h3 className="m-0 text-base font-semibold text-slate-900">Products handled</h3>
        <div className="flex items-center gap-3">
          <span className="text-xs text-slate-400">
            {Array.from(selectedProductIds).length} selected
          </span>
          <button
            type="button"
            onClick={saveAssignments}
            disabled={!hasChanges || saving}
            className="px-3 py-2 rounded-lg bg-brand-600 hover:bg-brand-700 text-white text-sm font-semibold disabled:opacity-60 disabled:cursor-not-allowed"
          >
            {saving ? "Saving..." : "Save changes"}
          </button>
        </div>
      </div>

      <div className="mb-4 relative">
        <input
          type="text"
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          placeholder="Search products..."
          className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-transparent"
        />
        {searchTerm && (
          <button
            type="button"
            onClick={() => setSearchTerm("")}
            className="absolute right-2 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 text-xs"
          >
            Clear
          </button>
        )}
      </div>

      {error && (
        <div className="mb-4 text-xs text-red-600 bg-red-50 border border-red-100 rounded-lg px-3 py-2">
          {error}
        </div>
      )}

      {loading ? (
        <div className="text-center py-6 text-slate-400 text-sm">Loading products...</div>
      ) : filteredProducts.length === 0 ? (
        <div className="text-center py-6 text-slate-400 text-sm">
          {searchTerm ? `No products match "${searchTerm}".` : "No products found."}
        </div>
      ) : (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
            {filteredProducts.map((p) => {
              const normalizedProductId = normalizeId(p.id);
              const assignment = assignmentByProductId.get(normalizedProductId);
              const isSelected = selectedProductIds.has(normalizedProductId);
              const isAssignedToTarget = isSelected;
              const isAssignedToOther = Boolean(assignment && normalizeId(assignment.staffId) !== normalizeId(targetId) && !isSelected);
              const isPending = pendingIds.has(p.id);

              return (
                <label
                  key={p.id}
                  className={`flex items-start gap-3 rounded-xl border p-3 cursor-pointer transition-colors ${
                    isAssignedToTarget
                      ? "border-brand-200 bg-brand-50"
                      : "border-slate-100 hover:bg-slate-50"
                  } ${isPending ? "opacity-50 pointer-events-none" : ""}`}
                >
                  <input
                    type="checkbox"
                    className="mt-1 h-4 w-4 rounded border-slate-300 text-brand-600 focus:ring-brand-500"
                    checked={isSelected}
                    disabled={saving}
                    onChange={() => toggleProduct(p.id)}
                  />
                  <div className="min-w-0">
                    <p className="text-sm font-medium text-slate-800 truncate">{p.name}</p>
                    {isAssignedToOther && (
                      <p className="text-xs text-amber-600 mt-0.5">
                        Currently with {assignment.staffName || "another staff member"}
                      </p>
                    )}
                  </div>
                </label>
              );
            })}
          </div>

          {/* Pagination controls */}
          <div className="flex items-center justify-between mt-5 pt-4 border-t border-slate-100">
            <p className="text-xs text-slate-400">
              Page {page + 1} of {Math.max(totalPages, 1)} &bull; {totalElements} products
            </p>
            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0 || saving}
                className="px-3 py-1.5 rounded-lg border border-slate-200 text-sm font-medium disabled:opacity-40 disabled:cursor-not-allowed hover:bg-slate-50"
              >
                Previous
              </button>
              <button
                type="button"
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1 || saving}
                className="px-3 py-1.5 rounded-lg border border-slate-200 text-sm font-medium disabled:opacity-40 disabled:cursor-not-allowed hover:bg-slate-50"
              >
                Next
              </button>
            </div>
          </div>
        </>
      )}
    </div>
  );
}

// ── Today stat card ──────────────────────────────────────────
function TodayStat({ label, value, money, raw, color }) {
  const colors = {
    blue:   "text-brand-600",
    green:  "text-green-600",
    purple: "text-violet-600",
    amber:  "text-amber-600",
  };
  const display = money ? `Ksh ${Number(value).toLocaleString()}` : value;
  return (
    <div className="ui-card p-4">
      <div className="flex items-center justify-between gap-2">
        <p className="m-0 text-xs font-medium text-slate-500">{label}</p>
        <span className="rounded-full bg-brand-50 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-brand-600">Today</span>
      </div>
      <p className={`m-0 mt-2 text-xl font-bold tracking-tight ${colors[color] || "text-slate-900"}`}>
        {display}
      </p>
    </div>
  );
}

// ── All time stat card ───────────────────────────────────────
function AllTimeStat({ label, value, money, color }) {
  const colors = {
    blue:   "text-brand-600",
    green:  "text-green-600",
    purple: "text-violet-600",
    amber:  "text-amber-600",
  };
  const display = money ? `Ksh ${Number(value).toLocaleString()}` : value;
  return (
    <div className="ui-card p-4">
      <div className="flex items-center justify-between gap-2">
        <p className="m-0 text-xs font-medium text-slate-500">{label}</p>
        <span className="rounded-full bg-slate-100 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-slate-500">All time</span>
      </div>
      <p className={`m-0 mt-2 text-xl font-bold tracking-tight ${colors[color] || "text-slate-900"}`}>
        {display}
      </p>
    </div>
  );
}

// ── Status badge ─────────────────────────────────────────────
function StatusBadge({ status }) {
  const s = String(status || "").toUpperCase();
  let bg = "#f3f4f6", color = "#374151";
  if (s.includes("DELIVERED") || s === "COMPLETED") { bg = "#dcfce7"; color = "#166534"; }
  else if (s.includes("DISPATCHED")) { bg = "#dbeafe"; color = "#1d4ed8"; }
  else if (s.includes("AWAITING"))   { bg = "#fef3c7"; color = "#92400e"; }
  else if (s === "CANCELLED")        { bg = "#fee2e2"; color = "#991b1b"; }
  return (
    <span style={{ background: bg, color, padding: "3px 10px", borderRadius: 999, fontSize: 11.5, fontWeight: 600, whiteSpace: "nowrap" }}>
      {String(status || "").replaceAll("_", " ").toLowerCase().replace(/\b\w/g, c => c.toUpperCase())}
    </span>
  );
}
