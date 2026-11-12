import React, { useEffect, useMemo, useState } from "react";
import { api } from "../api/api";
import { loadProducts as fetchProductCatalog, loadStaff as fetchStaffList } from "../api/referenceData";
import { useAuth } from "../auth/useAuth";
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
  labelStyle,
  primaryBtn,
  secondaryBtn as cancelBtn,
  actionBtn,
  dangerActionBtn,
  pageBtnStyle,
  modalOverlay,
  modalBox as baseModalBox,
  modalTitle,
  closeBtn,
  tableCard,
  alertError,
} from "../components/styles";

const MPESA_PAYBILL = "522522";

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

  // Order status enums
  const ORDER_STATUSES = [
    "AWAITING_DISPATCH",
    "DISPATCHED_PAID",
    "DISPATCHED_PARTIAL",
    "DISPATCHED_UNPAID",
    "DELIVERED_PAID",
    "DELIVERED_UNPAID",
    "RETURN_REQUESTED",
    "RETURN_IN_TRANSIT",
    "RETURNED_AWAITING_INSPECTION",
    "RETURNED_GOOD",
    "RETURNED_DAMAGED",
    "CANCELLED",
    "COMPLETED",
  ];

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
          MPESA_PAYBILL
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

  function formatMoney(value) {
    return `KES ${Number(value || 0).toLocaleString()}`;
  }

  function statusStyle(status) {
    const s = String(status || "").toUpperCase();

    if (s.includes("DELIVERED") || s === "COMPLETED")
      return { color: "#166534", bg: "#dcfce7" };

    if (s.includes("DISPATCHED"))
      return { color: "#1d4ed8", bg: "#dbeafe" };

    if (s.includes("AWAITING"))
      return { color: "#b45309", bg: "#ffedd5" };

    if (s === "CANCELLED") return { color: "#9f1239", bg: "#ffe4e6" };

    return { color: "#374151", bg: "#f3f4f6" };
  }

  function niceStatus(status) {
    return String(status || "")
      .replaceAll("_", " ")
      .toLowerCase()
      .replace(/\b\w/g, (c) => c.toUpperCase());
  }

  // Return a color hex for highlighting rows/cards based on age and status
  function getParcelAgeHighlight(order) {
    if (!order) return null;
    const createdAt = order.createdAt ? new Date(order.createdAt) : null;
    if (!createdAt || isNaN(createdAt.getTime())) return null;

    const days = Math.floor((Date.now() - createdAt.getTime()) / (1000 * 60 * 60 * 24));
    const status = String(order.orderStatus || "").toUpperCase();

    // Dispatched and paid: distinct color
    if (status.includes("DISPATCHED") && status.includes("PAID")) {
      return "#0ea5a4"; // teal for dispatched+paid
    }

    // Green: picked and paid (status includes PAID and is delivered/completed)
    if (
      status.includes("PAID") &&
      (status.includes("DELIVERED") || status === "COMPLETED")
    ) {
      return "#16a34a"; // green
    }

    // Special: DISPATCHED but UNPAID -> show its own color initially,
    // but after 7 days, if still not delivered/paid, fall through to age rules.
    if (status.includes("DISPATCHED") && status.includes("UNPAID")) {
      if (days < 7) return "#1d4ed8"; // blue for recently dispatched-unpaid
      // else allow fallthrough so days>=7 becomes orange, >=14 becomes red
    }

    // Red: more than or equal to 14 days and not collected/paid
    if (days >= 14) return "#dc2626"; // red

    // Orange: more than or equal to 7 days and not collected/paid
    if (days >= 7) return "#f97316"; // orange

    return null;
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
      };

      console.log("ORDER PAYLOAD:", orderPayload);

      await api.post("/api/v1/orders", orderPayload);

      closeAddModal();
      setPage(0);
      await loadOrders(0, statusFilter, search.trim());

      alert("Order created successfully!");
    } catch (e) {
      console.log(e);
      setError("Failed to create order. Check backend + customer endpoint.");
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

  function previewText(text, maxLength = 60) {
    if (!text) return "No delivery notes";
    return text.length <= maxLength ? text : `${text.slice(0, maxLength)}…`;
  }

  function getOrderProductLines(order) {
    const itemList = [order.items, order.orderItems, order.products]
      .find((value) => Array.isArray(value) && value.length > 0);

    if (itemList) {
      const lines = itemList
        .map((item) => {
          const name = typeof item === "string"
            ? item
            : item.productName || item.product?.name || item.name;
          if (!name) return null;
          const quantity = item.quantity ?? item.qty;
          return quantity != null ? `${name} (×${quantity})` : String(name);
        })
        .filter(Boolean);

      if (lines.length) return lines;
    }

    return [order.productName || "-"];
  }

  return (
    <div className="orders-page" style={{ width: "100%", boxSizing: "border-box" }}>
      {/*  Responsive CSS must be INSIDE return */}
      <style>{`
        @media (max-width: 768px) {
          .orders-page {
            width: 100%;
            box-sizing: border-box;
          }

          .desktop-table-header,
          .desktop-row {
            display: none !important;
          }

          .orders-table {
            border-radius: 12px !important;
          }

          .orders-filter {
            display: grid !important;
            grid-template-columns: 1fr;
            gap: 8px !important;
          }

          .orders-filter select,
          .orders-filter input {
            min-width: 0;
            width: 100%;
            box-sizing: border-box;
          }

          .mobile-card {
            display: block;
            padding: 14px 12px;
            border-top: 1px solid #f3f4f6;
            background: #fff;
          }

          .mobile-card-top {
            align-items: flex-start !important;
          }

          .mobile-card-customer {
            min-width: 0;
            overflow-wrap: anywhere;
          }

          .mobile-card-summary {
            flex: 0 0 auto;
            max-width: 46%;
          }

          .mobile-card-products {
            overflow-wrap: anywhere;
            line-height: 1.45;
          }

          .orders-pagination {
            align-items: center !important;
            justify-content: center !important;
          }

          .orders-pagination-controls {
            width: 100%;
            justify-content: center;
          }

          .responsive-grid-2 {
            grid-template-columns: 1fr !important;
          }

          .responsive-grid-3 {
            grid-template-columns: 1fr !important;
          }

          .header-flex {
            flex-direction: column;
            align-items: flex-start !important;
            gap: 12px;
          }

          .header-actions {
            width: 100%;
            justify-content: space-between;
          }

          .actions-row {
            flex-wrap: wrap;
          }

          .actions-row button {
            min-height: 40px;
            flex: 1 1 120px;
          }

          .orders-modal-overlay {
            padding: 8px !important;
            place-items: center stretch !important;
            overflow-y: auto;
          }

          .orders-modal,
          .orders-modal-small {
            width: 100% !important;
            max-width: 100% !important;
            max-height: calc(100dvh - 16px);
            overflow-y: auto;
            padding: 14px !important;
            border-radius: 14px !important;
            box-sizing: border-box;
          }

          .orders-modal-footer {
            position: sticky;
            bottom: -14px;
            padding: 12px 0 2px;
            background: #fff;
          }

          .orders-create-button {
            width: 100%;
            min-height: 44px;
          }
        }

        @media (max-width: 420px) {
          .orders-page {
            width: 100%;
            min-height: 100%;
            box-sizing: border-box;
          }

          .header-actions {
            gap: 6px !important;
          }

          .header-actions > div {
            width: 32px !important;
            height: 32px !important;
          }

          .orders-filter {
            grid-template-columns: 1fr !important;
          }

          .mobile-card-summary {
            max-width: 52%;
          }
        }

        @media (min-width: 769px) {
          .mobile-card {
            display: none;
          }
        }

        .print-receipt {
          display: none;
        }

        .receipt-paper {
          width: 80mm;
          box-sizing: border-box;
          padding: 8mm;
          color: #111827;
          background: #fff;
          font-family: Arial, sans-serif;
          font-size: 12px;
        }

        .receipt-header {
          text-align: center;
          border-bottom: 2px solid #111827;
          padding-bottom: 10px;
          margin-bottom: 12px;
        }

        .receipt-header h1 {
          margin: 0;
          font-size: 20px;
          letter-spacing: 1px;
        }

        .receipt-title {
          margin: 5px 0 0;
          font-size: 11px;
          text-transform: uppercase;
          color: #4b5563;
        }

        .receipt-section {
          border-bottom: 1px dashed #9ca3af;
          padding: 4px 0 8px;
          margin-bottom: 8px;
        }

        .receipt-row {
          display: flex;
          justify-content: space-between;
          gap: 12px;
          margin: 6px 0;
        }

        .receipt-row strong:last-child {
          text-align: right;
          overflow-wrap: anywhere;
        }

        .receipt-total {
          font-size: 15px;
          font-weight: 800;
          border-top: 2px solid #111827;
          padding-top: 9px;
        }

        .receipt-payment {
          background: #f3f4f6;
          padding: 9px;
          margin-top: 10px;
        }

        .receipt-payment-value {
          font-size: 18px !important;
        }

        .receipt-footer {
          text-align: center;
          margin-top: 14px;
          color: #4b5563;
          font-size: 11px;
        }

        @media print {
          @page {
            size: 80mm auto;
            margin: 0;
          }

          html,
          body,
          #root {
            margin: 0 !important;
            padding: 0 !important;
            background: #fff !important;
            width: 80mm !important;
            height: auto !important;
            overflow: visible !important;
          }

          #root > *:not(.print-receipt) {
            display: none !important;
            visibility: hidden !important;
          }

          .print-receipt,
          .print-receipt * {
            visibility: visible !important;
          }

          .print-receipt {
            display: block !important;
            position: static !important;
            width: 80mm !important;
            margin: 0 !important;
            padding: 0 !important;
            background: #fff !important;
            page-break-inside: avoid;
            break-inside: avoid;
          }
        }
      `}</style>

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

      {/* ADD ORDER MODAL */}
      {isAddOpen && (
        <div className="orders-modal-overlay" style={modalOverlay} onClick={closeAddModal}>
          <div className="orders-modal" style={modalBox} onClick={(e) => e.stopPropagation()}>
            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
                marginBottom: 12,
              }}
            >
              <h3 style={{ ...modalTitle, margin: 0 }}>Create Order</h3>

              <button onClick={closeAddModal} style={xBtn}><FiX size={16} /></button>
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
                onChange={handleCustomerChange}
                placeholder="Customer Name"
                style={inputStyle}
              />
              <input
                name="phone"
                value={customerForm.phone}
                onChange={handleCustomerChange}
                placeholder="Phone Number"
                style={inputStyle}
              />
              <input
                name="email"
                value={customerForm.email}
                onChange={handleCustomerChange}
                placeholder="Email (optional)"
                style={inputStyle}
              />
              <input
                name="city_town"
                value={customerForm.city_town}
                onChange={handleCustomerChange}
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
                onChange={(e) =>
                  setOrderForm((f) => ({ ...f, orderType: e.target.value }))
                }
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
                    onChange={handleOrderChange}
                    placeholder="Delivery City"
                    style={inputStyle}
                  />
                  <input
                    name="deliveryNotes"
                    value={orderForm.deliveryNotes}
                    onChange={handleOrderChange}
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
                onChange={handleOrderChange}
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
                onChange={handleItemChange}
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
                onChange={handleItemChange}
                placeholder="Qty"
                type="number"
                style={inputStyle}
              />

              <button onClick={addItemToCart} style={addItemBtn}>
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

                      <button onClick={() => removeCartItem(i.productId)} style={removeBtn}><FiX size={16} /></button>
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
              <button className="orders-create-button" onClick={handleCreateOrder} style={createBtn}>
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
      )}

      {/* EDIT STATUS MODAL */}
      {editingOrder && (
        <div className="orders-modal-overlay" style={modalOverlay} onClick={closeEditModal}>
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

              <button onClick={closeEditModal} style={xBtn}><FiX size={16} /></button>
            </div>

            <div style={{ marginBottom: 10, color: "#6b7280" }}>
              Order: <b>{String(editingOrder.id).slice(0, 8).toUpperCase()}</b>
            </div>

            <select
              value={editStatus}
              onChange={(e) => setEditStatus(e.target.value)}
              style={inputStyle}
            >
              {ORDER_STATUSES.map((s) => (
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
              <button onClick={closeEditModal} style={cancelBtn}>
                Cancel
              </button>

              <button onClick={handleUpdateStatus} style={saveBtn}>
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
      )}

      {cashPaymentOrder && (
        <div className="orders-modal-overlay" style={modalOverlay} onClick={closeCashPaymentModal}>
          <div className="orders-modal-small" style={modalBoxSmall} onClick={(e) => e.stopPropagation()}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 12 }}>
              <h3 style={{ ...modalTitle, margin: 0 }}>Record Cash Payment</h3>
              <button onClick={closeCashPaymentModal} style={xBtn} disabled={recordingCashPayment}><FiX size={16} /></button>
            </div>

            <div style={{ marginBottom: 10, color: "#6b7280" }}>
              Order: <b>{String(cashPaymentOrder.id).slice(0, 8).toUpperCase()}</b>
            </div>
            <div style={{ marginBottom: 14, color: "#374151" }}>
              Customer: <b>{cashPaymentOrder.customerName || "-"}</b>
              <br />
              Order total: <b>{formatMoney(cashPaymentOrder.totalAmount)}</b>
              <br />
              Payment method: <b>Cash</b>
            </div>

            <label style={{ display: "block", marginBottom: 6, fontWeight: 700 }}>Amount received (KES)</label>
            <input
              type="number"
              min="0.01"
              step="0.01"
              value={cashPaymentAmount}
              onChange={(e) => setCashPaymentAmount(e.target.value)}
              style={inputStyle}
              disabled={recordingCashPayment}
            />

            <div style={{ display: "flex", justifyContent: "flex-end", gap: 10, marginTop: 16 }}>
              <button onClick={closeCashPaymentModal} style={cancelBtn} disabled={recordingCashPayment}>
                Cancel
              </button>
              <button onClick={handleRecordCashPayment} style={cashPaymentBtn} disabled={recordingCashPayment}>
                {recordingCashPayment ? "Recording..." : "Confirm Cash Payment"}
              </button>
            </div>

            {cashPaymentError && (
              <div style={{ marginTop: 10, color: "crimson", fontWeight: 700 }}>
                {cashPaymentError}
              </div>
            )}
          </div>
        </div>
      )}

      {selectedOrder && (
        <div className="orders-modal-overlay" style={modalOverlay} onClick={closeNotesModal}>
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
              <button onClick={closeNotesModal} style={xBtn}><FiX size={16} /></button>
            </div>

            <div style={{ marginBottom: 10, color: "#6b7280" }}>
              Order: <b>{String(selectedOrder.id).slice(0, 8).toUpperCase()}</b>
            </div>

            <textarea
              value={noteText}
              onChange={(e) => setNoteText(e.target.value)}
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
              <button onClick={closeNotesModal} style={cancelBtn}>
                Cancel
              </button>

              <button onClick={handleUpdateNotes} style={saveBtn}>
                {loading ? "Saving..." : "Save Notes"}
              </button>
            </div>

            {noteError && (
              <div style={{ marginTop: 10, color: "crimson", fontWeight: 700 }}>
                {noteError}
              </div>
            )}
          </div>
        </div>
      )}

      {receiptOrder && (
        <div id="receipt-print-root" className="print-receipt" aria-hidden="true">
          <div className="receipt-paper">
            <div className="receipt-header">
              <h1>URBAN TRENDS</h1>
              <div className="receipt-title">Dispatch Receipt</div>
            </div>

            <div className="receipt-section">
              <div className="receipt-row">
                <strong>Order</strong>
                <strong>{String(receiptOrder.id || "").slice(0, 8).toUpperCase()}</strong>
              </div>
              <div className="receipt-row">
                <span>Date</span>
                <span>{formatDateTime(receiptOrder.createdAt)}</span>
              </div>
              <div className="receipt-row">
                <span>Status</span>
                <span>{niceStatus(receiptOrder.orderStatus)}</span>
              </div>
              <div className="receipt-row">
                <span>Served by</span>
                <span>{receiptOrder.servedBy || servedBy}</span>
              </div>
            </div>

            <div className="receipt-section">
              <div className="receipt-row">
                <span>Customer</span>
                <strong>{receiptOrder.customerName || "-"}</strong>
              </div>
              <div className="receipt-row">
                <span>Phone</span>
                <strong>{receiptOrder.phoneNumber || "-"}</strong>
              </div>
              <div className="receipt-row">
                <span>City</span>
                <strong>{receiptOrder.deliveryCity || "-"}</strong>
              </div>
            </div>

            <div className="receipt-section">
              <div className="receipt-row">
                <span>{getOrderProductLines(receiptOrder).length > 1 ? "Products" : "Product"}</span>
                <strong>
                  {getOrderProductLines(receiptOrder).map((productLine, productIndex) => (
                    <div key={`${receiptOrder.id}-receipt-product-${productIndex}`}>{productLine}</div>
                  ))}
                </strong>
              </div>
              <div className="receipt-row receipt-total">
                <span>Amount Due</span>
                <span>{formatMoney(receiptOrder.totalAmount)}</span>
              </div>
            </div>

            <div className="receipt-payment">
              <div className="receipt-row">
                <span>M-Pesa Paybill</span>
                <strong>{MPESA_PAYBILL}</strong>
              </div>
              <div className="receipt-row">
                <span>Account No</span>
                <strong>{receiptOrder.accountNumber || "Use order number"}</strong>
              </div>
            </div>
          </div>
        </div>
      )}

      {printingReceipt && (
        <div style={{ position: "fixed", right: 16, bottom: 16, background: "#111827", color: "#fff", padding: "10px 14px", borderRadius: 12, fontWeight: 700, zIndex: 1001 }}>
          Printing receipt...
        </div>
      )}
    </div>
  );
}

const formatDateTime = (dateString) => {
  if (!dateString) return "-";

  const date = new Date(dateString);

  return `${date.toLocaleDateString("en-CA")} ${date.toLocaleTimeString([], {
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  })}`;
};

/* --------------------------
   Styles (same theme)
-------------------------- */

const addOrderBtn = primaryBtn;

const editBtn = actionBtn;

const cashPaymentBtn = {
  ...actionBtn,
  border: "1px solid transparent",
  background: "var(--success)",
  color: "#fff",
};

const addItemBtn = { ...primaryBtn, height: 42 };

const createBtn = primaryBtn;

const removeBtn = dangerActionBtn;

const modalBox = { ...baseModalBox, width: 760 };

const modalBoxSmall = baseModalBox;

const xBtn = closeBtn;

const saveBtn = primaryBtn;

const sectionTitle = {
  fontSize: 12,
  fontWeight: 600,
  textTransform: "uppercase",
  letterSpacing: "0.05em",
  color: "var(--text-muted)",
  marginBottom: 10,
};

const noteBtn = actionBtn;

