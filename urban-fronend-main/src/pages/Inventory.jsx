import React, { useEffect, useMemo, useState } from "react";
import { FiPlus, FiMinus, FiArchive, FiPrinter, FiRefreshCw } from "react-icons/fi";
import { PageHeader, SearchInput, EmptyState, LoadingBlock } from "../components/ui";
import {
  inputStyle,
  actionBtn,
  modalOverlay,
  modalBox,
  modalTitle,
  modalFooter,
  secondaryBtn as cancelBtn,
  primaryBtn as saveBtn,
  tableCard,
  alertError,
} from "../components/styles";
import { api } from "../api/api"; // adjust path if needed
import { useAuth } from "../auth/useAuth";
import StickerModal from "../components/StickerModal";

export default function Inventory() {
  const [activeTab, setActiveTab] = useState("All Inventory");
  const [search, setSearch] = useState("");

  // Backend state
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  // Products
  const [products, setProducts] = useState([]);

  // Inventory map: productId -> inventory
  const [inventoryMap, setInventoryMap] = useState({});

  // Low stock list (normalized to products shape)
  const [lowStock, setLowStock] = useState([]);

  // Modal states
  const [restockProduct, setRestockProduct] = useState(null);
  const [reduceProduct, setReduceProduct] = useState(null);
  const [stickerModal, setStickerModal] = useState(null); // { mode, product }

  const { can } = useAuth();
  const canPrintStickers = can("CAN_RESTOCK_INVENTORY");

  // Forms
  const [restockForm, setRestockForm] = useState({
    quantity: "",
    reorderLevel: "",
  });

  const [reduceForm, setReduceForm] = useState({
    quantity: "",
  });

  const tabs = ["All Inventory", "Low Stock"];

  useEffect(() => {
    loadProducts();
    loadLowStock();
  }, []);

  async function loadProducts() {
    try {
      setLoading(true);
      setError("");

      const res = await api.get("/api/v1/products", {
        params: { page: 0, size: 200, sort: "createdAt,desc" },
      });

      const list = res.data.content || [];
      setProducts(list);

      await loadInventory();
    } catch (e) {
      console.log(e);
      setError("Failed to load products/inventory.");
    } finally {
      setLoading(false);
    }
  }

  async function loadInventory() {
    try {
      const res = await api.get("/api/v1/inventory");
      const list = Array.isArray(res.data) ? res.data : res.data?.content || [];

      const map = {};
      list.forEach((inv) => {
        const productId = inv.product?.id || inv.productId;
        if (productId) map[productId] = inv;
      });

      setInventoryMap(map);
    } catch (e) {
      console.log(e);
    }
  }

  async function loadLowStock() {
    try {
      const res = await api.get("/api/v1/inventory/low-stock");
      const data = res.data || [];

      const normalized = data
        .map((x) => {
          if (x.id && x.name && x.sku) return x;

          return {
            id: x.productId || x.product?.id || x.id,
            name: x.productName || x.product?.name || x.name,
            sku: x.sku || x.product?.sku || "",
            quantity: x.quantity,
            reorderLevel: x.reorderLevel,
          };
        })
        .filter((x) => x.id);

      setLowStock(normalized);
    } catch (e) {
      console.log(e);
    }
  }

  const filteredProducts = useMemo(() => {
    return products.filter((p) => {
      const q = search.toLowerCase();
      return (
        (p.name || "").toLowerCase().includes(q) ||
        (p.sku || "").toLowerCase().includes(q)
      );
    });
  }, [products, search]);

  function getStock(productId) {
    const inv = inventoryMap[productId];
    if (!inv) return "-";

    if (inv.quantity !== undefined) return inv.quantity;
    if (inv.stock !== undefined) return inv.stock;
    if (inv.available !== undefined) return inv.available;

    return "-";
  }

  function openRestockModal(product) {
    setRestockProduct(product);
    setRestockForm({ quantity: "", reorderLevel: "" });
  }

  function closeRestockModal() {
    setRestockProduct(null);
  }

  function openReduceModal(product) {
    setReduceProduct(product);
    setReduceForm({ quantity: "" });
  }

  function closeReduceModal() {
    setReduceProduct(null);
  }

  async function handleRestock() {
    if (!restockForm.quantity || Number(restockForm.quantity) <= 0) {
      alert("Enter a valid restock quantity.");
      return;
    }

    if (
      restockForm.reorderLevel === "" ||
      Number(restockForm.reorderLevel) < 0
    ) {
      alert("Enter a valid reorder level (0 or above).");
      return;
    }

    try {
      setLoading(true);
      setError("");

      await api.post("/api/v1/inventory/restock", {
        productId: restockProduct.id,
        quantity: Number(restockForm.quantity),
        reorderLevel: Number(restockForm.reorderLevel),
      });

      closeRestockModal();
      await loadProducts();
      await loadLowStock();
    } catch (e) {
      console.log(e);
      alert("Restock failed.");
    } finally {
      setLoading(false);
    }
  }

  async function handleReduce() {
    if (!reduceForm.quantity || Number(reduceForm.quantity) <= 0) {
      alert("Enter a valid reduce quantity.");
      return;
    }

    try {
      setLoading(true);
      setError("");

      await api.post("/api/v1/inventory/reduce", {
        productId: reduceProduct.id,
        quantity: Number(reduceForm.quantity),
      });

      closeReduceModal();
      await loadProducts();
      await loadLowStock();
    } catch (e) {
      console.log(e);
      alert("Reduce failed.");
    } finally {
      setLoading(false);
    }
  }

  function stockBadge(stock) {
    const qty = Number(stock);
    if (isNaN(qty)) return { bg: "#f3f4f6", color: "#374151" };

    if (qty <= 5) return { bg: "#fee2e2", color: "#991b1b" };
    if (qty <= 15) return { bg: "#ffedd5", color: "#b45309" };

    return { bg: "#dcfce7", color: "#166534" };
  }

  const tableData = activeTab === "Low Stock" ? lowStock : filteredProducts;

  return (
    <div>
      {/* RESPONSIVE CSS */}
      <style>{`
        @media (max-width: 768px) {
          .inv-toolbar {
            flex-direction: column !important;
            align-items: stretch !important;
            gap: 12px !important;
          }

          .inv-search {
            min-width: 100% !important;
            width: 100% !important;
          }

          .inv-tabs {
            width: 100%;
            display: flex;
            flex-wrap: wrap;
            gap: 8px;
          }

          .inv-desktop-header,
          .inv-desktop-row {
            display: none !important;
          }

          .inv-mobile-card {
            display: block;
            padding: 14px;
            border-top: 1px solid #f3f4f6;
            background: #fff;
          }

          .inv-actions {
            flex-wrap: wrap;
            white-space: normal !important;
          }
        }

        @media (min-width: 769px) {
          .inv-mobile-card {
            display: none;
          }
        }
      `}</style>

      {/* Header */}
      <PageHeader title="Inventory" subtitle="Track stock, restock, and reduce inventory">
        {canPrintStickers && (
          <button onClick={() => setStickerModal({ mode: "reprint" })} style={actionBtn}>
            <FiRefreshCw size={13} /> Reprint Stickers
          </button>
        )}
      </PageHeader>

      {/* Tabs + Search */}
      <div
        className="inv-toolbar"
        style={{
          display: "flex",
          gap: 12,
          marginBottom: 16,
          flexWrap: "wrap",
          alignItems: "center",
        }}
      >
        <div className="inv-tabs" style={{ display: "inline-flex", gap: 4, padding: 4, background: "#eef0f5", borderRadius: 12 }}>
          {tabs.map((tab) => {
            const active = activeTab === tab;
            return (
              <button
                key={tab}
                onClick={() => setActiveTab(tab)}
                style={{
                  border: "none",
                  padding: "8px 14px",
                  borderRadius: 9,
                  background: active ? "#fff" : "transparent",
                  color: active ? "var(--text)" : "#64748b",
                  boxShadow: active ? "var(--shadow-sm)" : "none",
                  fontWeight: 600,
                  fontSize: 14,
                }}
              >
                {tab}
              </button>
            );
          })}
        </div>

        <div style={{ flex: 1 }} />

        <SearchInput
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Search product..."
          className="inv-search w-full sm:w-72"
        />
      </div>

      {/* Loading/Error */}
      {error && <div style={{ ...alertError, marginBottom: 16 }}>{error}</div>}

      {/* TABLE */}
      <div style={tableCard}>
        {/* Desktop Header */}
        <div
          className="inv-desktop-header desktop-header"
          style={{
            display: "grid",
            gridTemplateColumns: "1.3fr 0.8fr 0.6fr 2.3fr",
            gap: 10,
            padding: "14px 16px",
            background: "var(--surface-muted)",
          }}
        >
          <div>Product</div>
          <div>SKU</div>
          <div>Stock</div>
          <div>Actions</div>
        </div>

        {/* Rows */}
        {tableData.map((p, idx) => {
          const stock =
            p.quantity !== undefined && p.quantity !== null
              ? p.quantity
              : getStock(p.id);
          const badge = stockBadge(stock);

          return (
            <React.Fragment key={p.id}>
              {/* Desktop Row */}
              <div
                className="inv-desktop-row ui-row"
                style={{
                  display: "grid",
                  gridTemplateColumns: "1.3fr 0.8fr 0.6fr 2.3fr",
                  gap: 10,
                  padding: "14px 16px",
                  borderTop: idx === 0 ? "none" : "1px solid #f3f4f6",
                  alignItems: "center",
                  fontSize: 14,
                }}
              >
                <div style={{ fontWeight: 600 }}>{p.name}</div>
                <div style={{ fontFamily: "ui-monospace, SFMono-Regular, Menlo, monospace", fontSize: 13, color: "var(--text-muted)" }}>{p.sku}</div>

                <div>
                  <span
                    style={{
                      background: badge.bg,
                      color: badge.color,
                      padding: "4px 10px",
                      borderRadius: 999,
                      fontWeight: 600,
                      fontSize: 12.5,
                    }}
                  >
                    {stock}
                  </span>
                </div>

                <div className="inv-actions" style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
                  <button onClick={() => openRestockModal(p)} style={actionBtn}>
                    <FiPlus size={13} /> Restock
                  </button>

                  {canPrintStickers && (
                    <>
                      <button onClick={() => setStickerModal({ mode: "restock", product: p })} style={actionBtn}>
                        <FiPrinter size={13} /> Restock & Print Stickers
                      </button>
                      <button onClick={() => setStickerModal({ mode: "print", product: p })} style={actionBtn}>
                        <FiPrinter size={13} /> Print Stickers
                      </button>
                    </>
                  )}

                  <button
                    onClick={() => openReduceModal(p)}
                    style={{ ...actionBtn, background: "#fff7ed", borderColor: "#fed7aa", color: "#9a3412" }}
                  >
                    <FiMinus size={13} /> Reduce
                  </button>
                </div>
              </div>

              {/* Mobile Card */}
              <div className="inv-mobile-card">
                <div style={{ fontWeight: 600, fontSize: 15 }}>{p.name}</div>
                <div style={{ marginTop: 6, color: "#6b7280" }}>
                  SKU: <b style={{ color: "#374151" }}>{p.sku || "-"}</b>
                </div>

                <div style={{ marginTop: 10 }}>
                  <span
                    style={{
                      background: badge.bg,
                      color: badge.color,
                      padding: "4px 10px",
                      borderRadius: 999,
                      fontWeight: 600,
                      fontSize: 12.5,
                      display: "inline-block",
                    }}
                  >
                    Stock: {stock}
                  </span>
                </div>

                <div className="inv-actions" style={{ display: "flex", gap: 8, marginTop: 12 }}>
                  <button onClick={() => openRestockModal(p)} style={actionBtn}>
                    <FiPlus size={13} /> Restock
                  </button>

                  {canPrintStickers && (
                    <>
                      <button onClick={() => setStickerModal({ mode: "restock", product: p })} style={actionBtn}>
                        <FiPrinter size={13} /> Restock & Print Stickers
                      </button>
                      <button onClick={() => setStickerModal({ mode: "print", product: p })} style={actionBtn}>
                        <FiPrinter size={13} /> Print Stickers
                      </button>
                    </>
                  )}

                  <button
                    onClick={() => openReduceModal(p)}
                    style={{ ...actionBtn, background: "#fff7ed", borderColor: "#fed7aa", color: "#9a3412" }}
                  >
                    <FiMinus size={13} /> Reduce
                  </button>
                </div>
              </div>
            </React.Fragment>
          );
        })}

        {loading && tableData.length === 0 && <LoadingBlock label="Loading inventory..." />}
        {!loading && tableData.length === 0 && (
          <EmptyState icon={FiArchive} title="No inventory found" message="Try a different tab or search term." />
        )}
      </div>

      {/* RESTOCK MODAL */}
      {restockProduct && (
        <div style={modalOverlay}>
          <div style={modalBox}>
            <h3 style={{ ...modalTitle, marginBottom: 4 }}>Restock</h3>

            <p style={{ marginTop: 0, marginBottom: 16, color: "var(--text-muted)", fontSize: 14 }}>
              Product: <b style={{ color: "var(--text)" }}>{restockProduct.name}</b>
            </p>

            <input
              value={restockForm.quantity}
              onChange={(e) =>
                setRestockForm({ ...restockForm, quantity: e.target.value })
              }
              placeholder="Quantity"
              type="number"
              style={inputStyle}
            />

            <input
              value={restockForm.reorderLevel}
              onChange={(e) =>
                setRestockForm({
                  ...restockForm,
                  reorderLevel: e.target.value,
                })
              }
              placeholder="Reorder Level"
              type="number"
              style={{ ...inputStyle, marginTop: 12 }}
            />

            <div style={modalFooter}>
              <button onClick={closeRestockModal} style={cancelBtn}>
                Cancel
              </button>

              <button onClick={handleRestock} style={saveBtn}>
                Restock
              </button>
            </div>
          </div>
        </div>
      )}

      {/* STICKER MODAL */}
      {stickerModal && (
        <StickerModal
          mode={stickerModal.mode}
          product={stickerModal.product}
          onClose={() => setStickerModal(null)}
          onSuccess={() => {
            loadProducts();
            loadLowStock();
          }}
        />
      )}

      {/* REDUCE MODAL */}
      {reduceProduct && (
        <div style={modalOverlay}>
          <div style={modalBox}>
            <h3 style={{ ...modalTitle, marginBottom: 4 }}>Reduce Stock</h3>

            <p style={{ marginTop: 0, marginBottom: 16, color: "var(--text-muted)", fontSize: 14 }}>
              Product: <b style={{ color: "var(--text)" }}>{reduceProduct.name}</b>
            </p>

            <input
              value={reduceForm.quantity}
              onChange={(e) => setReduceForm({ quantity: e.target.value })}
              placeholder="Quantity"
              type="number"
              style={inputStyle}
            />

            <div style={modalFooter}>
              <button onClick={closeReduceModal} style={cancelBtn}>
                Cancel
              </button>

              <button onClick={handleReduce} style={{ ...saveBtn, background: "#ea580c" }}>
                Reduce
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
