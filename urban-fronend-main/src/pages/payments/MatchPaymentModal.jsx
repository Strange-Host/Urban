import React, { useEffect, useState } from "react";
import { FiX } from "react-icons/fi";
import { matchPaymentToOrder, searchOrdersForPayment } from "../../api/paymentsApi";
import { inputStyle, modalOverlay, modalTitle, secondaryBtn as cancelBtn } from "../../components/styles";
import { modalBoxSmall, xBtn, cashPaymentBtn } from "../orders/orderStyles";
import { formatMoney, niceStatus } from "../orders/orderUtils";
import { orderBalance } from "../orders/paymentEntry";
import { getErrorDetail } from "../../utils/errors";
import { matchProblem } from "../../utils/paymentMatching";

/** Attach a payment that arrived without a usable account number to the right order. */
export default function MatchPaymentModal({ payment, onClose, onMatched }) {
  const [query, setQuery] = useState(payment.payerName || payment.payerPhone || "");
  const [orders, setOrders] = useState([]);
  const [searching, setSearching] = useState(false);
  const [selectedId, setSelectedId] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    const timer = setTimeout(async () => {
      try {
        setSearching(true);
        const found = await searchOrdersForPayment(query.trim());
        if (active) setOrders(found);
      } catch (e) {
        if (active) setError(getErrorDetail(e, "Could not search orders."));
      } finally {
        if (active) setSearching(false);
      }
    }, 300);

    return () => {
      active = false;
      clearTimeout(timer);
    };
  }, [query]);

  const selected = orders.find((o) => o.id === selectedId);
  const problem = selected ? matchProblem(payment, selected) : "";

  async function confirm() {
    if (!selected || problem) return;

    try {
      setSaving(true);
      setError("");
      await matchPaymentToOrder(payment.id, selected.id);
      onMatched();
    } catch (e) {
      setError(getErrorDetail(e, "Could not match the payment."));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="orders-modal-overlay" style={modalOverlay} onClick={onClose}>
      <div className="orders-modal-small" style={{ ...modalBoxSmall, maxWidth: 560 }} onClick={(e) => e.stopPropagation()}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 12 }}>
          <h3 style={{ ...modalTitle, margin: 0 }}>Match payment to an order</h3>
          <button onClick={onClose} style={xBtn} disabled={saving}><FiX size={16} /></button>
        </div>

        <div style={{ marginBottom: 12, color: "#374151" }}>
          <b>{formatMoney(payment.amount)}</b> from <b>{payment.payerName || payment.payerPhone || "unknown payer"}</b>
          {payment.transactionRef ? <> · ref <b>{payment.transactionRef}</b></> : null}
          {payment.notes ? <div style={{ color: "#6b7280", marginTop: 4 }}>{payment.notes}</div> : null}
        </div>

        <input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Search by account number, customer or phone"
          style={inputStyle}
          disabled={saving}
        />

        <div style={{ marginTop: 10, maxHeight: 260, overflowY: "auto", display: "grid", gap: 6 }}>
          {searching && <div style={{ color: "#6b7280" }}>Searching...</div>}
          {!searching && orders.length === 0 && <div style={{ color: "#6b7280" }}>No orders found.</div>}
          {orders.map((o) => (
            <label
              key={o.id}
              style={{
                display: "flex",
                gap: 10,
                alignItems: "flex-start",
                padding: 10,
                borderRadius: 10,
                border: `1px solid ${selectedId === o.id ? "#4f46e5" : "#e5e7eb"}`,
                cursor: "pointer",
              }}
            >
              <input
                type="radio"
                name="match-order"
                checked={selectedId === o.id}
                onChange={() => setSelectedId(o.id)}
              />
              <div style={{ minWidth: 0 }}>
                <div style={{ fontWeight: 700 }}>
                  {o.accountNumber || String(o.id).slice(0, 8).toUpperCase()} · {o.customerName || "-"}
                </div>
                <div style={{ color: "#6b7280", fontSize: 13 }}>
                  {niceStatus(o.orderStatus)} · total {formatMoney(o.totalAmount)} · balance {formatMoney(orderBalance(o))}
                </div>
              </div>
            </label>
          ))}
        </div>

        {selected && problem && (
          <div style={{ marginTop: 10, color: "crimson", fontWeight: 700 }}>{problem}</div>
        )}
        {error && <div style={{ marginTop: 10, color: "crimson", fontWeight: 700 }}>{error}</div>}

        <div style={{ display: "flex", justifyContent: "flex-end", gap: 10, marginTop: 16 }}>
          <button onClick={onClose} style={cancelBtn} disabled={saving}>
            Cancel
          </button>
          <button onClick={confirm} style={cashPaymentBtn} disabled={saving || !selected || !!problem}>
            {saving ? "Matching..." : "Match payment"}
          </button>
        </div>
      </div>
    </div>
  );
}
