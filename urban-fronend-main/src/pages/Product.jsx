import React, { useEffect, useState } from "react";
import { FiPlus, FiEdit2, FiTrash2, FiBox, FiChevronLeft, FiChevronRight } from "react-icons/fi";
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
  tableCard,
  alertError,
  badgeStyle,
} from "../components/styles";
import { api } from "../api/api"; // adjust if needed
import { useAuth } from "../auth/useAuth";

export default function Products() {
  const { can } = useAuth();
  const [search, setSearch] = useState("");

  // Backend state
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  // Pageable
  const [page, setPage] = useState(0);
  const size = 10;
  const [pageData, setPageData] = useState(null);

  // Products list
  const [products, setProducts] = useState([]);

  // Categories (for dropdown)
  const [categories, setCategories] = useState([]);

  // Modal states
  const [isAddOpen, setIsAddOpen] = useState(false);
  const [editingProduct, setEditingProduct] = useState(null);

  // Add form
  const [form, setForm] = useState({
    name: "",
    sku: "",
    sellingPrice: "",
    costPrice: "",
    categoryId: "",
  });

  // Load products on mount, page change, or debounced search change.
  useEffect(() => {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => {
      loadProducts(page, search, controller.signal);
    }, search.trim() ? 250 : 0);

    return () => {
      clearTimeout(timeoutId);
      controller.abort();
    };
    // eslint-disable-next-line
  }, [page, search]);

  // Load categories once (for select dropdown)
  useEffect(() => {
    loadCategories();
    // eslint-disable-next-line
  }, []);

  async function loadProducts(requestPage = page, searchTerm = search, signal) {
    try {
      setLoading(true);
      setError("");

      const query = searchTerm.trim();
      const res = await api.get("/api/v1/products", {
        params: {
          page: requestPage,
          size,
          sort: "name,asc",
          ...(query ? { query } : {}),
        },
        signal,
      });

      if (signal?.aborted) return;
      const data = res.data;
      const list = Array.isArray(data) ? data : data?.content || [];

      setProducts(list);
      setPageData({
        totalElements: data?.totalElements ?? list.length,
        totalPages: data?.totalPages ?? 1,
        last: data?.last ?? true,
      });
    } catch (e) {
      if (signal?.aborted || e.code === "ERR_CANCELED") return;
      console.log(e);
      setError("Failed to load products. Check backend, token, and CORS.");
    } finally {
      if (!signal?.aborted) setLoading(false);
    }
  }

  async function loadCategories() {
    try {
      const res = await api.get("/api/v1/category", {
        params: {
          page: 0,
          size: 200,
          sort: "name,asc",
        },
      });

      setCategories(res.data.content || []);
    } catch (e) {
      console.log(e);
    }
  }

  function openAddModal() {
    if (!can("CAN_CREATE_PRODUCTS")) {
      setError("You don't have permission to add product.");
      return;
    }

    setForm({
      name: "",
      sku: "",
      sellingPrice: "",
      costPrice: "",
      categoryId: "",
    });
    setIsAddOpen(true);
  }

  function closeAddModal() {
    setIsAddOpen(false);
  }

  function handleFormChange(e) {
    setForm({ ...form, [e.target.name]: e.target.value });
  }

  function openEditModal(product) {
    setEditingProduct({
      ...product,
      categoryId: product.category?.id || product.categoryId || "",
      sellingPrice: product.sellingPrice ?? "",
      costPrice: product.costPrice ?? "",
    });
  }

  function closeEditModal() {
    setEditingProduct(null);
  }

  function handleEditChange(e) {
    setEditingProduct({ ...editingProduct, [e.target.name]: e.target.value });
  }

  function money(v) {
    if (v === null || v === undefined || v === "") return "-";
    return `KES ${Number(v).toLocaleString()}`;
  }

  function getCategoryName(product) {
    if (product?.category?.name) return product.category.name;

    const id = product.categoryId || product.category?.id;
    const found = categories.find((c) => c.id === id);
    return found?.name || "-";
  }

  async function handleAddProduct() {
    if (!form.name.trim() || !form.sku.trim()) {
      alert("Please fill product name and SKU.");
      return;
    }

    if (!form.categoryId) {
      alert("Please select a category.");
      return;
    }

    if (!form.sellingPrice || !form.costPrice) {
      alert("Please fill selling price and cost price.");
      return;
    }

    try {
      setLoading(true);
      setError("");

      await api.post("/api/v1/products", {
        name: form.name,
        sku: form.sku,
        sellingPrice: Number(form.sellingPrice),
        costPrice: Number(form.costPrice),
        categoryId: form.categoryId,
      });

      closeAddModal();
      if (page === 0) {
        await loadProducts(0, search);
      } else {
        setPage(0);
      }
    } catch (e) {
      console.error("Failed to add product:", e);
      if (e.response?.status === 403) {
        setError("You don't have permission to add product.");
      } else {
        setError("Failed to add product. Please try again.");
      }
    } finally {
      setLoading(false);
    }
  }

  async function handleSaveEdit() {
    if (!editingProduct.name.trim() || !editingProduct.sku.trim()) {
      alert("Please fill product name and SKU.");
      return;
    }

    if (!editingProduct.categoryId) {
      alert("Please select a category.");
      return;
    }

    if (!editingProduct.sellingPrice || !editingProduct.costPrice) {
      alert("Please fill selling price and cost price.");
      return;
    }

    try {
      setLoading(true);
      setError("");

      await api.put(`/api/v1/products/${editingProduct.id}`, {
        name: editingProduct.name,
        sku: editingProduct.sku,
        sellingPrice: Number(editingProduct.sellingPrice),
        costPrice: Number(editingProduct.costPrice),
        categoryId: editingProduct.categoryId,
      });

      closeEditModal();
      await loadProducts(page, search);
    } catch (e) {
      console.error("Failed to update product:", e);
      if (e.response?.status === 403) {
        setError("You don't have permission to edit product.");
      } else {
        setError("Failed to update product. Please try again.");
      }
    } finally {
      setLoading(false);
    }
  }

  async function handleDeleteProduct(id) {
    const ok = window.confirm("Are you sure you want to delete this product?");
    if (!ok) return;

    try {
      setLoading(true);
      setError("");

      await api.delete(`/api/v1/products/${id}`);
      await loadProducts(page, search);
    } catch (e) {
      console.log(e);
      alert("Failed to delete product. Check console.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div>
      {/* Header */}
      <PageHeader title="Products" subtitle="Add and manage products in your shop">
        <button onClick={openAddModal} style={saveBtn}>
          <FiPlus size={16} /> Add Product
        </button>
      </PageHeader>

      {/* Toolbar */}
      <div
        className="prod-toolbar"
        style={{
          display: "flex",
          justifyContent: "space-between",
          gap: 12,
          flexWrap: "wrap",
          marginBottom: 14,
        }}
      >
        {/* Search */}
        <SearchInput
          value={search}
          onChange={(e) => {
              setPage(0);
              setSearch(e.target.value);
            }}
          placeholder="Search products..."
          className="prod-search w-full sm:w-80"
        />
      </div>

      {/* Loading/Error */}
      {error && <div style={{ ...alertError, marginBottom: 16 }}>{error}</div>}

      {/* Products */}
      <div style={tableCard}>
        {/* Desktop headings */}
        <div
          className="prod-desktop-header desktop-header"
          style={{
            display: "grid",
            gridTemplateColumns: "1.6fr 1.2fr 1.2fr 1.2fr 1.3fr 1fr",
            gap: 10,
            padding: "14px 16px",
            background: "var(--surface-muted)",
          }}
        >
          <div>Name</div>
          <div>SKU</div>
          <div>Selling</div>
          <div>Cost</div>
          <div>Category</div>
          <div>Actions</div>
        </div>

        {/* Rows */}
        {products.map((p, idx) => (
          <React.Fragment key={p.id}>
            {/* Desktop row */}
            <div
              className="prod-desktop-row ui-row"
              style={{
                display: "grid",
                gridTemplateColumns: "1.6fr 1.2fr 1.2fr 1.2fr 1.3fr 1fr",
                gap: 10,
                padding: "14px 16px",
                borderTop: idx === 0 ? "none" : "1px solid #f3f4f6",
                alignItems: "center",
                fontSize: 14,
              }}
            >
              <div style={{ fontWeight: 600 }}>{p.name}</div>
              <div style={{ fontFamily: "ui-monospace, SFMono-Regular, Menlo, monospace", fontSize: 13, color: "var(--text-muted)" }}>{p.sku}</div>
              <div style={{ fontWeight: 600 }}>{money(p.sellingPrice)}</div>
              <div style={{ color: "var(--text-muted)" }}>{money(p.costPrice)}</div>
              <div>
                <span style={badgeStyle("indigo")}>{getCategoryName(p)}</span>
              </div>

              <div style={{ display: "flex", gap: 8, whiteSpace: "nowrap" }}>
                <button onClick={() => openEditModal(p)} style={actionBtn}>
                  <FiEdit2 size={13} /> Edit
                </button>

                <button
                  onClick={() => handleDeleteProduct(p.id)}
                  style={dangerActionBtn}
                >
                  <FiTrash2 size={13} /> Delete
                </button>
              </div>
            </div>

            {/* Mobile card */}
            <div className="prod-mobile-card">
              <div className="prod-mobile-top">
                <div className="prod-mobile-name">{p.name}</div>
                <div className="prod-mobile-sku">{p.sku}</div>
              </div>

              <div className="prod-mobile-grid">
                <div className="prod-mobile-field">
                  <div className="prod-mobile-label">Selling</div>
                  <div className="prod-mobile-value" style={{ fontWeight: 700 }}>
                    {money(p.sellingPrice)}
                  </div>
                </div>

                <div className="prod-mobile-field">
                  <div className="prod-mobile-label">Cost</div>
                  <div className="prod-mobile-value" style={{ fontWeight: 700 }}>
                    {money(p.costPrice)}
                  </div>
                </div>

                <div className="prod-mobile-field">
                  <div className="prod-mobile-label">Category</div>
                  <div className="prod-mobile-value" style={{ fontWeight: 600 }}>
                    {getCategoryName(p)}
                  </div>
                </div>
              </div>

              <div className="prod-mobile-actions">
                <button onClick={() => openEditModal(p)} style={actionBtn}>
                  <FiEdit2 size={13} /> Edit
                </button>

                <button
                  onClick={() => handleDeleteProduct(p.id)}
                  style={dangerActionBtn}
                >
                  <FiTrash2 size={13} /> Delete
                </button>
              </div>
            </div>
          </React.Fragment>
        ))}

        {loading && products.length === 0 && <LoadingBlock label="Loading products..." />}
        {!loading && products.length === 0 && (
          <EmptyState
            icon={FiBox}
            title={search.trim() ? "No matching products" : "No products yet"}
            message={search.trim() ? `No products match "${search}".` : "Add your first product to get started."}
          />
        )}

        {/* Footer pagination */}
        {pageData && (
          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              padding: "12px 16px",
              alignItems: "center",
              borderTop: "1px solid var(--border)",
              background: "#fff",
              color: "var(--text-muted)",
              fontSize: 14,
              flexWrap: "wrap",
              gap: 10,
            }}
          >
            <div>
              Showing {products.length} of {pageData.totalElements} products
            </div>

            <div style={{ display: "flex", gap: 10, alignItems: "center" }}>
              <button
                style={pageBtnStyle}
                disabled={page === 0}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
              >
                <FiChevronLeft size={14} /> Prev
              </button>

              <span style={{ fontWeight: 600, color: "#334155" }}>
                {page + 1} / {pageData.totalPages || 1}
              </span>

              <button
                style={pageBtnStyle}
                disabled={pageData.last}
                onClick={() => setPage((p) => p + 1)}
              >
                Next <FiChevronRight size={14} />
              </button>
            </div>
          </div>
        )}
      </div>

      {/* ADD MODAL */}
      {isAddOpen && (
        <div style={modalOverlay}>
          <div style={modalBox}>
            <h3 style={modalTitle}>Add Product</h3>

            <div style={{ display: "grid", gap: 12 }}>
              <input
                name="name"
                value={form.name}
                onChange={handleFormChange}
                placeholder="Product Name"
                style={inputStyle}
              />

              <input
                name="sku"
                value={form.sku}
                onChange={handleFormChange}
                placeholder="SKU"
                style={inputStyle}
              />

              <input
                name="sellingPrice"
                value={form.sellingPrice}
                onChange={handleFormChange}
                placeholder="Selling Price"
                type="number"
                style={inputStyle}
              />

              <input
                name="costPrice"
                value={form.costPrice}
                onChange={handleFormChange}
                placeholder="Cost Price"
                type="number"
                style={inputStyle}
              />

              <select
                name="categoryId"
                value={form.categoryId}
                onChange={handleFormChange}
                style={inputStyle}
              >
                <option value="">Select Category</option>
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            </div>

            <div style={modalFooter}>
              <button onClick={closeAddModal} style={cancelBtn}>
                Cancel
              </button>

              <button onClick={handleAddProduct} style={saveBtn}>
                Add Product
              </button>
            </div>
          </div>
        </div>
      )}

      {/* EDIT MODAL */}
      {editingProduct && (
        <div style={modalOverlay}>
          <div style={modalBox}>
            <h3 style={modalTitle}>Edit Product</h3>

            <div style={{ display: "grid", gap: 12 }}>
              <input
                name="name"
                value={editingProduct.name}
                onChange={handleEditChange}
                placeholder="Product Name"
                style={inputStyle}
              />

              <input
                name="sku"
                value={editingProduct.sku}
                onChange={handleEditChange}
                placeholder="SKU"
                style={inputStyle}
              />

              <input
                name="sellingPrice"
                value={editingProduct.sellingPrice}
                onChange={handleEditChange}
                placeholder="Selling Price"
                type="number"
                style={inputStyle}
              />

              <input
                name="costPrice"
                value={editingProduct.costPrice}
                onChange={handleEditChange}
                placeholder="Cost Price"
                type="number"
                style={inputStyle}
              />

              <select
                name="categoryId"
                value={editingProduct.categoryId}
                onChange={handleEditChange}
                style={inputStyle}
              >
                <option value="">Select Category</option>
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
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

      {/* Responsive styles */}
      <style>{`
        /* Mobile cards hidden on desktop */
        .prod-mobile-card { display: none; }

        @media (max-width: 768px) {
          .prod-search { min-width: 100% !important; }

          /* Hide desktop table */
          .prod-desktop-header,
          .prod-desktop-row {
            display: none !important;
          }

          /* Show mobile cards */
          .prod-mobile-card {
            display: block;
            padding: 14px 16px;
            border-top: 1px solid #f3f4f6;
            background: #fff;
          }

          .prod-mobile-top {
            display: flex;
            justify-content: space-between;
            gap: 10px;
            align-items: baseline;
            margin-bottom: 10px;
          }

          .prod-mobile-name {
            font-weight: 600;
            font-size: 16px;
            color: #111827;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            max-width: 70%;
          }

          .prod-mobile-sku {
            font-weight: 500;
            font-size: 13px;
            color: #64748b;
            white-space: nowrap;
          }

          .prod-mobile-grid {
            display: grid;
            grid-template-columns: 1fr;
            gap: 10px;
            margin-bottom: 12px;
          }

          .prod-mobile-field {
            display: flex;
            justify-content: space-between;
            align-items: center;
            gap: 12px;
          }

          .prod-mobile-label {
            color: #64748b;
            font-size: 13px;
            font-weight: 500;
          }

          .prod-mobile-value { text-align: right; }

          .prod-mobile-actions {
            display: flex;
            gap: 8px;
            flex-wrap: wrap;
          }
        }
      `}</style>
    </div>
  );
}
