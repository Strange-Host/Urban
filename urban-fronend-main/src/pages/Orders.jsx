import React, { useEffect, useMemo, useState } from "react";
import { api } from "../api/api";
import { loadProducts as fetchProductCatalog, loadStaff as fetchStaffList } from "../api/referenceData";
import { useAuth } from "../auth/useAuth";
import { loadMpesaPaybill } from "../utils/settings";
import { getErrorDetail } from "../utils/errors";
import {
  emptyPaymentLine,
  normalizeCounterPayments,
  validateCounterPayments,
} from "./orders/counterPayments";
import { printReceiptToUSB } from "../utils/printer";
import {
  FiPlus,
  FiX,
  FiEdit2,
  FiFileText,
  FiDollarSign,
  FiMapPin,
  FiCalendar,
  FiShoppingBag,
  FiChevronLeft,
  FiChevronRight,
} from "react-icons/fi";
import { PageHeader, EmptyState, LoadingBlock } from "../components/ui";
import {
  inputStyle,
  secondaryBtn as cancelBtn,
  pageBtnStyle,
  tableCard,
  alertError,
} from "../components/styles";
import {
  AddOrderModal,
  CashPaymentModal,
  EditStatusModal,
  OrderNotesModal,
  ReceiptModal,
} from "./orders/OrderModals";
import { addOrderBtn, editBtn, noteBtn, cashPaymentBtn } from "./orders/orderStyles";
import {
  ORDER_STATUSES,
  formatDateTime,
  formatMoney,
  getOrderProductLines,
  getParcelAgeHighlight,
  niceStatus,
  previewText,
  statusStyle,
} from "./orders/orderUtils";
import "./orders/orders.css";

export default function Orders() {
  const { user, can } = useAuth();
  const servedByName = user?.name || user?.email || "-";
  const [servedBy, setServedBy] = useState(
    user?.phoneNumber ? `${servedByName} · ${user.phoneNumber}` : servedByName
  );

  // Backend state
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  // Pageable orders
  const [page, setPage] = useState(0);
  const size = 10;
  const [pageData, setPageData] = useState(null);
  const [orders, setOrders] = useState([]);

  // Products for dropdown
  const [products, setProducts] = useState([]);

  // filter state
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("");

  // Create order modal
  const [isAddOpen, setIsAddOpen] = useState(false);

  // Edit status modal
  const [editingOrder, setEditingOrder] = useState(null);
  const [editStatus, setEditStatus] = useState("AWAITING_DISPATCH");
  const [cashPaymentOrder, setCashPaymentOrder] = useState(null);
  const [cashPaymentAmount, setCashPaymentAmount] = useState("");
  const [cashPaymentError, setCashPaymentError] = useState("");
  const [recordingCashPayment, setRecordingCashPayment] = useState(false);
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [noteText, setNoteText] = useState("");
  const [noteError, setNoteError] = useState("");
  const [receiptOrder, setReceiptOrder] = useState(null);
  const [printingReceipt, setPrintingReceipt] = useState(false);

  // Quick Order form
  const [customerForm, setCustomerForm] = useState({
    name: "",
    phone: "",
    email: "",
    city_town: "",
  });

  const [orderForm, setOrderForm] = useState({
    orderType: "ONLINE",
    deliveryCity: "",
    deliveryNotes: "",
    assignedTo: "",
    discount: 0,
  });

  // Counter sales are paid on the spot; lines allow a split such as cash + M-Pesa.
  const [paymentLines, setPaymentLines] = useState([emptyPaymentLine()]);

  const [itemForm, setItemForm] = useState({
    productId: "",
    quantity: 1,
  });

  const [cartItems, setCartItems] = useState([]);

  async function resolveServedBy() {
    let staff = null;

    if (user?.id) {
      try {
        const res = await api.get(`/api/v1/auth/user/${user.id}`);
        staff = res.data?.data || res.data;
      } catch (e) {
        console.warn("Could not load served-by staff by ID", e);
      }
    }

    if (!staff && user?.email && (user.role === "ADMIN" || user.role === "MANAGER")) {
      try {
        const staffList = await fetchStaffList();
        staff = staffList.find(
          (person) => String(person.email || "").toLowerCase() === user.email.toLowerCase()
        );
      } catch (e) {
        console.warn("Could not find served-by staff by email", e);
      }
    }

    const staffName = staff
      ? [staff.firstName, staff.lastName].filter(Boolean).join(" ") || staff.name || servedByName
      : servedByName;
    const phoneNumber = staff?.phoneNumber || staff?.phone || staff?.mobileNumber || staff?.mobile || user?.phoneNumber;

    return phoneNumber ? `${staffName} · ${phoneNumber}` : staffName;
  }

  useEffect(() => {
    let isMounted = true;
    const updateServedBy = async () => {
      const resolvedServedBy = await resolveServedBy();
      if (isMounted) {
        setServedBy(resolvedServedBy);
      }
    };

    updateServedBy();
    return () => {
      isMounted = false;
    };
  }, [user, servedByName]);

  useEffect(() => {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => {
      loadOrders(page, statusFilter, search.trim(), controller.signal);
    }, search.trim() ? 300 : 0);

    return () => {
      clearTimeout(timeoutId);
      controller.abort();
    };
    // eslint-disable-next-line
  }, [page, statusFilter, search]);

  useEffect(() => {
    loadProducts();
    // eslint-disable-next-line
  }, []);

  useEffect(() => {
    if (!receiptOrder) return undefined;

    let isMounted = true;

    const printReceipt = async () => {
      try {
        setPrintingReceipt(true);
        await printReceiptToUSB(
          { ...receiptOrder, servedBy: receiptOrder.servedBy || servedBy },
          await loadMpesaPaybill()
        );
      } catch (e) {
        console.error("Receipt print failed:", e);
      } finally {
        if (isMounted) {
          setPrintingReceipt(false);
          setReceiptOrder(null);
        }
      }
    };

    printReceipt();

    return () => {
      isMounted = false;
    };
  }, [receiptOrder]);

  async function loadOrders(requestPage = page, status = statusFilter, query = "", signal) {
    try {
      setLoading(true);
      setError("");

      const res = await api.get("/api/v1/orders", {
        params: {
          page: requestPage,
          size,
          sort: "createdAt,desc",
          status: status || undefined,
          query: query || undefined,
        },
        signal,
      });

      if (signal?.aborted) return;
      setPageData(res.data);
      setOrders(res.data.content || []);
    } catch (e) {
      if (signal?.aborted || e.code === "ERR_CANCELED") return;
      console.log(e);
      setError("Failed to load orders. Check backend, token, and CORS.");
    } finally {
      if (!signal?.aborted) setLoading(false);
    }
  }

  async function loadProducts() {
    try {
      const list = await fetchProductCatalog();

      setProducts(list);

      if (list.length > 0) {
        setItemForm((prev) => ({ ...prev, productId: list[0].id }));
      }
    } catch (e) {
      console.log(e);
    }
  }

  function openAddModal() {
    setCustomerForm({ name: "", phone: "", email: "", city_town: "" });
    setOrderForm({
      orderType: "ONLINE",
      deliveryCity: "",
      deliveryNotes: "",
      assignedTo: "",
      discount: 0,
    });

    setCartItems([]);
    setPaymentLines([emptyPaymentLine()]);
    setError("");
    setIsAddOpen(true);
  }

  function closeAddModal() {
    setIsAddOpen(false);
  }

  function openEditModal(order) {
    setEditingOrder(order);
    setEditStatus(order.orderStatus || "AWAITING_DISPATCH");
    setError("");
  }

  function closeEditModal() {
    setEditingOrder(null);
  }

  function openCashPaymentModal(order) {
    setCashPaymentOrder(order);
    setCashPaymentAmount(Number(order.totalAmount || 0).toFixed(2));
    setCashPaymentError("");
  }

  function closeCashPaymentModal() {
    if (recordingCashPayment) return;
    setCashPaymentOrder(null);
    setCashPaymentAmount("");
    setCashPaymentError("");
  }

  function handleCustomerChange(e) {
    setCustomerForm({ ...customerForm, [e.target.name]: e.target.value });
  }

  function handleOrderChange(e) {
    setOrderForm({ ...orderForm, [e.target.name]: e.target.value });
  }

  function handleItemChange(e) {
    setItemForm({ ...itemForm, [e.target.name]: e.target.value });
  }

  function addItemToCart() {
    if (!itemForm.productId) {
      alert("Select a product");
      return;
    }

    const qty = Number(itemForm.quantity);
    if (!qty || qty <= 0) {
      alert("Enter valid quantity");
      return;
    }

    const product = products.find((p) => p.id === itemForm.productId);
    if (!product) {
      alert("Invalid product");
      return;
    }

    const exists = cartItems.find((i) => i.productId === itemForm.productId);
    if (exists) {
      setCartItems(
        cartItems.map((i) =>
          i.productId === itemForm.productId
            ? { ...i, quantity: Number(i.quantity) + qty }
            : i
        )
      );
      return;
    }

    setCartItems([
      ...cartItems,
      {
        productId: itemForm.productId,
        productName: product.name,
        sellingPrice: product.sellingPrice,
        quantity: qty,
      },
    ]);
  }

  function removeCartItem(productId) {
    setCartItems(cartItems.filter((i) => i.productId !== productId));
  }

  const cartTotal = useMemo(() => {
    return cartItems.reduce((sum, i) => {
      return sum + Number(i.sellingPrice || 0) * Number(i.quantity || 0);
    }, 0);
  }, [cartItems]);

  async function handleCreateOrder() {
    if (!customerForm.name) {
      alert("Customer name is required.");
      return;
    }

    // phone required only for ONLINE orders
    if (orderForm.orderType === "ONLINE" && !customerForm.phone) {
      alert("Customer phone is required for ONLINE orders.");
      return;
    }

    // delivery city required only for ONLINE orders
    if (orderForm.orderType === "ONLINE" && !orderForm.deliveryCity) {
      alert("Delivery city is required for ONLINE orders.");
      return;
    }

    const discountAmount = Number(orderForm.discount || 0);
    if (cartItems.length === 0) {
      alert("Add at least one product to the order.");
      return;
    }

    if (discountAmount < 0) {
      alert("Discount cannot be negative.");
      return;
    }

    const cartTotal = cartItems.reduce((sum, i) => {
      return sum + Number(i.sellingPrice || 0) * Number(i.quantity || 0);
    }, 0);

    if (discountAmount > cartTotal) {
      alert("Discount cannot exceed the cart total.");
      return;
    }

    const grandTotal = Math.max(0, cartTotal - discountAmount);

    if (orderForm.orderType === "COUNTER") {
      const paymentError = validateCounterPayments(paymentLines, grandTotal);
      if (paymentError) {
        setError(paymentError);
        return;
      }
    }

    try {
      setLoading(true);
      setError("");

      // 1) Create customer first
      const customerPayload = {
        name: customerForm.name,
        phone: customerForm.phone || null,
        email: customerForm.email || null,
        city_town:
          customerForm.city_town ||
          (orderForm.orderType === "ONLINE" ? orderForm.deliveryCity : null),
      };

      const customerRes = await api.post("/api/v1/customers", customerPayload);

      const customerId = customerRes.data.id || customerRes.data.customerId;
      if (!customerId)
        throw new Error("Customer created but no customerId returned.");

      // 2) Create order
      const orderPayload = {
        customerId,
        orderType: orderForm.orderType || "ONLINE",
        assignedTo: orderForm.assignedTo || null,
        discount: Number(orderForm.discount || 0),

        deliveryCity:
          orderForm.orderType === "ONLINE" ? orderForm.deliveryCity : null,

        deliveryNotes:
          orderForm.orderType === "ONLINE"
            ? orderForm.deliveryNotes || null
            : null,

        items: cartItems.map((i) => ({
          productId: i.productId,
          quantity: i.quantity,
        })),

        ...(orderForm.orderType === "COUNTER"
          ? { payments: normalizeCounterPayments(paymentLines, grandTotal) }
          : {}),
      };

      await api.post("/api/v1/orders", orderPayload);

      closeAddModal();
      setPage(0);
      await loadOrders(0, statusFilter, search.trim());

      alert("Order created successfully!");
    } catch (e) {
      console.log(e);
      setError(getErrorDetail(e, "Failed to create order. Check backend + customer endpoint."));
      alert("Failed to create order. Check console.");
    } finally {
      setLoading(false);
    }
  }

  async function handleUpdateStatus() {
    if (!editingOrder?.id) return;

    try {
      setLoading(true);
      setError("");

      await api.patch(`/api/v1/orders/${editingOrder.id}/status`, null, {
        params: { status: editStatus },
      });

      closeEditModal();
      await loadOrders(page, statusFilter, search.trim());

      if (editStatus === "DISPATCHED_UNPAID") {
        const resolvedServedBy = await resolveServedBy();
        setServedBy(resolvedServedBy);
        setReceiptOrder({
          ...editingOrder,
          orderStatus: editStatus,
          servedBy: resolvedServedBy,
        });
      }

      alert("Order status updated!");
    } catch (e) {
      console.log(e);
      setError("Failed to update status.");
      alert("Failed to update status. check your inventory");
    } finally {
      setLoading(false);
    }
  }

  async function handleRecordCashPayment() {
    if (!cashPaymentOrder?.id) return;

    const amount = Number(cashPaymentAmount);
    const orderTotal = Number(cashPaymentOrder.totalAmount || 0);
    if (!Number.isFinite(amount) || amount <= 0) {
      setCashPaymentError("Enter a payment amount greater than zero.");
      return;
    }
    if (orderTotal > 0 && amount > orderTotal) {
      setCashPaymentError("Payment amount cannot exceed the order total.");
      return;
    }

    try {
      setRecordingCashPayment(true);
      setCashPaymentError("");

      await api.post("/api/v1/payments", {
        orderId: cashPaymentOrder.id,
        method: "CASH",
        amount,
        payerName: cashPaymentOrder.customerName || null,
        payerPhone: cashPaymentOrder.phoneNumber || null,
      });

      setCashPaymentOrder(null);
      setCashPaymentAmount("");
      await loadOrders(page, statusFilter, search.trim());
      alert("Cash payment recorded successfully.");
    } catch (e) {
      console.error("Could not record cash payment:", e);
      setCashPaymentError(
        e.response?.data?.message || "Failed to record cash payment. Check your payment permission and backend."
      );
    } finally {
      setRecordingCashPayment(false);
    }
  }

  function openNotesModal(order) {
    setSelectedOrder(order);
    setNoteText(order.deliveryNotes || "");
    setNoteError("");
  }

  function closeNotesModal() {
    setSelectedOrder(null);
    setNoteText("");
    setNoteError("");
  }

  async function handleUpdateNotes() {
    if (!selectedOrder?.id) return;

    try {
      setLoading(true);
      setNoteError("");

      await api.patch(`/api/v1/orders/${selectedOrder.id}/notes`, {
        deliveryNotes: noteText || null,
      });

      closeNotesModal();
      await loadOrders(page, statusFilter, search.trim());

      alert("Delivery notes updated.");
    } catch (e) {
      console.log(e);
      setNoteError("Failed to update delivery notes.");
      alert("Failed to update notes. Check console.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="orders-page" style={{ width: "100%", boxSizing: "border-box" }}>

      {/* Header */}
      <PageHeader title="Orders" subtitle="Create, track and settle customer orders">
        <button onClick={openAddModal} style={addOrderBtn}>
          <FiPlus size={16} /> Add Order
        </button>
      </PageHeader>

      <div className="orders-filter" style={{ marginBottom: 16, display: "flex", gap: 10, flexWrap: "wrap" }}>
        <select
          value={statusFilter}
          onChange={(e) => {
            setPage(0);
            setStatusFilter(e.target.value);
          }}
          style={{ ...inputStyle, width: "auto", minWidth: 190, height: 42, padding: "0 12px" }}
        >
          <option value="">All Statuses</option>

          {ORDER_STATUSES.map((s) => (
            <option key={s} value={s}>
              {niceStatus(s)}
            </option>
          ))}
        </select>

        <input
          type="search"
          aria-label="Search orders"
          placeholder="Search customer, phone, product, city..."
          value={search}
          onChange={(e) => {
            setPage(0);
            setSearch(e.target.value);
          }}
          style={{
            ...inputStyle,
            flex: "1 1 260px",
            width: "auto",
            minWidth: 0,
            height: 42,
            paddingLeft: 38,
            background: "#fff url(\"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='16' height='16' fill='none' stroke='%2394a3b8' stroke-width='2' stroke-linecap='round' stroke-linejoin='round' viewBox='0 0 24 24'%3E%3Ccircle cx='11' cy='11' r='8'/%3E%3Cpath d='m21 21-4.35-4.35'/%3E%3C/svg%3E\") no-repeat 13px center",
          }}
        />

        {(statusFilter || search) && (
          <button
            onClick={() => {
              setStatusFilter("");
              setSearch("");
              setPage(0);
            }}
            style={{ ...cancelBtn, height: 42 }}
          >
            <FiX size={14} /> Clear Filters
          </button>
        )}
      </div>

      {/* Orders Table */}
      <div
        className="orders-table"
        style={tableCard}
      >
        {/* Table Header (Desktop only) */}
        <div
          className="desktop-table-header desktop-header"
          style={{
            display: "grid",
            gridTemplateColumns: "1fr 1fr 1fr 1.3fr 1fr 1.6fr 1fr 1fr 1fr 1.1fr",
            gap: 10,
            padding: "14px 16px",
            background: "var(--surface-muted)",
          }}
        >
          <div>Customer</div>
          <div>Product</div>
          <div>Phone</div>
          <div>Amount</div>
          <div>Status</div>
          <div>Notes</div>
          <div>City</div>
          <div>Account</div>
          <div>Date</div>
          <div>Action</div>
        </div>

        {loading && <LoadingBlock label="Loading orders..." />}
        {error && !isAddOpen && !editingOrder && (
          <div style={{ ...alertError, margin: 16 }}>{error}</div>
        )}

        {/* Rows */}
        {!loading &&
          orders.map((o, idx) => {
            const s = statusStyle(o.orderStatus);
            const highlight = getParcelAgeHighlight(o);

            return (
              <React.Fragment key={o.id}>
                {/* Desktop row */}
                <div
                  className="desktop-row"
                  style={{
                    display: "grid",
                    gridTemplateColumns: "1fr 1fr 1fr 1.3fr 1fr 1.6fr 1fr 1fr 1fr 1.1fr",
                    gap: 10,
                    padding: "14px 16px",
                    borderTop: idx === 0 ? "none" : "1px solid #f3f4f6",
                    alignItems: "center",
                    fontSize: 14,
                    boxShadow: highlight ? `inset 4px 0 0 ${highlight}` : undefined,
                  }}
                >
                  <div style={{ color: "var(--text)", fontWeight: 600 }}>
                    {String(o.customerName)}
                  </div>

                  <div style={{ color: "#374151" }}>
                    {getOrderProductLines(o).map((productLine, productIndex) => (
                      <div key={`${o.id}-product-${productIndex}`}>{productLine}</div>
                    ))}
                  </div>

                  <div style={{ color: "#6b7280" }}>
                    {String(o.phoneNumber)}
                  </div>

                  <div>
                    <span
                      style={{ fontWeight: 600, color: "var(--text)", whiteSpace: "nowrap" }}
                    >
                      {formatMoney(o.totalAmount)}
                    </span>
                  </div>

                  <div>
                    <span
                      style={{
                        background: s.bg,
                        color: s.color,
                        padding: "3px 10px",
                        borderRadius: 999,
                        fontWeight: 600,
                        fontSize: 12,
                        whiteSpace: "nowrap",
                      }}
                    >
                      {niceStatus(o.orderStatus)}
                    </span>
                  </div>

                  <div style={{ color: "var(--text-muted)", fontSize: 13, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>
                    {previewText(o.deliveryNotes, 40)}
                  </div>

                  <div>{o.deliveryCity || "-"}</div>

                  <div>{o.accountNumber || "-"}</div>

                  <div style={{ color: "var(--text-muted)", fontSize: 13 }}>
                    {formatDateTime(o.createdAt)}
                  </div>

                  <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
                    {can("CAN_CREATE_PAYMENTS") && ["DISPATCHED_UNPAID", "DELIVERED_UNPAID"].includes(o.orderStatus) && (
                      <button onClick={() => openCashPaymentModal(o)} style={cashPaymentBtn}>
                        <FiDollarSign size={13} /> Record Cash
                      </button>
                    )}
                    <button onClick={() => openEditModal(o)} style={editBtn}>
                      <FiEdit2 size={13} /> Edit
                    </button>
                    <button onClick={() => openNotesModal(o)} style={noteBtn}>
                      <FiFileText size={13} /> Notes
                    </button>
                  </div>
                </div>

                {/* Mobile card */}
                <div className="mobile-card" style={{ boxShadow: highlight ? `inset 4px 0 0 ${highlight}` : undefined }}>
                  <div className="mobile-card-top" style={{ display: "flex", justifyContent: "space-between", gap: 10 }}>
                    <div className="mobile-card-customer">
                      <div style={{ fontWeight: 700, fontSize: 16 }}>
                        {String(o.customerName)}
                      </div>
                      <div className="mobile-card-products" style={{ color: "#6b7280", marginTop: 2 }}>
                        {getOrderProductLines(o).map((productLine, productIndex) => (
                          <div key={`${o.id}-mobile-product-${productIndex}`}>{productLine}</div>
                        ))}
                      </div>
                    </div>

                    <div className="mobile-card-summary" style={{ textAlign: "right" }}>
                      <div style={{ fontWeight: 700 }}>{formatMoney(o.totalAmount)}</div>
                      <div style={{ marginTop: 6 }}>
                        <span
                          style={{
                            background: s.bg,
                            color: s.color,
                            padding: "3px 10px",
                            borderRadius: 999,
                            fontWeight: 600,
                            fontSize: 12,
                            whiteSpace: "nowrap",
                            display: "inline-block",
                          }}
                        >
                          {niceStatus(o.orderStatus)}
                        </span>
                      </div>
                    </div>
                  </div>

                  <div style={{ marginTop: 10, color: "#374151", fontSize: 13 }}>
                    <FiMapPin size={12} style={{ verticalAlign: -1 }} /> {o.deliveryCity || "No delivery city"}
                  </div>

                  <div style={{ color: "#6b7280" }}>
                    {String(o.accountNumber)}
                  </div>

                  <div style={{ marginTop: 6, color: "#374151", fontSize: 13 }}>
                    <strong>Notes:</strong> {previewText(o.deliveryNotes, 80)}
                  </div>

                  <div style={{ marginTop: 6, color: "#374151", fontSize: 13 }}>
                    <FiCalendar size={12} style={{ verticalAlign: -1 }} /> {formatDateTime(o.createdAt)}
                  </div>

                  <div className="actions-row" style={{ display: "flex", gap: 10, marginTop: 12, flexWrap: "wrap" }}>
                    {can("CAN_CREATE_PAYMENTS") && ["DISPATCHED_UNPAID", "DELIVERED_UNPAID"].includes(o.orderStatus) && (
                      <button onClick={() => openCashPaymentModal(o)} style={cashPaymentBtn}>
                        <FiDollarSign size={13} /> Record Cash
                      </button>
                    )}
                    <button onClick={() => openEditModal(o)} style={editBtn}>
                      <FiEdit2 size={13} /> Edit
                    </button>
                    <button onClick={() => openNotesModal(o)} style={noteBtn}>
                      <FiFileText size={13} /> Notes
                    </button>
                  </div>
                </div>
              </React.Fragment>
            );
          })}

        {!loading && orders.length === 0 && (
          <EmptyState
            icon={FiShoppingBag}
            title={search ? "No matching orders" : "No orders found"}
            message={search ? "No orders match your search." : "Create an order to get started."}
          />
        )}

        {/* Footer pagination */}
        {pageData && (
          <div
            className="orders-pagination"
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
              Showing {orders.length} orders ({pageData.totalElements} total)
            </div>

            <div className="orders-pagination-controls" style={{ display: "flex", gap: 10, alignItems: "center" }}>
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

      {isAddOpen && (
        <AddOrderModal
          customerForm={customerForm}
          orderForm={orderForm}
          itemForm={itemForm}
          cartItems={cartItems}
          cartTotal={cartTotal}
          products={products}
          loading={loading}
          error={error}
          onCustomerChange={handleCustomerChange}
          onOrderChange={handleOrderChange}
          onItemChange={handleItemChange}
          paymentLines={paymentLines}
          onPaymentLinesChange={setPaymentLines}
          onOrderTypeChange={(value) => {
            setOrderForm((f) => ({ ...f, orderType: value }));
            setPaymentLines([emptyPaymentLine()]);
          }}
          onAddItem={addItemToCart}
          onRemoveItem={removeCartItem}
          onClose={closeAddModal}
          onCreate={handleCreateOrder}
        />
      )}

      {editingOrder && (
        <EditStatusModal
          order={editingOrder}
          statuses={ORDER_STATUSES}
          status={editStatus}
          loading={loading}
          error={error}
          onStatusChange={setEditStatus}
          onClose={closeEditModal}
          onSave={handleUpdateStatus}
        />
      )}

      {cashPaymentOrder && (
        <CashPaymentModal
          order={cashPaymentOrder}
          amount={cashPaymentAmount}
          error={cashPaymentError}
          saving={recordingCashPayment}
          onAmountChange={setCashPaymentAmount}
          onClose={closeCashPaymentModal}
          onConfirm={handleRecordCashPayment}
        />
      )}

      {selectedOrder && (
        <OrderNotesModal
          order={selectedOrder}
          text={noteText}
          error={noteError}
          loading={loading}
          onChange={setNoteText}
          onClose={closeNotesModal}
          onSave={handleUpdateNotes}
        />
      )}

      {receiptOrder && <ReceiptModal order={receiptOrder} fallbackServedBy={servedBy} />}

      {printingReceipt && (
        <div style={{ position: "fixed", right: 16, bottom: 16, background: "#111827", color: "#fff", padding: "10px 14px", borderRadius: 12, fontWeight: 700, zIndex: 1001 }}>
          Printing receipt...
        </div>
      )}
    </div>
  );
}
