import React, { useEffect, useState } from "react";
import { FiCheckCircle, FiDownload, FiExternalLink } from "react-icons/fi";
import {
  inputStyle,
  labelStyle,
  modalOverlay,
  modalBox,
  modalTitle,
  modalFooter,
  secondaryBtn,
  primaryBtn,
  alertError,
  alertSuccess,
} from "./styles";
import { Spinner } from "./ui";
import {
  STICKER_LAYOUTS,
  getSavedLayout,
  saveLayout,
  restockWithStickers,
  printProductStickers,
  reprintBatch,
  getErrorDetail,
} from "../api/stickersApi";

const TITLES = {
  restock: "Restock & Print Stickers",
  print: "Print Stickers",
  reprint: "Reprint Stickers",
};

const LAYOUT_HINTS = {
  A4: "24 stickers per A4 sheet",
  ROLL: "One 50×30mm label per page (thermal printer)",
};

/**
 * mode: "restock" | "print" | "reprint"
 * product: required for restock/print
 * onSuccess: called after a restock succeeds so the caller can refresh data
 */
export default function StickerModal({ mode, product, onClose, onSuccess }) {
  const [form, setForm] = useState({ quantity: "", reorderLevel: "", batchId: "" });
  const [layout, setLayout] = useState(getSavedLayout);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [result, setResult] = useState(null); // { url, batchId, message }

  useEffect(() => {
    if (!result?.url) return undefined;
    const url = result.url;
    // Give the opened tab time to finish loading before releasing the blob.
    return () => setTimeout(() => URL.revokeObjectURL(url), 60000);
  }, [result]);

  function chooseLayout(value) {
    setLayout(value);
    saveLayout(value);
  }

  function validate() {
    if (mode === "reprint") {
      return form.batchId.trim() ? "" : "Enter a batch ID.";
    }
    const qty = Number(form.quantity);
    if (!Number.isInteger(qty) || qty < 1 || qty > 5000) {
      return "Quantity must be a whole number between 1 and 5000.";
    }
    if (mode === "restock" && form.reorderLevel !== "") {
      const level = Number(form.reorderLevel);
      if (!Number.isInteger(level) || level < 0) return "Reorder level must be 0 or above.";
    }
    return "";
  }

  async function handleSubmit() {
    const problem = validate();
    if (problem) {
      setError(problem);
      return;
    }

    setError("");
    setSubmitting(true);

    // Open the tab synchronously so the browser doesn't treat it as a pop-up.
    const tab = window.open("", "_blank");

    try {
      const qty = Number(form.quantity);
      let pdf;
      let message;

      if (mode === "restock") {
        pdf = await restockWithStickers(
          {
            productId: product.id,
            quantity: qty,
            reorderLevel: form.reorderLevel === "" ? undefined : Number(form.reorderLevel),
          },
          layout
        );
        message = `Restocked ${qty} units. ${qty} stickers ready to print.`;
      } else if (mode === "print") {
        pdf = await printProductStickers(product.id, qty, layout);
        message = `${qty} stickers ready to print.`;
      } else {
        pdf = await reprintBatch(form.batchId.trim(), layout);
        message = "Stickers ready to print.";
      }

      const url = URL.createObjectURL(pdf.blob);
      if (tab) tab.location.href = url;

      setResult({ url, batchId: pdf.batchId || form.batchId.trim() || null, message });
      if (mode === "restock") onSuccess?.();
    } catch (e) {
      if (tab) tab.close();
      setError(await getErrorDetail(e, "Failed to generate stickers."));
    } finally {
      setSubmitting(false);
    }
  }

  function download() {
    const a = document.createElement("a");
    a.href = result.url;
    a.download = `stickers-${result.batchId || "batch"}.pdf`;
    document.body.appendChild(a);
    a.click();
    a.remove();
  }

  return (
    <div style={modalOverlay}>
      <div style={modalBox}>
        <h3 style={{ ...modalTitle, marginBottom: 4 }}>{TITLES[mode]}</h3>

        {product && (
          <p style={{ marginTop: 0, marginBottom: 16, color: "var(--text-muted)", fontSize: 14 }}>
            Product: <b style={{ color: "var(--text)" }}>{product.name}</b>
          </p>
        )}
        {mode === "print" && !result && (
          <p style={{ marginTop: -8, marginBottom: 16, color: "var(--text-muted)", fontSize: 13 }}>
            For stock already on the shelf. Inventory quantity will not change.
          </p>
        )}

        {result ? (
          <div>
            <div style={{ ...alertSuccess, display: "flex", gap: 10, alignItems: "flex-start" }}>
              <FiCheckCircle size={18} style={{ flexShrink: 0, marginTop: 1 }} />
              <div>
                <div>{result.message}</div>
                {result.batchId && (
                  <div style={{ marginTop: 4, fontSize: 13, fontWeight: 400 }}>
                    Batch ID:{" "}
                    <code style={{ fontFamily: "ui-monospace, SFMono-Regular, Menlo, monospace", userSelect: "all" }}>
                      {result.batchId}
                    </code>
                  </div>
                )}
              </div>
            </div>

            <div style={modalFooter}>
              <button onClick={onClose} style={secondaryBtn}>
                Close
              </button>
              <button onClick={download} style={secondaryBtn}>
                <FiDownload size={15} /> Download
              </button>
              <button onClick={() => window.open(result.url, "_blank")} style={primaryBtn}>
                <FiExternalLink size={15} /> Open PDF
              </button>
            </div>
          </div>
        ) : (
          <div>
            {error && <div style={{ ...alertError, marginBottom: 14 }}>{error}</div>}

            {mode === "reprint" ? (
              <div>
                <label style={labelStyle}>Batch ID</label>
                <input
                  value={form.batchId}
                  onChange={(e) => setForm({ ...form, batchId: e.target.value })}
                  placeholder="e.g. 3f2b8c1e-..."
                  style={inputStyle}
                  autoFocus
                />
              </div>
            ) : (
              <div>
                <label style={labelStyle}>Quantity</label>
                <input
                  value={form.quantity}
                  onChange={(e) => setForm({ ...form, quantity: e.target.value })}
                  placeholder="1 – 5000"
                  type="number"
                  min={1}
                  max={5000}
                  step={1}
                  style={inputStyle}
                  autoFocus
                />
              </div>
            )}

            {mode === "restock" && (
              <div style={{ marginTop: 12 }}>
                <label style={labelStyle}>Reorder level (optional)</label>
                <input
                  value={form.reorderLevel}
                  onChange={(e) => setForm({ ...form, reorderLevel: e.target.value })}
                  placeholder="Reorder Level"
                  type="number"
                  min={0}
                  style={inputStyle}
                />
              </div>
            )}

            <div style={{ marginTop: 14 }}>
              <label style={labelStyle}>Sticker layout</label>
              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 8 }}>
                {STICKER_LAYOUTS.map((value) => {
                  const active = layout === value;
                  return (
                    <button
                      key={value}
                      type="button"
                      onClick={() => chooseLayout(value)}
                      style={{
                        textAlign: "left",
                        padding: "10px 12px",
                        borderRadius: 10,
                        border: `1px solid ${active ? "var(--brand)" : "var(--border)"}`,
                        background: active ? "var(--brand-soft)" : "#fff",
                        boxShadow: active ? "0 0 0 3px var(--brand-ring)" : "none",
                      }}
                    >
                      <div style={{ fontWeight: 700, fontSize: 14, color: "var(--text)" }}>{value}</div>
                      <div style={{ fontSize: 12, color: "var(--text-muted)", marginTop: 2 }}>{LAYOUT_HINTS[value]}</div>
                    </button>
                  );
                })}
              </div>
            </div>

            <div style={modalFooter}>
              <button onClick={onClose} style={secondaryBtn} disabled={submitting}>
                Cancel
              </button>
              <button onClick={handleSubmit} style={primaryBtn} disabled={submitting}>
                {submitting && <Spinner />}
                {mode === "restock" ? "Restock & Print" : mode === "print" ? "Print Stickers" : "Reprint"}
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
