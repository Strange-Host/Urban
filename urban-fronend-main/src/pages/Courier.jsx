import React, { useEffect, useMemo, useState } from "react";
import { api } from "../api/api";
import { FiPlus, FiTruck, FiPackage, FiEdit2, FiSlash, FiChevronLeft, FiChevronRight, FiX } from "react-icons/fi";
import { PageHeader, SearchInput, EmptyState, LoadingBlock } from "../components/ui";
import {
  inputStyle,
  actionBtn,
  dangerActionBtn,
  pageBtnStyle,
  modalOverlay,
  modalBox,
  modalTitle,
  modalFooter,
  secondaryBtn as cancelBtn,
  primaryBtn as saveBtn,
  closeBtn as xBtn,
  tableCard,
  alertError,
  badgeStyle,
} from "../components/styles";

export default function Couriers() {
  const [search, setSearch] = useState("");

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const [page, setPage] = useState(0);
  const size = 10;
  const [pageData, setPageData] = useState(null);

  const [couriers, setCouriers] = useState([]);
  const [showInactive, setShowInactive] = useState(false);

  const [isAddOpen, setIsAddOpen] = useState(false);
  const [editingCourier, setEditingCourier] = useState(null);
  const [selectedCourier, setSelectedCourier] = useState(null);
  const [courierOrders, setCourierOrders] = useState([]);
  const [ordersLoading, setOrdersLoading] = useState(false);
  const [ordersError, setOrdersError] = useState("");

  const [form, setForm] = useState({
    name: "",
    phoneNumber: "",
    vehicleInfo: "",
    notes: "",
  });

  useEffect(() => {
    loadCouriers();
    // eslint-disable-next-line
  }, [page, showInactive]);

  async function loadCouriers() {
    try {
      setLoading(true);
      setError("");

      const res = await api.get("/api/v1/couriers", {
        params: { page, size, activeOnly: !showInactive },
      });

      setPageData(res.data);
      setCouriers(res.data.content || []);
    } catch (e) {
      setError("Failed to load couriers.");
    } finally {
      setLoading(false);
    }
  }

  const filtered = useMemo(() => {
    return couriers.filter((c) => {
      const q = search.toLowerCase();
      return (
        (c.name || "").toLowerCase().includes(q) ||
        (c.phoneNumber || "").toLowerCase().includes(q) ||
        (c.vehicleInfo || "").toLowerCase().includes(q)
      );
    });
  }, [couriers, search]);

  function openAddModal() {
    setForm({ name: "", phoneNumber: "", vehicleInfo: "", notes: "" });
    setIsAddOpen(true);
  }

  function closeAddModal() {
    setIsAddOpen(false);
  }

  function handleFormChange(e) {
    setForm({ ...form, [e.target.name]: e.target.value });
  }

  async function handleAddCourier() {
    if (!form.name.trim()) {
      alert("Courier name is required.");
      return;
    }
    if (!form.phoneNumber.trim()) {
      alert("Courier phone number is required.");
      return;
    }

    try {
      setLoading(true);
      await api.post("/api/v1/couriers", form);
      closeAddModal();
      setPage(0);
      await loadCouriers();
    } catch (e) {
      alert(e.response?.data?.message || "Failed to add courier.");
    } finally {
      setLoading(false);
    }
  }

  function openEditModal(courier) {
    setEditingCourier({ ...courier });
  }

  function closeEditModal() {
    setEditingCourier(null);
  }

  function handleEditChange(e) {
    const { name, value, type, checked } = e.target;
    setEditingCourier({
      ...editingCourier,
      [name]: type === "checkbox" ? checked : value,
    });
  }

  async function handleSaveEdit() {
    if (!editingCourier.name.trim()) {
      alert("Courier name is required.");
      return;
    }
    if (!editingCourier.phoneNumber.trim()) {
      alert("Courier phone number is required.");
      return;
    }

    try {
      setLoading(true);
      await api.put(`/api/v1/couriers/${editingCourier.id}`, editingCourier);
      closeEditModal();
      await loadCouriers();
    } catch (e) {
      alert(e.response?.data?.message || "Failed to update courier.");
    } finally {
      setLoading(false);
    }
  }

  async function handleDeactivateCourier(id) {
    if (!window.confirm("Deactivate this courier? They won't show up when assigning couriers to new orders, but past orders keep their history.")) return;

    try {
      setLoading(true);
      await api.delete(`/api/v1/couriers/${id}`);
      await loadCouriers();
    } catch {
      alert("Failed to deactivate courier.");
    } finally {
      setLoading(false);
    }
  }

  async function openCourierOrders(courier) {
    setSelectedCourier(courier);
    setCourierOrders([]);
    setOrdersError("");
    setOrdersLoading(true);

    try {
      const res = await api.get("/api/v1/orders", {
        params: {
          page: 0,
          size: 10,
          sort: "createdAt,desc",
          courierId: String(courier.id),
        },
      });
      setCourierOrders(Array.isArray(res.data) ? res.data : res.data.content || []);
    } catch (e) {
      const status = e.response?.status;
      setOrdersError(
        status === 403
          ? "You do not have permission to view orders."
          : status === 404
            ? "The orders endpoint was not found."
            : "Failed to load orders for this courier."
      );
    } finally {
      setOrdersLoading(false);
    }
  }

  function closeCourierOrders() {
    setSelectedCourier(null);
    setCourierOrders([]);
    setOrdersError("");
  }

  return (
    <div>

      {/* RESPONSIVE CSS */}
      <style>{`
        @media (max-width: 768px) {

          .desktop-header,
          .desktop-row {
            display: none !important;
          }

          .mobile-card {
            display: block;
            padding: 14px;
            border-top: 1px solid #f3f4f6;
            background: #fff;
          }

          .toolbar {
            flex-direction: column !important;
            align-items: stretch !important;
          }

          .toolbar button {
            width: 100%;
          }

          .search-box {
            width: 100% !important;
            min-width: 100% !important;
          }
        }

        @media (min-width: 769px) {
          .mobile-card {
            display: none;
          }
        }
      `}</style>

      {/* Header */}
      <PageHeader title="Couriers" subtitle="Manage the couriers you dispatch orders to">
        <button onClick={openAddModal} style={saveBtn}>
          <FiPlus size={16} /> Add Courier
        </button>
      </PageHeader>

      {/* Toolbar */}
      <div
        className="toolbar"
        style={{
          display: "flex",
          alignItems: "center",
          gap: 16,
          flexWrap: "wrap",
          marginBottom: 16,
        }}
      >
        {/* Search */}
        <SearchInput
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Search couriers..."
          className="search-box w-full sm:w-80"
        />

        <label
          style={{
            display: "flex",
            alignItems: "center",
            gap: 8,
            fontSize: 14,
            fontWeight: 500,
            color: "#334155",
          }}
        >
          <input
            type="checkbox"
            checked={showInactive}
            onChange={(e) => {
              setPage(0);
              setShowInactive(e.target.checked);
            }}
          />
          Show inactive
        </label>
      </div>

      {error && (
        <div style={{ ...alertError, marginBottom: 16 }}>
          {error}
        </div>
      )}

      {/* Table Container */}
      <div style={tableCard}>
        {/* Desktop Header */}
        <div
          className="desktop-header"
          style={{
            display: "grid",
            gridTemplateColumns: "1.2fr 1fr 1.2fr 0.8fr 1fr",
            gap: 10,
            padding: "14px 16px",
            background: "var(--surface-muted)",
          }}
        >
          <div>Name</div>
          <div>Phone</div>
          <div>Vehicle Info</div>
          <div>Status</div>
          <div>Actions</div>
        </div>

        {loading && filtered.length === 0 && <LoadingBlock label="Loading couriers..." />}
        {!loading && filtered.length === 0 && (
          <EmptyState icon={FiTruck} title="No couriers found" message="Try adjusting your search or filters." />
        )}

        {/* Rows */}
        {filtered.map((c, idx) => (
          <React.Fragment key={c.id}>
            {/* Desktop row */}
            <div
              className="desktop-row"
              onClick={() => openCourierOrders(c)}
              style={{
                display: "grid",
                gridTemplateColumns: "1.2fr 1fr 1.2fr 0.8fr 1fr",
                gap: 10,
                padding: "14px 16px",
                borderTop: idx === 0 ? "none" : "1px solid #f3f4f6",
                alignItems: "center",
                cursor: "pointer",
              }}
            >
              <div style={{ fontWeight: 600 }}>{c.name}</div>
              <div>{c.phoneNumber}</div>
              <div>{c.vehicleInfo || "-"}</div>
              <div>
                <span style={badgeStyle(c.active ? "green" : "gray")}>
                  {c.active ? "Active" : "Inactive"}
                </span>
              </div>
              <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
                <button onClick={(ev) => { ev.stopPropagation(); openCourierOrders(c); }} style={actionBtn}>
                  <FiPackage size={13} /> Orders
                </button>
                <button onClick={(e) => { e.stopPropagation(); openEditModal(c); }} style={actionBtn}>
                  <FiEdit2 size={13} /> Edit
                </button>
                {c.active && (
                  <button
                    onClick={(e) => { e.stopPropagation(); handleDeactivateCourier(c.id); }}
                    style={dangerActionBtn}
                  >
                    <FiSlash size={13} /> Deactivate
                  </button>
                )}
              </div>
            </div>

            {/* Mobile Card */}
            <div className="mobile-card" onClick={() => openCourierOrders(c)} style={{ cursor: "pointer" }}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <div style={{ fontWeight: 600, fontSize: 15 }}>{c.name}</div>
                <span style={badgeStyle(c.active ? "green" : "gray")}>
                  {c.active ? "Active" : "Inactive"}
                </span>
              </div>
              <div style={{ marginTop: 6, color: "#6b7280" }}>{c.phoneNumber}</div>
              <div style={{ marginTop: 2, color: "#6b7280" }}>
                {c.vehicleInfo || "No vehicle info"}
              </div>
              <div style={{ display: "flex", gap: 8, marginTop: 12, flexWrap: "wrap" }}>
                <button onClick={(ev) => { ev.stopPropagation(); openCourierOrders(c); }} style={actionBtn}>
                  <FiPackage size={13} /> Orders
                </button>
                <button onClick={(e) => { e.stopPropagation(); openEditModal(c); }} style={actionBtn}>
                  <FiEdit2 size={13} /> Edit
                </button>
                {c.active && (
                  <button
                    onClick={(e) => { e.stopPropagation(); handleDeactivateCourier(c.id); }}
                    style={dangerActionBtn}
                  >
                    <FiSlash size={13} /> Deactivate
                  </button>
                )}
              </div>
            </div>
          </React.Fragment>
        ))}
      </div>

      {/* Pagination */}
      {pageData && pageData.totalPages > 1 && (
        <div style={{ display: "flex", justifyContent: "center", gap: 10, marginTop: 16 }}>
          <button
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={page === 0}
            style={pageBtnStyle}
          >
            <FiChevronLeft size={14} /> Prev
          </button>
          <span style={{ alignSelf: "center", fontSize: 13, color: "#6b7280" }}>
            Page {page + 1} of {pageData.totalPages}
          </span>
          <button
            onClick={() => setPage((p) => Math.min(pageData.totalPages - 1, p + 1))}
            disabled={page + 1 >= pageData.totalPages}
            style={pageBtnStyle}
          >
            Next <FiChevronRight size={14} />
          </button>
        </div>
      )}

      {/* ADD MODAL */}
      {isAddOpen && (
        <div style={modalOverlay}>
          <div style={modalBox}>
            <h3 style={modalTitle}>Add Courier</h3>
            <div style={{ display: "grid", gap: 12 }}>
              <input
                name="name"
                value={form.name}
                onChange={handleFormChange}
                placeholder="Courier Name"
                style={inputStyle}
              />
              <input
                name="phoneNumber"
                value={form.phoneNumber}
                onChange={handleFormChange}
                placeholder="Phone Number"
                style={inputStyle}
              />
              <input
                name="vehicleInfo"
                value={form.vehicleInfo}
                onChange={handleFormChange}
                placeholder="Vehicle Info (e.g. Motorbike - KMEA 123X)"
                style={inputStyle}
              />
              <textarea
                name="notes"
                value={form.notes}
                onChange={handleFormChange}
                placeholder="Notes (e.g. coverage area)"
                rows={3}
                style={{ ...inputStyle, resize: "none" }}
              />
            </div>

            <div style={modalFooter}>
              <button onClick={closeAddModal} style={cancelBtn}>
                Cancel
              </button>
              <button onClick={handleAddCourier} style={saveBtn}>
                Add Courier
              </button>
            </div>
          </div>
        </div>
      )}

      {/* EDIT MODAL */}
      {editingCourier && (
        <div style={modalOverlay}>
          <div style={modalBox}>
            <h3 style={modalTitle}>Edit Courier</h3>
            <div style={{ display: "grid", gap: 12 }}>
              <input
                name="name"
                value={editingCourier.name}
                onChange={handleEditChange}
                style={inputStyle}
              />
              <input
                name="phoneNumber"
                value={editingCourier.phoneNumber}
                onChange={handleEditChange}
                style={inputStyle}
              />
              <input
                name="vehicleInfo"
                value={editingCourier.vehicleInfo || ""}
                onChange={handleEditChange}
                style={inputStyle}
              />
              <textarea
                name="notes"
                value={editingCourier.notes || ""}
                onChange={handleEditChange}
                rows={3}
                style={{ ...inputStyle, resize: "none" }}
              />
              <label style={{ display: "flex", alignItems: "center", gap: 8, fontWeight: 700 }}>
                <input
                  type="checkbox"
                  name="active"
                  checked={!!editingCourier.active}
                  onChange={handleEditChange}
                />
                Active
              </label>
            </div>

            <div style={modalFooter}>
              <button onClick={closeEditModal} style={cancelBtn}>
                Cancel
              </button>
              <button onClick={handleSaveEdit} style={saveBtn}>
                Save Changes
              </button>
            </div>
          </div>
        </div>
      )}

      {selectedCourier && (
        <div style={modalOverlay} onClick={closeCourierOrders}>
          <div style={ordersModalBox} onClick={(e) => e.stopPropagation()}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 12 }}>
              <div>
                <h3 style={{ ...modalTitle, margin: 0 }}>{selectedCourier.name}'s Orders</h3>
                <div style={{ marginTop: 4, color: "#6b7280", fontSize: 13 }}>
                  Orders assigned to this courier
                </div>
              </div>
              <button onClick={closeCourierOrders} style={xBtn}><FiX size={16} /></button>
            </div>

            {ordersLoading && <LoadingBlock label="Loading orders..." />}
            {ordersError && <div style={{ ...alertError, marginTop: 16 }}>{ordersError}</div>}
            {!ordersLoading && !ordersError && courierOrders.length === 0 && (
              <EmptyState icon={FiPackage} title="No orders yet" message="No orders found for this courier." />
            )}
            {!ordersLoading && !ordersError && courierOrders.length > 0 && (
              <div style={{ overflowX: "auto", marginTop: 16, border: "1px solid var(--border)", borderRadius: 12 }}>
                <table className="ui-table">
                  <thead>
                    <tr>
                      <th>Customer</th>
                      <th>Product</th>
                      <th>Amount</th>
                      <th>Status</th>
                      <th>Date</th>
                    </tr>
                  </thead>
                  <tbody>
                    {courierOrders.map((order) => (
                      <tr key={order.id}>
                        <td>{order.customerName || "-"}</td>
                        <td>{order.productName || "-"}</td>
                        <td style={{ fontWeight: 600 }}>
                          KES {Number(order.totalAmount || 0).toLocaleString()}
                        </td>
                        <td><span style={badgeStyle("indigo")}>{order.orderStatus || "-"}</span></td>
                        <td>{order.createdAt ? String(order.createdAt).slice(0, 10) : "-"}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}


const ordersModalBox = {
  ...modalBox,
  width: "900px",
};
