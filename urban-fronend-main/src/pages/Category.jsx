import React, { useEffect, useMemo, useState } from "react";
import { FiPlus, FiEdit2, FiTrash2, FiTag } from "react-icons/fi";
import { api } from "../api/api";
import { PageHeader, SearchInput, EmptyState, LoadingBlock } from "../components/ui";
import {
  inputStyle,
  actionBtn,
  dangerActionBtn,
  modalOverlay,
  modalBox,
  modalTitle,
  modalFooter,
  secondaryBtn as cancelBtn,
  primaryBtn as saveBtn,
  tableCard,
  alertError,
} from "../components/styles";

export default function Categories() {
  const [search, setSearch] = useState("");

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const [page, setPage] = useState(0);
  const size = 10;
  const [pageData, setPageData] = useState(null);

  const [categories, setCategories] = useState([]);

  const [isAddOpen, setIsAddOpen] = useState(false);
  const [editingCategory, setEditingCategory] = useState(null);

  const [form, setForm] = useState({
    name: "",
    description: "",
  });

  useEffect(() => {
    loadCategories();
  }, [page]);

  async function loadCategories() {
    try {
      setLoading(true);
      setError("");

      const res = await api.get("/api/v1/category", {
        params: { page, size, sort: "createdAt,desc" },
      });

      setPageData(res.data);
      setCategories(res.data.content || []);
    } catch (e) {
      setError("Failed to load categories.");
    } finally {
      setLoading(false);
    }
  }

  const filtered = useMemo(() => {
    return categories.filter((c) => {
      const q = search.toLowerCase();
      return (
        (c.name || "").toLowerCase().includes(q) ||
        (c.description || "").toLowerCase().includes(q)
      );
    });
  }, [categories, search]);

  function openAddModal() {
    setForm({ name: "", description: "" });
    setIsAddOpen(true);
  }

  function closeAddModal() {
    setIsAddOpen(false);
  }

  function handleFormChange(e) {
    setForm({ ...form, [e.target.name]: e.target.value });
  }

  async function handleAddCategory() {
    if (!form.name.trim()) {
      alert("Category name is required.");
      return;
    }

    try {
      setLoading(true);
      await api.post("/api/v1/category", form);
      closeAddModal();
      setPage(0);
      await loadCategories();
    } catch {
      alert("Failed to add category.");
    } finally {
      setLoading(false);
    }
  }

  function openEditModal(cat) {
    setEditingCategory({ ...cat });
  }

  function closeEditModal() {
    setEditingCategory(null);
  }

  function handleEditChange(e) {
    setEditingCategory({
      ...editingCategory,
      [e.target.name]: e.target.value,
    });
  }

  async function handleSaveEdit() {
    if (!editingCategory.name.trim()) {
      alert("Category name is required.");
      return;
    }

    try {
      setLoading(true);
      await api.put(
        `/api/v1/category/${editingCategory.id}`,
        editingCategory
      );
      closeEditModal();
      await loadCategories();
    } catch {
      alert("Failed to update category.");
    } finally {
      setLoading(false);
    }
  }

  async function handleDeleteCategory(id) {
    if (!window.confirm("Delete this category?")) return;

    try {
      setLoading(true);
      await api.delete(`/api/v1/category/${id}`);
      await loadCategories();
    } catch {
      alert("Failed to delete category.");
    } finally {
      setLoading(false);
    }
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

      <PageHeader title="Categories" subtitle="Create and manage product categories">
        <button onClick={openAddModal} style={saveBtn}>
          <FiPlus size={16} /> Add Category
        </button>
      </PageHeader>

      {/* Toolbar */}
      <div className="toolbar" style={{ display: "flex", gap: 12, flexWrap: "wrap", marginBottom: 16 }}>
        <SearchInput
          value={search}
          onChange={(ev) => setSearch(ev.target.value)}
          placeholder="Search categories..."
          className="search-box w-full sm:w-80"
        />
      </div>

      {error && <div style={{ ...alertError, marginBottom: 16 }}>{error}</div>}

      {/* Table Container */}
      <div style={tableCard}>
        {/* Desktop Header */}
        <div
          className="desktop-header"
          style={{
            display: "grid",
            gridTemplateColumns: "1.2fr 2fr 1fr",
            gap: 10,
            padding: "14px 16px",
            background: "var(--surface-muted)",
          }}
        >
          <div>Name</div>
          <div>Description</div>
          <div>Actions</div>
        </div>

        {loading && categories.length === 0 && <LoadingBlock label="Loading categories..." />}
        {!loading && filtered.length === 0 && (
          <EmptyState
            icon={FiTag}
            title={search ? "No matching categories" : "No categories yet"}
            message={search ? "Try a different search term." : "Add your first category to get started."}
          />
        )}

        {/* Rows */}
        {filtered.map((c, idx) => (
          <React.Fragment key={c.id}>
            {/* Desktop row */}
            <div
              className="desktop-row"
              style={{
                display: "grid",
                gridTemplateColumns: "1.2fr 2fr 1fr",
                gap: 10,
                padding: "14px 16px",
                borderTop: idx === 0 ? "none" : "1px solid #f3f4f6",
                alignItems: "center",
              }}
            >
              <div style={{ fontWeight: 600, color: "var(--text)" }}>{c.name}</div>
              <div style={{ color: "var(--text-muted)", fontSize: 14 }}>{c.description || "—"}</div>
              <div style={{ display: "flex", gap: 8 }}>
                <button onClick={() => openEditModal(c)} style={actionBtn}>
                  <FiEdit2 size={13} /> Edit
                </button>
                <button
                  onClick={() => handleDeleteCategory(c.id)}
                  style={dangerActionBtn}
                >
                  <FiTrash2 size={13} /> Delete
                </button>
              </div>
            </div>

            {/* Mobile Card */}
            <div className="mobile-card">
              <div style={{ fontWeight: 600, fontSize: 15 }}>
                {c.name}
              </div>
              <div style={{ marginTop: 6, color: "#6b7280" }}>
                {c.description || "No description"}
              </div>
              <div style={{ display: "flex", gap: 8, marginTop: 12 }}>
                <button onClick={() => openEditModal(c)} style={actionBtn}>
                  <FiEdit2 size={13} /> Edit
                </button>
                <button
                  onClick={() => handleDeleteCategory(c.id)}
                  style={dangerActionBtn}
                >
                  <FiTrash2 size={13} /> Delete
                </button>
              </div>
            </div>
          </React.Fragment>
        ))}
      </div>

      {/* ADD MODAL */}
      {isAddOpen && (
        <div style={modalOverlay}>
          <div style={modalBox}>
            <h3 style={modalTitle}>Add Category</h3>
            <div style={{ display: "grid", gap: 12 }}>
              <input
                name="name"
                value={form.name}
                onChange={handleFormChange}
                placeholder="Category Name"
                style={inputStyle}
              />
              <textarea
                name="description"
                value={form.description}
                onChange={handleFormChange}
                placeholder="Description"
                rows={4}
                style={{ ...inputStyle, resize: "none" }}
              />
            </div>

            <div style={modalFooter}>
              <button onClick={closeAddModal} style={cancelBtn}>
                Cancel
              </button>
              <button onClick={handleAddCategory} style={saveBtn}>
                Add Category
              </button>
            </div>
          </div>
        </div>
      )}

      {/* EDIT MODAL */}
      {editingCategory && (
        <div style={modalOverlay}>
          <div style={modalBox}>
            <h3 style={modalTitle}>Edit Category</h3>
            <div style={{ display: "grid", gap: 12 }}>
              <input
                name="name"
                value={editingCategory.name}
                onChange={handleEditChange}
                style={inputStyle}
              />
              <textarea
                name="description"
                value={editingCategory.description || ""}
                onChange={handleEditChange}
                rows={4}
                style={{ ...inputStyle, resize: "none" }}
              />
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
    </div>
  );
}
