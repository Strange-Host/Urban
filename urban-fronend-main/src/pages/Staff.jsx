import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../api/api";
import { useAuth } from "../auth/useAuth";
import PermissionGate from "../auth/PermissionGate";
import {
  FiEye,
  FiEyeOff,
  FiPlus,
  FiEdit2,
  FiShield,
  FiTrash2,
  FiUsers,
  FiX,
  FiChevronLeft,
  FiChevronRight,
} from "react-icons/fi";
import { invalidateStaff } from "../api/referenceData";
import { PageHeader, SearchInput, EmptyState, LoadingBlock } from "../components/ui";
import {
  inputStyle,
  labelStyle,
  actionBtn,
  dangerActionBtn,
  pageBtnStyle,
  modalOverlay,
  modalBox,
  modalTitle,
  modalFooter,
  closeBtn,
  secondaryBtn as cancelBtn,
  primaryBtn as saveBtn,
  tableCard,
  alertError,
  alertSuccess,
  badgeStyle,
} from "../components/styles";

const PERMISSION_GROUPS = {
  "CRM / Leads":  ["CAN_VIEW_LEADS","CAN_CREATE_LEADS","CAN_UPDATE_LEAD_STATUS","CAN_CONVERT_LEADS"],
  "Orders":       ["CAN_VIEW_ORDERS","CAN_CREATE_ORDERS","CAN_UPDATE_ORDER_STATUS","CAN_UPDATE_DELIVERY_NOTES","CAN_VIEW_ORDER_SUMMARY","CAN_VIEW_TOP_SELLING"],
  "Products":     ["CAN_VIEW_PRODUCTS","CAN_CREATE_PRODUCTS","CAN_EDIT_PRODUCTS","CAN_DELETE_PRODUCTS"],
  "Categories":   ["CAN_VIEW_CATEGORIES","CAN_CREATE_CATEGORIES","CAN_EDIT_CATEGORIES","CAN_DELETE_CATEGORIES"],
  "Customers":    ["CAN_VIEW_CUSTOMERS","CAN_CREATE_CUSTOMERS","CAN_DELETE_CUSTOMERS"],
  "Inventory":    ["CAN_VIEW_INVENTORY","CAN_VIEW_LOW_STOCK","CAN_RESTOCK_INVENTORY","CAN_REDUCE_INVENTORY"],
  "Payments":     ["CAN_VIEW_PAYMENTS","CAN_CREATE_PAYMENTS","CAN_MATCH_PAYMENTS","CAN_VIEW_ORDER_PAYMENTS"],
  "Requisitions": ["CAN_VIEW_REQUISITIONS","CAN_CREATE_REQUISITIONS","CAN_VIEW_OWN_REQUISITIONS","CAN_APPROVE_REQUISITIONS","CAN_REJECT_REQUISITIONS","CAN_MARK_REQUISITION_PAID"],
  "Expenses":     ["CAN_VIEW_EXPENSES","CAN_CREATE_EXPENSES","CAN_DELETE_EXPENSES"],
  "Payroll":      ["CAN_VIEW_OWN_PAYROLL","CAN_VIEW_PAYROLL_SUMMARY"],
  // "Reports":      ["CAN_VIEW_PROFIT_REPORT"],
  "Admin":        ["CAN_VIEW_STAFF","CAN_DELETE_STAFF","CAN_MANAGE_PERMISSIONS"],
};

const PERMISSION_LABELS = {
  CAN_VIEW_LEADS: "View leads", CAN_CREATE_LEADS: "Create leads",
  CAN_UPDATE_LEAD_STATUS: "Update lead status", CAN_CONVERT_LEADS: "Convert leads",
  CAN_VIEW_ORDERS: "View orders", CAN_CREATE_ORDERS: "Create orders",
  CAN_UPDATE_ORDER_STATUS: "Update order status", CAN_UPDATE_DELIVERY_NOTES: "Update delivery notes",
  CAN_VIEW_ORDER_SUMMARY: "View order summary", CAN_VIEW_TOP_SELLING: "View top selling",
  CAN_VIEW_PRODUCTS: "View products", CAN_CREATE_PRODUCTS: "Create products",
  CAN_EDIT_PRODUCTS: "Edit products",
  CAN_DELETE_PRODUCTS: "Delete products", CAN_VIEW_CATEGORIES: "View categories",
  CAN_CREATE_CATEGORIES: "Create categories", CAN_EDIT_CATEGORIES: "Edit categories",
  CAN_DELETE_CATEGORIES: "Delete categories", CAN_VIEW_CUSTOMERS: "View customers",
  CAN_CREATE_CUSTOMERS: "Create customers", CAN_DELETE_CUSTOMERS: "Delete customers",
  CAN_VIEW_INVENTORY: "View inventory", CAN_VIEW_LOW_STOCK: "View low stock",
  CAN_RESTOCK_INVENTORY: "Restock inventory", CAN_REDUCE_INVENTORY: "Reduce inventory",
  CAN_VIEW_PAYMENTS: "View payments", CAN_CREATE_PAYMENTS: "Create payments",
  CAN_MATCH_PAYMENTS: "Match payments", CAN_VIEW_ORDER_PAYMENTS: "View order payments",
  CAN_VIEW_REQUISITIONS: "View requisitions", CAN_CREATE_REQUISITIONS: "Create requisitions",
  CAN_VIEW_OWN_REQUISITIONS: "View own requisitions", CAN_APPROVE_REQUISITIONS: "Approve requisitions",
  CAN_REJECT_REQUISITIONS: "Reject requisitions", CAN_MARK_REQUISITION_PAID: "Mark requisition paid",
  CAN_VIEW_EXPENSES: "View expenses", CAN_CREATE_EXPENSES: "Create expenses",
  CAN_DELETE_EXPENSES: "Delete expenses", CAN_VIEW_OWN_PAYROLL: "View own payroll",
  CAN_VIEW_PAYROLL_SUMMARY: "View payroll summary",
  CAN_VIEW_STAFF: "View staff", CAN_DELETE_STAFF: "Delete staff",
  CAN_MANAGE_PERMISSIONS: "Manage permissions",
};

export default function Staff() {
  const navigate = useNavigate();
  const { can } = useAuth();

  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [page, setPage] = useState(0);
  const size = 10;
  const [pageData, setPageData] = useState(null);
  const [staffList, setStaffList] = useState([]);
  const roles = ["CASHIER", "MANAGER", "STAFF"];
  const [isAddOpen, setIsAddOpen] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [form, setForm] = useState({
    fullName: "", email: "", phone: "", role: "CASHIER", password: "",
  });

  const [permStaff, setPermStaff] = useState(null);
  const [grantedPerms, setGrantedPerms] = useState([]);
  const [permLoading, setPermLoading] = useState(false);
  const [permSaving, setPermSaving] = useState(false);
  const [permSuccess, setPermSuccess] = useState("");
  // Saving is only allowed once the current permissions were loaded
  // successfully; otherwise an empty list would revoke everything.
  const [permLoaded, setPermLoaded] = useState(false);
  const [permError, setPermError] = useState("");

  useEffect(() => {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => {
      loadStaff(page, search.trim(), controller.signal);
    }, search.trim() ? 300 : 0);

    return () => {
      clearTimeout(timeoutId);
      controller.abort();
    };
    // eslint-disable-next-line
  }, [page, search]);

  async function loadStaff(requestPage = page, query = "", signal) {
    try {
      setLoading(true);
      setError("");
      const res = await api.get("/api/v1/auth/staffs", {
        params: {
          page: requestPage,
          size,
          sort: "createdAt,desc",
          query: query || undefined,
        },
        signal,
      });
      if (signal?.aborted) return;
      setPageData(res.data);
      setStaffList(res.data.content || []);
    } catch (e) {
      if (signal?.aborted || e.code === "ERR_CANCELED") return;
      setError("Failed to load staff.");
    } finally {
      if (!signal?.aborted) setLoading(false);
    }
  }

  async function openPermissions(staff) {
    setPermStaff(staff);
    setGrantedPerms([]);
    setPermSuccess("");
    setPermError("");
    setPermLoaded(false);
    setPermLoading(true);
    try {
      const res = await api.get(`/api/v1/admin/permissions/${staff.id}`);
      const granted = res.data.filter(p => p.granted).map(p => p.permission);
      setGrantedPerms(granted);
      setPermLoaded(true);
    } catch (e) {
      console.error("Failed to load permissions", e);
      setPermError("Could not load this user's permissions. Close and try again; nothing was changed.");
    } finally {
      setPermLoading(false);
    }
  }

  function closePermissions() {
    setPermStaff(null);
    setGrantedPerms([]);
    setPermSuccess("");
    setPermError("");
    setPermLoaded(false);
  }

  function togglePerm(perm) {
    setGrantedPerms(prev =>
      prev.includes(perm) ? prev.filter(p => p !== perm) : [...prev, perm]
    );
  }

  async function savePermissions() {
    if (!permLoaded) return;
    try {
      setPermSaving(true);
      await api.put(`/api/v1/admin/permissions/${permStaff.id}`, grantedPerms);
      setPermSuccess("Permissions saved successfully!");
      setTimeout(() => setPermSuccess(""), 3000);
    } catch {
      alert("Failed to save permissions.");
    } finally {
      setPermSaving(false);
    }
  }

  function openAddModal() {
    setForm({ fullName: "", email: "", phone: "", role: "CASHIER", password: "" });
    setShowPassword(false);
    setIsAddOpen(true);
  }

  function closeAddModal() { setIsAddOpen(false); }
  function handleAddChange(e) { setForm({ ...form, [e.target.name]: e.target.value }); }

  function splitName(fullName) {
    const parts = String(fullName || "").trim().split(" ").filter(Boolean);
    if (parts.length === 0) return { firstName: "", lastName: "" };
    if (parts.length === 1) return { firstName: parts[0], lastName: "" };
    return { firstName: parts[0], lastName: parts.slice(1).join(" ") };
  }

  async function handleAddStaff() {
    if (!form.fullName || !form.phone || !form.email || !form.password) {
      alert("Please fill all fields.");
      return;
    }
    try {
      setLoading(true);
      const { firstName, lastName } = splitName(form.fullName);
      await api.post("/api/v1/auth/register", {
        firstName, lastName,
        email: form.email,
        phoneNumber: form.phone,
        password: form.password,
        role: form.role,
      });
      closeAddModal();
      setPage(0);
      invalidateStaff();
      await loadStaff(0, search.trim());
    } catch {
      alert("Failed to add staff.");
    } finally {
      setLoading(false);
    }
  }

  async function handleDeleteStaff(id) {
    if (!window.confirm("Delete this staff member?")) return;
    try {
      setLoading(true);
      await api.delete(`/api/v1/auth/user/${id}`);
      invalidateStaff();
      await loadStaff(page, search.trim());
    } catch {
      alert("Delete failed.");
    } finally {
      setLoading(false);
    }
  }

  function normalizeRole(role) {
    if (!role) return "-";
    return role.charAt(0) + role.slice(1).toLowerCase();
  }

  const cols = "1.6fr 1.2fr 0.9fr 0.8fr 1.7fr";

  function initials(name) {
    const parts = (name || "").split(/\s+/).filter(Boolean);
    return ((parts[0]?.[0] || "?") + (parts[1]?.[0] || "")).toUpperCase();
  }

  function rowActions(s) {
    return (
      <>
        <PermissionGate permission="CAN_VIEW_STAFF">
          <button
            onClick={(e) => { e.stopPropagation(); navigate(`/staff/${s.id}`); }}
            style={actionBtn}
          >
            <FiEdit2 size={13} /> Edit
          </button>
        </PermissionGate>

        <PermissionGate permission="CAN_MANAGE_PERMISSIONS">
          <button
            onClick={(e) => { e.stopPropagation(); openPermissions(s); }}
            style={{ ...actionBtn, background: "var(--brand-soft)", color: "#4338ca", borderColor: "#c7d2fe" }}
          >
            <FiShield size={13} /> Permissions
          </button>
        </PermissionGate>

        <PermissionGate permission="CAN_DELETE_STAFF">
          <button
            onClick={(e) => { e.stopPropagation(); handleDeleteStaff(s.id); }}
            style={dangerActionBtn}
          >
            <FiTrash2 size={13} /> Delete
          </button>
        </PermissionGate>
      </>
    );
  }

  return (
    <div>
      <PageHeader title="Staff" subtitle="Add and manage your staff accounts">
        <PermissionGate permission="CAN_MANAGE_PERMISSIONS">
          <button onClick={openAddModal} style={saveBtn}>
            <FiPlus size={16} /> Add Staff
          </button>
        </PermissionGate>
      </PageHeader>

      <div className="mb-4">
        <SearchInput
          value={search}
          onChange={(e) => {
            setPage(0);
            setSearch(e.target.value);
          }}
          placeholder="Search staff..."
          className="w-full sm:w-80"
        />
      </div>

      {error && <div style={{ ...alertError, marginBottom: 16 }}>{error}</div>}

      <div style={tableCard}>
        {/* Table header */}
        <div
          className="desktop-header hidden md:grid"
          style={{ gridTemplateColumns: cols, gap: 10, padding: "12px 16px", background: "var(--surface-muted)" }}
        >
          <div>Name</div>
          <div>Phone</div>
          <div>Role</div>
          <div>Status</div>
          <div>Actions</div>
        </div>

        {loading && staffList.length === 0 && <LoadingBlock label="Loading staff..." />}

        {/* Rows */}
        {staffList.map((s) => {
          const fullName = `${s.firstName || ""} ${s.lastName || ""}`.trim();
          return (
            <div
              key={s.id}
              onClick={() => navigate(`/staff/${s.id}`)}
              className="ui-row cursor-pointer border-b border-slate-100"
            >
              {/* Desktop */}
              <div
                className="hidden items-center text-sm md:grid"
                style={{ gridTemplateColumns: cols, gap: 10, padding: "12px 16px" }}
              >
                <div className="flex min-w-0 items-center gap-3">
                  <span className="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-brand-50 text-xs font-semibold text-brand-700">
                    {initials(fullName)}
                  </span>
                  <span className="truncate font-medium text-slate-900">{fullName || "-"}</span>
                </div>
                <div className="text-slate-600">{s.phoneNumber || "-"}</div>
                <div>
                  <span style={badgeStyle("indigo")}>{normalizeRole(s.role)}</span>
                </div>
                <div>
                  <span style={badgeStyle("green")}>Active</span>
                </div>
                <div className="flex flex-wrap gap-1.5">{rowActions(s)}</div>
              </div>

              {/* Mobile */}
              <div className="p-4 md:hidden">
                <div className="flex items-center gap-3">
                  <span className="grid h-10 w-10 shrink-0 place-items-center rounded-full bg-brand-50 text-sm font-semibold text-brand-700">
                    {initials(fullName)}
                  </span>
                  <div className="min-w-0 flex-1">
                    <div className="truncate font-medium text-slate-900">{fullName || "-"}</div>
                    <div className="text-sm text-slate-500">{s.phoneNumber || "-"}</div>
                  </div>
                  <span style={badgeStyle("indigo")}>{normalizeRole(s.role)}</span>
                </div>
                <div className="mt-3 flex flex-wrap gap-2">{rowActions(s)}</div>
              </div>
            </div>
          );
        })}

        {staffList.length === 0 && !loading && (
          <EmptyState icon={FiUsers} title="No staff found" message={search ? "Try a different search term." : undefined} />
        )}

        {/* Pagination */}
        {pageData && (
          <div className="flex flex-wrap items-center justify-between gap-3 px-4 py-3 text-sm text-slate-500">
            <div>
              Showing <span className="font-medium text-slate-700">{staffList.length}</span> of{" "}
              <span className="font-medium text-slate-700">{pageData.totalElements}</span> staff
            </div>
            <div className="flex items-center gap-2">
              <button style={pageBtnStyle} disabled={page === 0} onClick={() => setPage((p) => Math.max(0, p - 1))}>
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

      {/* ADD STAFF MODAL */}
      {isAddOpen && can("CAN_MANAGE_PERMISSIONS") && (
        <div style={modalOverlay} onClick={closeAddModal}>
          <div style={modalBox} onClick={(e) => e.stopPropagation()}>
            <h3 style={modalTitle}>Add Staff</h3>
            <div className="grid gap-4">
              <div>
                <label style={labelStyle}>Full name</label>
                <input name="fullName" value={form.fullName} onChange={handleAddChange} placeholder="Jane Wanjiku" style={inputStyle} />
              </div>
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div>
                  <label style={labelStyle}>Email</label>
                  <input name="email" value={form.email} onChange={handleAddChange} placeholder="jane@company.com" style={inputStyle} />
                </div>
                <div>
                  <label style={labelStyle}>Phone</label>
                  <input name="phone" value={form.phone} onChange={handleAddChange} placeholder="07XX XXX XXX" style={inputStyle} />
                </div>
              </div>
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div>
                  <label style={labelStyle}>Password</label>
                  <div
                    className="ui-field"
                    style={{ ...inputStyle, display: "flex", alignItems: "center", padding: "0 6px 0 0" }}
                  >
                    <input
                      name="password"
                      value={form.password}
                      onChange={handleAddChange}
                      placeholder="Password"
                      type={showPassword ? "text" : "password"}
                      style={{ ...inputStyle, flex: 1, minWidth: 0, border: "none", background: "transparent" }}
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword((visible) => !visible)}
                      aria-label={showPassword ? "Hide password" : "Show password"}
                      title={showPassword ? "Hide password" : "Show password"}
                      style={{ display: "grid", placeItems: "center", border: "none", background: "transparent", color: "#64748b", padding: 6, borderRadius: 8 }}
                    >
                      {showPassword ? <FiEyeOff size={17} /> : <FiEye size={17} />}
                    </button>
                  </div>
                </div>
                <div>
                  <label style={labelStyle}>Role</label>
                  <select name="role" value={form.role} onChange={handleAddChange} style={inputStyle}>
                    {roles.map((r) => <option key={r} value={r}>{normalizeRole(r)}</option>)}
                  </select>
                </div>
              </div>
            </div>
            <div style={modalFooter}>
              <button onClick={closeAddModal} style={cancelBtn}>Cancel</button>
              <button onClick={handleAddStaff} disabled={loading} style={saveBtn}>
                {loading ? "Adding..." : "Add Staff"}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* PERMISSIONS MODAL */}
      {permStaff && (
        <div style={modalOverlay} onClick={closePermissions}>
          <div style={{ ...modalBox, width: 720, padding: 0 }} onClick={(e) => e.stopPropagation()}>
            <div className="sticky top-0 z-10 flex items-start justify-between gap-3 border-b border-slate-100 bg-white px-6 py-5">
              <div className="flex items-center gap-3">
                <span className="grid h-10 w-10 place-items-center rounded-xl bg-brand-50 text-brand-600">
                  <FiShield size={18} />
                </span>
                <div>
                  <h3 style={{ ...modalTitle, margin: 0 }}>
                    {permStaff.firstName} {permStaff.lastName}
                  </h3>
                  <div className="mt-0.5 text-sm text-slate-500">
                    {normalizeRole(permStaff.role)} · {grantedPerms.length} permission{grantedPerms.length === 1 ? "" : "s"} granted
                  </div>
                </div>
              </div>
              <button onClick={closePermissions} style={closeBtn} aria-label="Close">
                <FiX size={16} />
              </button>
            </div>

            <div className="px-6 py-5">
              {permSuccess && (
                <div style={{ ...alertSuccess, marginBottom: 16 }}>{permSuccess}</div>
              )}

              {permError && (
                <div style={{ ...alertError, marginBottom: 16 }}>{permError}</div>
              )}

              {permLoading ? (
                <LoadingBlock label="Loading permissions..." />
              ) : !permLoaded ? null : (
                Object.entries(PERMISSION_GROUPS).map(([group, perms]) => (
                  <div key={group} className="mb-5 last:mb-0">
                    <div className="mb-2 text-[11px] font-semibold uppercase tracking-wider text-slate-500">{group}</div>
                    <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
                      {perms.map((perm) => {
                        const checked = grantedPerms.includes(perm);
                        return (
                          <label
                            key={perm}
                            onClick={() => togglePerm(perm)}
                            className={`flex cursor-pointer items-center gap-2.5 rounded-lg border px-3 py-2 transition-colors ${
                              checked
                                ? "border-brand-300 bg-brand-50 text-brand-800"
                                : "border-slate-200 bg-white text-slate-700 hover:border-slate-300"
                            }`}
                          >
                            <span
                              className={`grid h-4 w-4 shrink-0 place-items-center rounded ${
                                checked ? "bg-brand-600" : "border-[1.5px] border-slate-300 bg-white"
                              }`}
                            >
                              {checked && (
                                <svg width="10" height="10" viewBox="0 0 10 10">
                                  <polyline points="1,5 4,8 9,2" stroke="white" strokeWidth="1.5" fill="none" strokeLinecap="round" />
                                </svg>
                              )}
                            </span>
                            <span className={`text-[13px] ${checked ? "font-medium" : ""}`}>
                              {PERMISSION_LABELS[perm] || perm}
                            </span>
                          </label>
                        );
                      })}
                    </div>
                  </div>
                ))
              )}
            </div>

            <div className="sticky bottom-0 flex justify-end gap-2.5 border-t border-slate-100 bg-white px-6 py-4">
              <button onClick={closePermissions} style={cancelBtn}>Close</button>
              <button onClick={savePermissions} disabled={permSaving || permLoading || !permLoaded} style={saveBtn}>
                {permSaving ? "Saving..." : "Save Permissions"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
