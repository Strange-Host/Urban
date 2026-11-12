import React from "react";
import { FiX } from "react-icons/fi";
import {
  inputStyle,
  labelStyle,
  modalOverlay,
  modalTitle,
  secondaryBtn as cancelBtn,
} from "../../components/styles";
import {
  MPESA_PAYBILL,
  formatDateTime,
  formatMoney,
  getOrderProductLines,
  niceStatus,
} from "./orderUtils";
import {
  addItemBtn,
  cashPaymentBtn,
  createBtn,
  modalBoxSmall,
  modalBoxWide,
  removeBtn,
  saveBtn,
  sectionTitle,
  xBtn,
} from "./orderStyles";

export function AddOrderModal({
  customerForm,
  orderForm,
  itemForm,
  cartItems,
  cartTotal,
  products,
  loading,
  error,
  onCustomerChange,
  onOrderChange,
  onItemChange,
  onOrderTypeChange,
  onAddItem,
  onRemoveItem,
  onClose,
  onCreate,
}) {
  return (
    <div className="orders-modal-overlay" style={modalOverlay} onClick={onClose}>
      <div className="orders-modal" style={modalBoxWide} onClick={(e) => e.stopPropagation()}>
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            marginBottom: 12,
          }}
        >
          <h3 style={{ ...modalTitle, margin: 0 }}>Create Order</h3>

          <button onClick={onClose} style={xBtn}><FiX size={16} /></button>
        </div>

        {/* Customer */}
        <div style={sectionTitle}>Customer</div>
        <div
          className="responsive-grid-2"
          style={{
            display: "grid",
            gridTemplateColumns: "1fr 1fr",
            gap: 12,
          }}
        >
          <input
            name="name"
            value={customerForm.name}
            onChange={onCustomerChange}
            placeholder="Customer Name"
            style={inputStyle}
          />
          <input
            name="phone"
            value={customerForm.phone}
            onChange={onCustomerChange}
            placeholder="Phone Number"
            style={inputStyle}
          />
          <input
            name="email"
            value={customerForm.email}
            onChange={onCustomerChange}
            placeholder="Email (optional)"
            style={inputStyle}
          />
          <input
            name="city_town"
            value={customerForm.city_town}
            onChange={onCustomerChange}
            placeholder="City / Town"
            style={inputStyle}
          />
        </div>

        <div style={{ marginBottom: 12, marginTop: 12 }}>
          <label style={labelStyle}>
            Order Type
          </label>

          <select
            value={orderForm.orderType}
            onChange={(e) => onOrderTypeChange(e.target.value)}
            style={inputStyle}
          >
            <option value="COUNTER">COUNTER (Walk-in)</option>
            <option value="ONLINE">ONLINE (Delivery)</option>
          </select>
        </div>

        {orderForm.orderType === "ONLINE" && (
          <>
            <div style={{ ...sectionTitle, marginTop: 16 }}>
              Delivery
            </div>

            <div
              className="responsive-grid-2"
              style={{
                display: "grid",
                gridTemplateColumns: "1fr 1fr",
                gap: 12,
              }}
            >
              <input
                name="deliveryCity"
                value={orderForm.deliveryCity}
                onChange={onOrderChange}
                placeholder="Delivery City"
                style={inputStyle}
              />
              <input
                name="deliveryNotes"
                value={orderForm.deliveryNotes}
                onChange={onOrderChange}
                placeholder="Delivery Notes"
                style={inputStyle}
              />
            </div>
          </>
        )}

        <div style={{ marginTop: 12, marginBottom: 12 }}>
          <label style={labelStyle}>
            Discount (KES)
          </label>
          <input
            name="discount"
            value={orderForm.discount}
            onChange={onOrderChange}
            placeholder="0"
            type="number"
            min="0"
            style={inputStyle}
          />
        </div>

        {/* Items */}
        <div style={{ ...sectionTitle, marginTop: 16 }}>
          Items
        </div>

        <div
          className="responsive-grid-3"
          style={{
            display: "grid",
            gridTemplateColumns: "2fr 1fr 1fr",
            gap: 12,
            alignItems: "center",
          }}
        >
          <select
            name="productId"
            value={itemForm.productId}
            onChange={onItemChange}
            style={inputStyle}
          >
            {products.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name} ({p.sku})
              </option>
            ))}
          </select>

          <input
            name="quantity"
            value={itemForm.quantity}
            onChange={onItemChange}
            placeholder="Qty"
            type="number"
            style={inputStyle}
          />

          <button onClick={onAddItem} style={addItemBtn}>
            + Add
          </button>
        </div>

        {/* Cart */}
        {cartItems.length > 0 && (
          <div style={{ marginTop: 12 }}>
            {cartItems.map((i) => (
              <div
                key={i.productId}
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                  padding: "10px 12px",
                  border: "1px solid #e5e7eb",
                  borderRadius: 12,
                  marginBottom: 8,
                  background: "#f9fafb",
                  gap: 10,
                }}
              >
                <div style={{ minWidth: 0 }}>
                  <div style={{ fontWeight: 700, wordBreak: "break-word" }}>
                    {i.productName}
                  </div>
                  <div style={{ color: "#6b7280", fontSize: 13 }}>
                    {i.quantity} × {formatMoney(i.sellingPrice)}
                  </div>
                </div>

                <div style={{ display: "flex", gap: 10, alignItems: "center" }}>
                  <div style={{ fontWeight: 700 }}>
                    {formatMoney(Number(i.sellingPrice) * Number(i.quantity))}
                  </div>

                  <button onClick={() => onRemoveItem(i.productId)} style={removeBtn}><FiX size={16} /></button>
                </div>
              </div>
            ))}

            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                marginTop: 10,
                fontWeight: 700,
              }}
            >
              <div>Total</div>
              <div>{formatMoney(cartTotal)}</div>
            </div>

            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                marginTop: 8,
                color: "#6b7280",
              }}
            >
              <div>Discount</div>
              <div>{formatMoney(Number(orderForm.discount || 0))}</div>
            </div>

            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                marginTop: 10,
                fontWeight: 700,
              }}
            >
              <div>Grand Total</div>
              <div>{formatMoney(Math.max(0, cartTotal - Number(orderForm.discount || 0)))}</div>
            </div>
          </div>
        )}

        {/* Footer */}
        <div className="orders-modal-footer" style={{ display: "flex", justifyContent: "flex-end", marginTop: 16 }}>
          <button className="orders-create-button" onClick={onCreate} style={createBtn}>
            {loading ? "Saving..." : "Create Order"}
          </button>
        </div>

        {error && (
          <div style={{ marginTop: 10, color: "crimson", fontWeight: 700 }}>
            {error}
          </div>
        )}
      </div>
    </div>
  );
}

export function EditStatusModal({
  order,
  statuses,
  status,
  loading,
  error,
  onStatusChange,
  onClose,
  onSave,
}) {
  return (
    <div className="orders-modal-overlay" style={modalOverlay} onClick={onClose}>
      <div className="orders-modal-small" style={modalBoxSmall} onClick={(e) => e.stopPropagation()}>
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            marginBottom: 12,
          }}
        >
          <h3 style={{ ...modalTitle, margin: 0 }}>Edit Order Status</h3>

          <button onClick={onClose} style={xBtn}><FiX size={16} /></button>
        </div>

        <div style={{ marginBottom: 10, color: "#6b7280" }}>
          Order: <b>{String(order.id).slice(0, 8).toUpperCase()}</b>
        </div>

        <select
          value={status}
          onChange={(e) => onStatusChange(e.target.value)}
          style={inputStyle}
        >
          {statuses.map((s) => (
            <option key={s} value={s}>
              {niceStatus(s)}
            </option>
          ))}
        </select>

        <div
          style={{
            display: "flex",
            justifyContent: "flex-end",
            gap: 10,
            marginTop: 16,
          }}
        >
          <button onClick={onClose} style={cancelBtn}>
            Cancel
          </button>

          <button onClick={onSave} style={saveBtn}>
            {loading ? "Saving..." : "Update"}
          </button>
        </div>

        {error && (
          <div style={{ marginTop: 10, color: "crimson", fontWeight: 700 }}>
            {error}
          </div>
        )}
      </div>
    </div>
  );
}

export function CashPaymentModal({
  order,
  amount,
  error,
  saving,
  onAmountChange,
  onClose,
  onConfirm,
}) {
  return (
    <div className="orders-modal-overlay" style={modalOverlay} onClick={onClose}>
      <div className="orders-modal-small" style={modalBoxSmall} onClick={(e) => e.stopPropagation()}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 12 }}>
          <h3 style={{ ...modalTitle, margin: 0 }}>Record Cash Payment</h3>
          <button onClick={onClose} style={xBtn} disabled={saving}><FiX size={16} /></button>
        </div>

        <div style={{ marginBottom: 10, color: "#6b7280" }}>
          Order: <b>{String(order.id).slice(0, 8).toUpperCase()}</b>
        </div>
        <div style={{ marginBottom: 14, color: "#374151" }}>
          Customer: <b>{order.customerName || "-"}</b>
          <br />
          Order total: <b>{formatMoney(order.totalAmount)}</b>
          <br />
          Payment method: <b>Cash</b>
        </div>

        <label style={{ display: "block", marginBottom: 6, fontWeight: 700 }}>Amount received (KES)</label>
        <input
          type="number"
          min="0.01"
          step="0.01"
          value={amount}
          onChange={(e) => onAmountChange(e.target.value)}
          style={inputStyle}
          disabled={saving}
        />

        <div style={{ display: "flex", justifyContent: "flex-end", gap: 10, marginTop: 16 }}>
          <button onClick={onClose} style={cancelBtn} disabled={saving}>
            Cancel
          </button>
          <button onClick={onConfirm} style={cashPaymentBtn} disabled={saving}>
            {saving ? "Recording..." : "Confirm Cash Payment"}
          </button>
        </div>

        {error && (
          <div style={{ marginTop: 10, color: "crimson", fontWeight: 700 }}>
            {error}
          </div>
        )}
      </div>
    </div>
  );
}

export function OrderNotesModal({
  order,
  text,
  error,
  loading,
  onChange,
  onClose,
  onSave,
}) {
  return (
    <div className="orders-modal-overlay" style={modalOverlay} onClick={onClose}>
      <div className="orders-modal-small" style={modalBoxSmall} onClick={(e) => e.stopPropagation()}>
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            marginBottom: 12,
          }}
        >
          <h3 style={{ ...modalTitle, margin: 0 }}>Delivery Notes</h3>
          <button onClick={onClose} style={xBtn}><FiX size={16} /></button>
        </div>

        <div style={{ marginBottom: 10, color: "#6b7280" }}>
          Order: <b>{String(order.id).slice(0, 8).toUpperCase()}</b>
        </div>

        <textarea
          value={text}
          onChange={(e) => onChange(e.target.value)}
          placeholder="Enter delivery notes, customer comments, or pickup instructions"
          style={{
            width: "100%",
            minHeight: 140,
            borderRadius: 12,
            border: "1px solid #e5e7eb",
            padding: 12,
            fontFamily: "inherit",
            resize: "vertical",
          }}
        />

        <div
          style={{
            display: "flex",
            justifyContent: "flex-end",
            gap: 10,
            marginTop: 16,
          }}
        >
          <button onClick={onClose} style={cancelBtn}>
            Cancel
          </button>

          <button onClick={onSave} style={saveBtn}>
            {loading ? "Saving..." : "Save Notes"}
          </button>
        </div>

        {error && (
          <div style={{ marginTop: 10, color: "crimson", fontWeight: 700 }}>
            {error}
          </div>
        )}
      </div>
    </div>
  );
}

export function ReceiptModal({ order, fallbackServedBy }) {
  const productLines = getOrderProductLines(order);

  return (
    <div id="receipt-print-root" className="print-receipt" aria-hidden="true">
      <div className="receipt-paper">
        <div className="receipt-header">
          <h1>URBAN TRENDS</h1>
          <div className="receipt-title">Dispatch Receipt</div>
        </div>

        <div className="receipt-section">
          <div className="receipt-row">
            <strong>Order</strong>
            <strong>{String(order.id || "").slice(0, 8).toUpperCase()}</strong>
          </div>
          <div className="receipt-row">
            <span>Date</span>
            <span>{formatDateTime(order.createdAt)}</span>
          </div>
          <div className="receipt-row">
            <span>Status</span>
            <span>{niceStatus(order.orderStatus)}</span>
          </div>
          <div className="receipt-row">
            <span>Served by</span>
            <span>{order.servedBy || fallbackServedBy}</span>
          </div>
        </div>

        <div className="receipt-section">
          <div className="receipt-row">
            <span>Customer</span>
            <strong>{order.customerName || "-"}</strong>
          </div>
          <div className="receipt-row">
            <span>Phone</span>
            <strong>{order.phoneNumber || "-"}</strong>
          </div>
          <div className="receipt-row">
            <span>City</span>
            <strong>{order.deliveryCity || "-"}</strong>
          </div>
        </div>

        <div className="receipt-section">
          <div className="receipt-row">
            <span>{productLines.length > 1 ? "Products" : "Product"}</span>
            <strong>
              {productLines.map((productLine, productIndex) => (
                <div key={`${order.id}-receipt-product-${productIndex}`}>{productLine}</div>
              ))}
            </strong>
          </div>
          <div className="receipt-row receipt-total">
            <span>Amount Due</span>
            <span>{formatMoney(order.totalAmount)}</span>
          </div>
        </div>

        <div className="receipt-payment">
          <div className="receipt-row">
            <span>M-Pesa Paybill</span>
            <strong>{MPESA_PAYBILL}</strong>
          </div>
          <div className="receipt-row">
            <span>Account No</span>
            <strong>{order.accountNumber || "Use order number"}</strong>
          </div>
        </div>
      </div>
    </div>
  );
}
