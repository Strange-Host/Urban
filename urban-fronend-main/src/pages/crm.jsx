import React, { useEffect, useState, useRef, useCallback } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { api } from "../api/api";
import { loadProducts as fetchProductCatalog } from "../api/referenceData";
import { publishNotification } from "../utils/notificationBus.js";
import {
  FiPlus,
  FiBell,
  FiX,
  FiArrowRight,
  FiChevronLeft,
  FiChevronRight,
  FiUsers,
  FiStar,
  FiCheckCircle,
  FiPhoneCall,
  FiXCircle,
  FiClock,
  FiInbox,
} from "react-icons/fi";
import { PageHeader, EmptyState } from "../components/ui";

// ─── Toast Component ───────────────────────────────────────────────────────────
function Toast({ notifications, onDismiss, onNavigate }) {
  return (
    <div className="fixed top-4 right-4 z-[100] flex flex-col gap-2 max-w-sm w-full">
      {notifications.map((n) => (
        <div
          key={n.id}
          className="bg-white border border-slate-200 shadow-xl rounded-xl p-4 flex items-start gap-3 animate-slide-in"
        >
          <span className="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-emerald-50 text-emerald-600">
            <FiBell size={16} />
          </span>
          <div className="flex-1 min-w-0">
            <div className="font-semibold text-sm text-slate-800">{n.title}</div>
            <div className="text-xs text-slate-500 mt-1">{n.message}</div>
            <button
              onClick={() => onNavigate(n)}
              className="mt-2 inline-flex items-center gap-1 text-xs font-semibold text-brand-600 hover:text-brand-700"
            >
              View lead <FiArrowRight size={12} />
            </button>
          </div>
          <button
            onClick={() => onDismiss(n.id)}
            aria-label="Dismiss"
            className="grid h-7 w-7 place-items-center rounded-lg text-slate-400 hover:bg-slate-100 hover:text-slate-600"
          >
            <FiX size={15} />
          </button>
        </div>
      ))}
    </div>
  );
}

// ─── Main CRM Component ────────────────────────────────────────────────────────
export default function CRM() {
  const navigate = useNavigate();
  const { id: leadId } = useParams();

  const [leads, setLeads] = useState([]);
  const [pageData, setPageData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [statusFilter, setStatusFilter] = useState("");
  const [page, setPage] = useState(0);
  const [error, setError] = useState("");
  const [followUps, setFollowUps] = useState({});

  const [selectedLead, setSelectedLead] = useState(null);
  const [highlightedLeadId, setHighlightedLeadId] = useState(null);
  const leadRefs = useRef({});
  const [deliveryCity, setDeliveryCity] = useState("");
  const [deliveryFee, setDeliveryFee] = useState(0);
  const [discount, setDiscount] = useState(0);
  const [converting, setConverting] = useState(false);
  const [conversionItems, setConversionItems] = useState([]);
  const [productsForConversion, setProductsForConversion] = useState([]);
  const [conversionProductCatalog, setConversionProductCatalog] = useState([]);
  const [productPickerMode, setProductPickerMode] = useState("");
  const [pickerProductId, setPickerProductId] = useState("");
  const [productSearch, setProductSearch] = useState("");
  const [productSearchLoading, setProductSearchLoading] = useState(false);
  const [noteEdits, setNoteEdits] = useState({});
  const [focusedNoteId, setFocusedNoteId] = useState(null);
  const [extraInfo, setExtraInfo] = useState("");
  const [customerNotes, setCustomerNotes] = useState("");

  // Couriers for the "pick a courier" dropdown on the convert modal
  const [couriers, setCouriers] = useState([]);
  const [selectedCourierId, setSelectedCourierId] = useState("");

  // Manual lead creation
  const [showAddLeadModal, setShowAddLeadModal] = useState(false);
  const [newLead, setNewLead] = useState({
    name: "",
    phoneNumber: "",
    alternativePhoneNumber: "",
    location: "",
    formName: "",
    additionalInfo: "",
    notes: "",
    source: "CALL",
  });
  const [savingLead, setSavingLead] = useState(false);
  const [leadProducts, setLeadProducts] = useState([]);
  const [loadingLeadProducts, setLoadingLeadProducts] = useState(false);
  const [leadProductsError, setLeadProductsError] = useState("");

  // Toast notifications (live SSE popups)
  const [toasts, setToasts] = useState([]);

  const size = 10;

  // ── Navigate to lead from notification ──────────────────────────────────────
  const handleNotificationNavigate = (notification) => {
    navigate(`/crm/leads/${notification.referenceId}`);
  };

  // ── SSE: subscribe to live notifications ────────────────────────────────────
  useEffect(() => {
    const token = localStorage.getItem("accessToken");
    if (!token) return;

    const base = api.defaults.baseURL || "";
    const es = new EventSource(
      `${base}/api/v1/notifications/subscribe?access_token=${encodeURIComponent(token)}`
    );

    es.addEventListener("notification", (e) => {
      try {
        const notification = JSON.parse(e.data);

        setToasts((prev) =>
          prev.some((t) => t.id === notification.id)
            ? prev
            : [...prev, notification]
        );

        try {
          publishNotification(notification);
        } catch (busErr) {
          console.warn("Failed to publish notification to bus", busErr);
        }

        setTimeout(() => {
          setToasts((prev) => prev.filter((t) => t.id !== notification.id));
        }, 8000);
      } catch (err) {
        console.error("Failed to parse SSE notification", err);
      }
    });

    es.onerror = () => {
      console.warn("SSE connection lost, will retry automatically");
    };

    return () => es.close();
  }, []);

  // ── Fetch leads ─────────────────────────────────────────────────────────────
  useEffect(() => {
    fetchLeads();
  }, [page, statusFilter]);

  useEffect(() => {
    if (!showAddLeadModal) return;

    const fetchLeadProducts = async () => {
      try {
        setLoadingLeadProducts(true);
        setLeadProductsError("");
        setLeadProducts(await fetchProductCatalog());
      } catch (err) {
        console.error("Failed to load products for lead form", err);
        setLeadProductsError("Couldn't load products. Please close and reopen this form to try again.");
      } finally {
        setLoadingLeadProducts(false);
      }
    };

    fetchLeadProducts();
  }, [showAddLeadModal]);

  useEffect(() => {
    if (!leadId) return;
    setHighlightedLeadId(leadId);
    const element = leadRefs.current[leadId];
    if (element) {
      element.scrollIntoView({ behavior: "smooth", block: "center" });
    }
  }, [leadId, leads]);

  const fetchLeads = async () => {
    try {
      setLoading(true);
      setError("");
      const res = await api.get("/api/v1/leads", {
        params: { page, size, status: statusFilter || undefined },
      });
      setLeads(res.data.content);
      setPageData(res.data);
    } catch (err) {
      console.error(err);
      setError("Failed to load leads");
    } finally {
      setLoading(false);
    }
  };

  // ── Update lead status (handles RESCHEDULED with date) ──────────────────────
  const updateLeadStatus = async (leadId, status) => {
    try {
      const payload = { status };
      if (status === "RESCHEDULED") {
        if (!followUps[leadId]) {
          alert("Please pick a follow-up date before setting status to Rescheduled.");
          return;
        }
        payload.followUpAt = followUps[leadId];
      }
      await api.patch(`/api/v1/leads/${leadId}/status`, payload);
      // Clear the follow-up input for this lead after successful save
      setFollowUps((prev) => ({ ...prev, [leadId]: "" }));
      fetchLeads();
    } catch (err) {
      console.error("Status update failed", err);
      alert("Failed to update lead status");
    }
  };

  const refreshProductsForConversion = useCallback(async (leadProductName = "") => {
    let productFromNameLookup = null;
    if (leadProductName) {
      try {
        const lookupRes = await api.get("/api/v1/products/by-name", {
          params: { name: leadProductName },
        });
        const lookupProduct = lookupRes.data;
        if (
          lookupProduct?.id &&
          String(lookupProduct.name).trim().toLowerCase() === String(leadProductName).trim().toLowerCase()
        ) {
          productFromNameLookup = lookupProduct;
        }
      } catch (lookupErr) {
        console.warn("Exact product lookup failed; checking the loaded catalog", lookupErr);
      }
    }

    try {
      const availableProducts = await fetchProductCatalog();

      if (leadProductName) {
        const matchingProducts = availableProducts.filter(
          (product) => String(product.name).trim().toLowerCase() === String(leadProductName).trim().toLowerCase()
        );

        // Do not guess when no exact match exists (or duplicate names are ambiguous).
        if (matchingProducts.length > 1) {
          setConversionProductCatalog(availableProducts);
          setProductsForConversion(availableProducts);
          return null;
        }
        if (matchingProducts.length === 1) {
          setConversionProductCatalog(availableProducts);
          setProductsForConversion(availableProducts);
          return matchingProducts[0];
        }

        if (productFromNameLookup) {
          const catalog = [...availableProducts, productFromNameLookup];
          setConversionProductCatalog(catalog);
          setProductsForConversion(catalog);
          return productFromNameLookup;
        }

        setConversionProductCatalog(availableProducts);
        setProductsForConversion(availableProducts);
        return null;
      }

      setConversionProductCatalog(availableProducts);
      setProductsForConversion(availableProducts);
      return null;
    } catch (productsErr) {
      console.error("Failed to load products for conversion", productsErr);
      const fallbackProducts = productFromNameLookup ? [productFromNameLookup] : [];
      setConversionProductCatalog(fallbackProducts);
      setProductsForConversion(fallbackProducts);
      return productFromNameLookup;
    }
  }, []);

  useEffect(() => {
    if (!productPickerMode) return;

    const query = productSearch.trim();
    if (!query) {
      setProductsForConversion(conversionProductCatalog);
      setProductSearchLoading(false);
      return;
    }

    let active = true;
    const timeoutId = setTimeout(async () => {
      setProductSearchLoading(true);
      try {
        const response = await api.get("/api/v1/products/search", {
          params: { query },
        });
        const data = response.data;
        const results = Array.isArray(data)
          ? data
          : Array.isArray(data?.content)
            ? data.content
            : Array.isArray(data?.data)
              ? data.data
            : Array.isArray(data?.products)
              ? data.products
              : [];

        if (active) setProductsForConversion(results);
      } catch (searchErr) {
        console.error("Product search failed", searchErr);
        if (active) setProductsForConversion([]);
      } finally {
        if (active) setProductSearchLoading(false);
      }
    }, 250);

    return () => {
      active = false;
      clearTimeout(timeoutId);
    };
  }, [productPickerMode, productSearch, conversionProductCatalog]);

  // ── Open convert modal ───────────────────────────────────────────────────────
  const openConvertModal = async (lead) => {
    try {
      setSelectedLead(lead);
      setDeliveryCity(lead.location || "");
      setDeliveryFee(0);
      setDiscount(0);
      setProductPickerMode("");
      setPickerProductId("");
      setSelectedCourierId("");

      const latestProduct = await refreshProductsForConversion(lead.formName);
      if (!latestProduct || !Number(latestProduct.sellingPrice) && Number(latestProduct.sellingPrice) !== 0) {
        const notification = {
          id: Date.now(),
          title: "Select a Product",
          message: `Couldn't uniquely match "${lead.formName}" to a product. Choose the correct product in the conversion window.`,
        };
        setToasts((prev) => [...prev, notification]);
        setTimeout(() => {
          setToasts((prev) => prev.filter((t) => t.id !== notification.id));
        }, 4000);
        setConversionItems([]);
        return;
      }

      const productToUse = {
        productId: latestProduct.id,
        productName: latestProduct.name || lead.formName,
        sellingPrice: Number(latestProduct.sellingPrice),
        quantity: 1,
      };

      setConversionItems([productToUse]);

      try {
        const courierRes = await api.get("/api/v1/couriers/active");
        setCouriers(courierRes.data || []);
      } catch (courierErr) {
        console.error("Failed to load couriers", courierErr);
        setCouriers([]);
      }
    } catch (err) {
      console.error("Failed to fetch product price", err);
      const notification = {
        id: Date.now(),
        title: "Product Not Available",
        message: `The product "${lead.formName}" is not available in your inventory`,
      };
      setToasts((prev) => [...prev, notification]);
      setTimeout(() => {
        setToasts((prev) => prev.filter((t) => t.id !== notification.id));
      }, 4000);
    }
  };

  const openProductPicker = async (mode) => {
    await refreshProductsForConversion(selectedLead?.formName || "");
    setProductPickerMode(mode);
    setPickerProductId("");
    setProductSearch("");
  };

  const applyPickedProduct = () => {
    const product = productsForConversion.find(
      (item) => String(item.id) === String(pickerProductId)
    );
    if (!product) return;

    const selectedProduct = {
      productId: product.id,
      productName: product.name,
      sellingPrice: Number(product.sellingPrice),
      quantity: 1,
    };

    if (productPickerMode === "change") {
      setConversionItems((items) => items.length === 0
        ? [selectedProduct]
        : items.map((item, index) => index === 0
          ? { ...selectedProduct, quantity: item.quantity }
          : item));
    } else {
      setConversionItems((items) => [...items, selectedProduct]);
    }
    setProductPickerMode("");
    setPickerProductId("");
    setProductSearch("");
  };

  // ── Convert lead ─────────────────────────────────────────────────────────────
  const convertLead = async () => {
    try {
      setConverting(true);
      if (!conversionItems.length || conversionItems.some((item) =>
        !Number.isFinite(Number(item.quantity)) || Number(item.quantity) <= 0 || !item.productId
      )) {
        alert("Add products with a valid quantity before converting.");
        return;
      }

      const subtotal = conversionItems.reduce(
        (sum, item) => sum + Number(item.quantity) * Number(item.sellingPrice || 0),
        0
      );
      const safeDeliveryFee = Number(deliveryFee) >= 0 ? Number(deliveryFee) : 0;
      const safeDiscount = Number(discount || 0) >= 0 ? Number(discount || 0) : 0;
      const totalAmount = Math.max(0, subtotal + safeDeliveryFee - safeDiscount);

      if (safeDiscount < 0) { alert("Discount cannot be negative."); return; }
      if (safeDiscount > subtotal + safeDeliveryFee) {
        alert("Discount cannot exceed the total order amount."); return;
      }

      const normalizedItems = conversionItems.map((item) => ({
        productId: item.productId,
        productName: item.productName,
        quantity: Number(item.quantity),
      }));

      const primaryProduct = normalizedItems[0];
      await api.post(`/api/v1/leads/${selectedLead.id}/convert`, {
        productId: primaryProduct.productId,
        productName: primaryProduct.productName,
        formName: primaryProduct.productName,
        quantity: Number(primaryProduct.quantity),
        items: normalizedItems,
        deliveryCity,
        deliveryFee: safeDeliveryFee,
        discount: safeDiscount,
        totalAmount,
        courierId: selectedCourierId || undefined,
        notes: customerNotes.trim() || undefined,
      });
      setSelectedLead(null);
      setSelectedCourierId("");
      setCustomerNotes("");
      fetchLeads();
    } catch (err) {
      console.error(err);
      const errorMsg = err.response?.data?.message || err.message;
      if (errorMsg && errorMsg.includes("product") && errorMsg.includes("not")) {
        alert(`The product "${selectedLead.formName}" is not available in your inventory`);
      } else {
        alert("Conversion failed");
      }
    } finally {
      setConverting(false);
    }
  };

  // ── Create manual lead ───────────────────────────────────────────────────────
  const createManualLead = async () => {
    if (!newLead.name.trim() || !newLead.phoneNumber.trim()) {
      alert("Name and phone number are required");
      return;
    }
    try {
      setSavingLead(true);
      await api.post("/api/v1/leads/manual", newLead);
      setShowAddLeadModal(false);
      setNewLead({
        name: "", phoneNumber: "", alternativePhoneNumber: "",
        location: "", formName: "", additionalInfo: "", notes: "", source: "CALL",
      });
      fetchLeads();
      const successToast = {
        id: Date.now(),
        title: "Lead Created",
        message: `Lead "${newLead.name}" has been added successfully`,
      };
      setToasts((prev) => [...prev, successToast]);
      setTimeout(() => {
        setToasts((prev) => prev.filter((t) => t.id !== successToast.id));
      }, 4000);
    } catch (err) {
      console.error("Failed to create lead", err);
      alert(err.response?.data?.message || "Failed to create lead");
    } finally {
      setSavingLead(false);
    }
  };

  // ── Computed totals for modal ────────────────────────────────────────────────
  const subtotal = conversionItems.reduce(
    (sum, item) => sum + Number(item.quantity || 0) * Number(item.sellingPrice || 0),
    0
  );
  const safeDeliveryFee = Number(deliveryFee) >= 0 ? Number(deliveryFee) : 0;
  const safeDiscount = Number(discount) >= 0 ? Number(discount) : 0;
  const totalAmount = Math.max(0, subtotal + safeDeliveryFee - safeDiscount);
  // ── Stats ────────────────────────────────────────────────────────────────────
  const totalLeads = pageData?.totalElements || 0;
  const newLeads = leads.filter((l) => l.status === "NEW").length;
  const convertedLeads = leads.filter((l) => l.status === "CONVERTED").length;
  const contactedLeads = leads.filter((l) => l.status === "CONTACTED").length;
  const cancelledLeads = leads.filter((l) => l.status === "CANCELLED").length;
  const rescheduledLeads = leads.filter((l) => l.status === "RESCHEDULED").length;

  return (
    <div>

      {/* ── Live Toast Notifications ── */}
      <Toast
        notifications={toasts}
        onDismiss={(id) => setToasts((prev) => prev.filter((t) => t.id !== id))}
        onNavigate={handleNotificationNavigate}
      />

      {/* ── Header ── */}
      <PageHeader title="CRM" subtitle="Track leads from first contact through to order">
        <button
          onClick={() => setShowAddLeadModal(true)}
          className="inline-flex h-[42px] items-center gap-2 rounded-[10px] bg-brand-600 px-4 text-sm font-semibold text-white shadow-sm hover:bg-brand-700"
        >
          <FiPlus size={16} /> Add Lead
        </button>
      </PageHeader>

      {/* ── Stats ── */}
      <div className="grid grid-cols-2 sm:grid-cols-3 xl:grid-cols-6 gap-3 md:gap-4 mb-6">
        <StatCard title="Total" value={totalLeads} icon={FiUsers} tone="slate" />
        <StatCard title="New" value={newLeads} icon={FiStar} tone="blue" />
        <StatCard title="Converted" value={convertedLeads} icon={FiCheckCircle} tone="green" />
        <StatCard title="Contacted" value={contactedLeads} icon={FiPhoneCall} tone="violet" />
        <StatCard title="Cancelled" value={cancelledLeads} icon={FiXCircle} tone="red" />
        <StatCard title="Rescheduled" value={rescheduledLeads} icon={FiClock} tone="amber" />
      </div>

      {/* ── Filter ── */}
      <div className="mb-4 flex gap-2 overflow-x-auto pb-1">
        {[
          ["", "All"],
          ["NEW", "New"],
          ["CONTACTED", "Contacted"],
          ["CONVERTED", "Converted"],
          ["CANCELLED", "Cancelled"],
          ["RESCHEDULED", "Rescheduled"],
        ].map(([value, label]) => (
          <button
            key={value || "all"}
            onClick={() => { setPage(0); setStatusFilter(value); }}
            className={`shrink-0 rounded-full border px-3.5 py-1.5 text-sm font-medium ${
              statusFilter === value
                ? "border-brand-600 bg-brand-600 text-white"
                : "border-slate-200 bg-white text-slate-600 hover:border-slate-300 hover:text-slate-800"
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      {error && <div className="mb-4 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-medium text-rose-700">{error}</div>}

      {leads.length === 0 && !error && (
        <div className="ui-card">
          <EmptyState icon={FiInbox} title="No leads found" message={statusFilter ? "Try a different status filter." : "New leads will appear here as they come in."} />
        </div>
      )}

      {/* ── MOBILE VIEW ── */}
      <div className="lg:hidden space-y-3">
        {leads.map((lead) => {
          const statusColor = {
            NEW: "bg-sky-100 text-sky-800",
            CONTACTED: "bg-violet-100 text-violet-800",
            RESCHEDULED: "bg-amber-100 text-amber-800",
            CANCELLED: "bg-red-100 text-red-800",
            CONVERTED: "bg-green-100 text-green-800",
          }[lead.status] || "bg-slate-100 text-slate-800";

          return (
            <div
              key={lead.id}
              ref={(el) => { if (el) leadRefs.current[lead.id] = el; }}
              className={`bg-white p-4 rounded-2xl shadow-sm border transition-all ${
                String(highlightedLeadId) === String(lead.id)
                  ? "border-brand-400 ring-4 ring-brand-500/15"
                  : "border-slate-200"
              }`}
            >
              {/* Name and Status Badge */}
              <div className="flex items-start justify-between gap-2 mb-2">
                <div className="flex-1 min-w-0">
                  <div className="font-semibold text-base truncate">{lead.name}</div>
                  {lead.phoneNumber && (
                    <a
                      href={formatPhoneLink(lead.phoneNumber)}
                      className="block text-xs text-brand-600 hover:underline truncate"
                    >
                      {lead.phoneNumber}
                    </a>
                  )}
                  {lead.alternativePhoneNumber && (
                    <a
                      href={formatPhoneLink(lead.alternativePhoneNumber)}
                      className="block text-xs text-brand-600 hover:underline truncate"
                    >
                      Alt Phone: {lead.alternativePhoneNumber}
                    </a>
                  )}
                </div>
                <span className={`px-2.5 py-0.5 rounded-full text-xs font-semibold whitespace-nowrap ${statusColor}`}>
                  {lead.status}
                </span>
              </div>

              {/* Product and Location */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 mb-3 text-xs">
                <div>
                  <div className="text-slate-500">Product</div>
                  <div className="font-medium truncate">{lead.formName}</div>
                </div>
                <div>
                  <div className="text-slate-500">Location</div>
                  <div className="font-medium truncate">{lead.location}</div>
                </div>
                <div>
                  <div className="text-slate-500">Extra Details</div>
                  <div className="font-medium truncate">{lead.additionalInfo}</div>
                </div>
                {formatProductOptions(lead.productOptions) && (
                  <div className="sm:col-span-2">
                    <div className="text-slate-500">Product Options</div>
                    <div className="font-medium break-words whitespace-normal p-2 bg-brand-50 text-brand-900 rounded-lg text-sm">{formatProductOptions(lead.productOptions)}</div>
                  </div>
                )}
                <div className="sm:col-span-2">
                  <div className="text-slate-500">Customer Notes</div>
                  <textarea
                    rows={3}
                    value={noteEdits[lead.id] ?? lead.customerNotes ?? lead.notes ?? ""}
                    onFocus={() => setFocusedNoteId(lead.id)}
                    onBlur={() => setTimeout(() => setFocusedNoteId((prev) => (prev === lead.id ? null : prev)), 100)}
                    onChange={(e) =>
                      setNoteEdits((prev) => ({ ...prev, [lead.id]: e.target.value }))
                    }
                    className="w-full rounded-lg border border-slate-200 p-2 text-sm"
                    placeholder="Add customer notes here"
                  />
                  {(focusedNoteId === lead.id || noteEdits[lead.id] !== undefined) && (
                    <button
                      onClick={async () => {
                        const notes = noteEdits[lead.id] ?? lead.customerNotes ?? lead.notes ?? "";
                        try {
                          await api.patch(`/api/v1/leads/${lead.id}`, { notes: notes || null });
                          setNoteEdits((prev) => {
                            const next = { ...prev };
                            delete next[lead.id];
                            return next;
                          });
                          setFocusedNoteId(null);
                          fetchLeads();
                        } catch (err) {
                          console.error("Failed to save notes", err);
                          alert("Failed to save notes");
                        }
                      }}
                      className="mt-2 w-full bg-brand-600 text-white py-2 rounded-lg text-sm font-medium hover:bg-brand-700"
                    >
                      Save Notes
                    </button>
                  )}
                </div>
              </div>

              {/* Timestamp */}
              <div className="text-xs text-slate-400 mb-3">{formatDateTime(lead.created)}</div>

              {/* Follow-up date picker — only shown when RESCHEDULED */}
              {lead.status === "RESCHEDULED" && lead.followUpAt && (
                <div className="mb-3 p-2.5 bg-amber-50 rounded-lg text-xs border border-amber-200">
                  <div className="text-slate-600 font-medium">Follow-up: {formatDateTime(lead.followUpAt)}</div>
                </div>
              )}

              {/* Status and Date Actions */}
              <div className="space-y-2">
                <div>
                  <label className="text-xs font-medium text-slate-700 block mb-1">Update Status</label>
                  <select
                    value={lead.status}
                    onChange={(e) => {
                      const newStatus = e.target.value;
                      if (newStatus === "RESCHEDULED") {
                        // Just mark in local state — date picker will appear
                        setFollowUps((prev) => ({
                          ...prev,
                          [lead.id]: prev[lead.id] || "",
                        }));
                      } else {
                        updateLeadStatus(lead.id, newStatus);
                      }
                    }}
                    className="rounded-lg border border-slate-200 bg-white px-2.5 py-2 w-full text-sm"
                  >
                    <option value="NEW">NEW</option>
                    <option value="CONTACTED">CONTACTED</option>
                    <option value="RESCHEDULED">RESCHEDULED</option>
                    <option value="CANCELLED">CANCELLED</option>
                    <option value="CONVERTED">CONVERTED</option>
                  </select>
                </div>

                {/* Date picker appears only when RESCHEDULED is selected */}
                {(lead.status === "RESCHEDULED" || followUps[lead.id] !== undefined) && (
                  <div>
                    <label className="text-xs font-medium text-slate-700 block mb-1">
                      Follow-up Date & Time
                    </label>
                    <input
                      type="datetime-local"
                      value={followUps[lead.id] || ""}
                      min={new Date().toISOString().slice(0, 16)}
                      onChange={(e) =>
                        setFollowUps((prev) => ({ ...prev, [lead.id]: e.target.value }))
                      }
                      className="rounded-lg border border-slate-200 bg-white px-2.5 py-2 w-full text-sm"
                    />
                    {followUps[lead.id] && (
                      <button
                        onClick={() => updateLeadStatus(lead.id, "RESCHEDULED")}
                        className="mt-1.5 w-full bg-brand-600 text-white py-1.5 rounded text-sm font-medium hover:bg-brand-700"
                      >
                        Save Follow-up
                      </button>
                    )}
                  </div>
                )}
              </div>

              {/* Convert Button */}
              {lead.status !== "CONVERTED" && (
                <button
                  onClick={() => openConvertModal(lead)}
                  className="mt-3 w-full bg-emerald-600 text-white py-2.5 rounded-[10px] text-sm font-semibold hover:bg-emerald-700"
                >
                  Convert to Order
                </button>
              )}
            </div>
          );
        })}
      </div>

      {/* ── DESKTOP TABLE ── */}
      <div className={`${leads.length === 0 ? "hidden" : "hidden lg:block"} ui-card overflow-x-auto`}>
        <table className="ui-table min-w-full">
          <thead>
            <tr>
              <th>Name</th>
              <th>Phone</th>
              <th>Product</th>
              <th>Options</th>
              <th>Location</th>
              <th>Alt Phone No</th>
              <th>Extra Details</th>
              <th>Notes</th>
              <th>Status</th>
              <th>Follow-up Date</th>
              <th>Timestamp</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            {leads.map((lead) => (
              <tr
              key={lead.id}
              ref={(el) => { if (el) leadRefs.current[lead.id] = el; }}
              className={`align-top ${String(highlightedLeadId) === String(lead.id) ? "!bg-brand-50" : ""}`}
            >
                <td className="font-medium text-slate-900 whitespace-nowrap">{lead.name}</td>
                <td>
                  {lead.phoneNumber ? (
                    <a href={formatPhoneLink(lead.phoneNumber)} className="text-brand-600 hover:underline">
                      {lead.phoneNumber}
                    </a>
                  ) : (
                    "—"
                  )}
                </td>
                <td>{lead.formName}</td>
                <td>{formatProductOptions(lead.productOptions) || "—"}</td>
                <td>{lead.location}</td>
                <td>
                  {lead.alternativePhoneNumber ? (
                    <a href={formatPhoneLink(lead.alternativePhoneNumber)} className="text-brand-600 hover:underline">
                      {lead.alternativePhoneNumber}
                    </a>
                  ) : (
                    "—"
                  )}
                </td>
                <td>{lead.additionalInfo || "—"}</td>
                <td>
                  <textarea
                    rows={2}
                    value={noteEdits[lead.id] ?? lead.customerNotes ?? lead.notes ?? ""}
                    onFocus={() => setFocusedNoteId(lead.id)}
                    onBlur={() => setTimeout(() => setFocusedNoteId((prev) => (prev === lead.id ? null : prev)), 100)}
                    onChange={(e) =>
                      setNoteEdits((prev) => ({ ...prev, [lead.id]: e.target.value }))
                    }
                    className="w-full min-w-[180px] rounded-lg border border-slate-200 p-1.5 text-sm"
                    placeholder="Add notes..."
                  />
                  {(focusedNoteId === lead.id || noteEdits[lead.id] !== undefined) && (
                    <button
                      onClick={async () => {
                        const notes = noteEdits[lead.id] ?? lead.customerNotes ?? lead.notes ?? "";
                        try {
                          await api.patch(`/api/v1/leads/${lead.id}`, { notes: notes || null });
                          setNoteEdits((prev) => {
                            const next = { ...prev };
                            delete next[lead.id];
                            return next;
                          });
                          setFocusedNoteId(null);
                          fetchLeads();
                        } catch (err) {
                          console.error("Failed to save notes", err);
                          alert("Failed to save notes");
                        }
                      }}
                      className="mt-2 bg-brand-600 text-white px-3 py-1 rounded-lg text-xs font-medium hover:bg-brand-700"
                    >
                      Save
                    </button>
                  )}
                </td>

                <td>
                  <select
                    value={lead.status}
                    onChange={(e) => {
                      const newStatus = e.target.value;
                      if (newStatus === "RESCHEDULED") {
                        // Just mark in local state — date picker will appear
                        setFollowUps((prev) => ({
                          ...prev,
                          [lead.id]: prev[lead.id] || "",
                        }));
                      } else {
                        updateLeadStatus(lead.id, newStatus);
                      }
                    }}
                    className="rounded-lg border border-slate-200 bg-white px-2 py-1.5 text-sm"
                  >
                    <option value="NEW">NEW</option>
                    <option value="CONTACTED">CONTACTED</option>
                    <option value="RESCHEDULED">RESCHEDULED</option>
                    <option value="CANCELLED">CANCELLED</option>
                    <option value="CONVERTED">CONVERTED</option>
                  </select>
                </td>

                {/* Follow-up date column */}
                <td className="min-w-[220px]">
                  {lead.status === "RESCHEDULED" || followUps[lead.id] !== undefined ? (
                    <div className="flex items-center gap-2">
                      <input
                        type="datetime-local"
                        value={followUps[lead.id] ?? (lead.followUpAt ? lead.followUpAt.slice(0, 16) : "")}
                        min={new Date().toISOString().slice(0, 16)}
                        onChange={(e) =>
                          setFollowUps((prev) => ({ ...prev, [lead.id]: e.target.value }))
                        }
                        className="rounded-lg border border-slate-200 p-1.5 text-xs"
                      />
                      {followUps[lead.id] && (
                        <button
                          onClick={() => updateLeadStatus(lead.id, "RESCHEDULED")}
                          className="bg-brand-600 text-white px-2 py-1 rounded-lg text-xs font-medium whitespace-nowrap hover:bg-brand-700"
                        >
                          Save
                        </button>
                      )}
                    </div>
                  ) : (
                    <span className="text-slate-400 text-xs">
                      {lead.followUpAt ? formatDateTime(lead.followUpAt) : "—"}
                    </span>
                  )}
                </td>

                <td className="whitespace-nowrap text-slate-500">{formatDateTime(lead.created)}</td>

                <td>
                  {lead.status !== "CONVERTED" && (
                    <button
                      onClick={() => openConvertModal(lead)}
                      className="inline-flex items-center gap-1 whitespace-nowrap rounded-lg bg-emerald-600 px-3 py-1.5 text-xs font-semibold text-white hover:bg-emerald-700"
                    >
                      Convert <FiArrowRight size={12} />
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* ── PAGINATION ── */}
      {pageData && (
        <div className="flex flex-col sm:flex-row items-center justify-between gap-3 mt-6">
          <button
            disabled={page === 0}
            onClick={() => setPage(page - 1)}
            className="inline-flex w-full items-center justify-center gap-1 rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50 sm:w-auto"
          >
            <FiChevronLeft size={14} /> Previous
          </button>
          <div className="text-sm font-medium text-slate-700">
            Page <span className="font-bold">{page + 1}</span> of <span className="font-bold">{pageData.totalPages}</span>
          </div>
          <button
            disabled={page + 1 >= pageData.totalPages}
            onClick={() => setPage(page + 1)}
            className="inline-flex w-full items-center justify-center gap-1 rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50 sm:w-auto"
          >
            Next <FiChevronRight size={14} />
          </button>
        </div>
      )}

      {/* ── CONVERT MODAL ── */}
      {selectedLead && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm flex items-end sm:items-center justify-center z-50 p-0 sm:p-4 animate-fade-in">
          <div className="bg-white rounded-t-2xl sm:rounded-2xl shadow-2xl w-full sm:max-w-md p-6 max-h-[90vh] overflow-y-auto animate-pop-in">
            <h2 className="m-0 mb-5 text-lg font-semibold text-slate-900">Convert Lead</h2>
            <div className="space-y-4">
              <div>
                <label className="text-sm font-medium text-slate-700">Customer</label>
                <div className="font-semibold">{selectedLead.name}</div>
              </div>
              <div>
                <label className="text-sm font-medium text-slate-700">Product</label>
                <div className="space-y-3 mt-1">
                  {conversionItems.map((item, index) => (
                    <div key={`${item.productId || item.productName}-${index}`} className="rounded-lg border border-slate-200 p-3">
                      <div className="flex items-start justify-between gap-2">
                        <div className="min-w-0">
                          <div className="font-medium truncate">{item.productName}</div>
                          <div className="text-sm text-slate-500">
                            KES {Number(item.sellingPrice).toLocaleString()} each
                          </div>
                        </div>
                        {conversionItems.length > 1 && (
                          <button
                            type="button"
                            onClick={() => setConversionItems((items) => items.filter((_, itemIndex) => itemIndex !== index))}
                            className="text-sm text-red-600 hover:text-red-700"
                            aria-label={`Remove ${item.productName}`}
                          >
                            Remove
                          </button>
                        )}
                      </div>
                      <label className="block text-xs text-slate-600 mt-2">Quantity</label>
                      <input
                        type="number"
                        min="1"
                        value={item.quantity}
                        onChange={(e) => setConversionItems((items) => items.map((line, lineIndex) =>
                          lineIndex === index ? { ...line, quantity: e.target.value } : line
                        ))}
                        className="w-full rounded-[10px] border border-slate-200 bg-white px-3 py-2 mt-1.5 text-sm"
                      />
                    </div>
                  ))}
                </div>
                <div className="flex flex-wrap gap-2 mt-3">
                  <button
                    type="button"
                    onClick={() => openProductPicker("change")}
                    className="px-3 py-2 border border-green-600 text-green-700 rounded-lg text-sm font-medium hover:bg-green-50"
                  >
                    Change Product
                  </button>
                  <button
                    type="button"
                    onClick={() => openProductPicker("add")}
                    className="px-3 py-2 bg-green-600 text-white rounded-lg text-sm font-medium hover:bg-green-700"
                  >
                    Add Product
                  </button>
                </div>
                {productPickerMode && (
                  <div className="mt-3 rounded-lg bg-slate-50 p-3">
                    <label className="text-sm font-medium text-slate-700">
                      {productPickerMode === "change" ? "Choose replacement product" : "Choose product to add"}
                    </label>
                    <input
                      type="search"
                      value={productSearch}
                      onChange={(e) => {
                        setProductSearch(e.target.value);
                        setPickerProductId("");
                      }}
                      placeholder="Type a product name or SKU..."
                      aria-label="Search products"
                      className="w-full border rounded p-2 mt-1 bg-white"
                    />
                    <div className="mt-2 max-h-48 overflow-y-auto rounded border bg-white" role="listbox" aria-label="Matching products">
                      {productSearchLoading ? (
                        <div className="px-3 py-4 text-sm text-slate-500" role="status">Searching products...</div>
                      ) : productsForConversion.length ? productsForConversion.map((product) => (
                        <button
                          key={product.id}
                          type="button"
                          role="option"
                          aria-selected={String(pickerProductId) === String(product.id)}
                          onClick={() => setPickerProductId(String(product.id))}
                          className={`block w-full border-b last:border-b-0 px-3 py-2 text-left text-sm hover:bg-green-50 ${String(pickerProductId) === String(product.id) ? "bg-green-100" : ""}`}
                        >
                          <span className="block font-medium">{product.name}</span>
                          <span className="text-xs text-slate-500">
                            {product.sku ? `${product.sku} · ` : ""}KES {Number(product.sellingPrice).toLocaleString()}
                          </span>
                        </button>
                      )) : (
                        <div className="px-3 py-4 text-sm text-slate-500" role="status">
                          {productSearch.trim() ? "No matching products found." : "No products available."}
                        </div>
                      )}
                    </div>
                    <div className="flex justify-end gap-2 mt-2">
                      <button
                        type="button"
                        onClick={() => { setProductPickerMode(""); setProductSearch(""); setPickerProductId(""); }}
                        className="px-3 py-1.5 border rounded text-sm"
                      >
                        Cancel
                      </button>
                      <button
                        type="button"
                        onClick={applyPickedProduct}
                        disabled={!pickerProductId}
                        className="px-3 py-1.5 bg-green-600 text-white rounded text-sm disabled:opacity-50"
                      >
                        {productPickerMode === "change" ? "Change" : "Add"}
                      </button>
                    </div>
                  </div>
                )}
              </div>
              <div>
                <label className="text-sm font-medium text-slate-700">Delivery Fee</label>
                <input type="number" min="0" value={deliveryFee}
                  onChange={(e) => setDeliveryFee(e.target.value)}
                  className="w-full rounded-[10px] border border-slate-200 bg-white px-3 py-2 mt-1.5 text-sm" />
              </div>
              <div>
                <label className="text-sm font-medium text-slate-700">Discount</label>
                <input type="number" min="0" value={discount}
                  onChange={(e) => setDiscount(e.target.value)}
                  className="w-full rounded-[10px] border border-slate-200 bg-white px-3 py-2 mt-1.5 text-sm" />
              </div>
              <div>
                <label className="text-sm font-medium text-slate-700">Customer Notes</label>
                <textarea
                  value={customerNotes}
                  onChange={(e) => setCustomerNotes(e.target.value)}
                  rows={4}
                  className="w-full border rounded p-2 mt-1 resize-none"
                  placeholder="Add what the customer said..."
                />
              </div>
              <div>
                <label className="text-sm font-medium text-slate-700">
                  Total (KES {subtotal.toLocaleString()} products + KES {Number(safeDeliveryFee).toLocaleString()} delivery − KES {Number(safeDiscount).toLocaleString()} discount)
                </label>
                <div className="text-lg font-bold text-green-600">
                  KES {totalAmount.toLocaleString()}
                </div>
              </div>
              <div>
                <label className="text-sm font-medium text-slate-700">Delivery Location</label>
                <input type="text" value={deliveryCity}
                  onChange={(e) => setDeliveryCity(e.target.value)}
                  className="w-full rounded-[10px] border border-slate-200 bg-white px-3 py-2 mt-1.5 text-sm" />
              </div>
              <div>
                <label className="text-sm font-medium text-slate-700">Courier</label>
                <select
                  value={selectedCourierId}
                  onChange={(e) => setSelectedCourierId(e.target.value)}
                  className="w-full rounded-[10px] border border-slate-200 bg-white px-3 py-2 mt-1.5 text-sm"
                >
                  <option value="">No courier (assign later)</option>
                  {couriers.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name} — {c.phoneNumber}
                    </option>
                  ))}
                </select>
                {couriers.length === 0 && (
                  <div className="text-xs text-slate-400 mt-1">
                    No active couriers found. Add one under the Couriers page.
                  </div>
                )}
              </div>
            </div>
            <div className="flex flex-col-reverse sm:flex-row justify-end gap-3 mt-6">
              <button onClick={() => { setSelectedLead(null); setSelectedCourierId(""); }}
                className="px-4 py-2.5 border border-slate-200 rounded-[10px] text-sm font-semibold text-slate-700 hover:bg-slate-50">
                Cancel
              </button>
              <button onClick={convertLead}
                disabled={converting || conversionItems.length === 0}
                className="px-4 py-2.5 bg-brand-600 text-white rounded-[10px] text-sm font-semibold hover:bg-brand-700">
                {converting ? "Converting..." : "Confirm Convert"}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ── ADD LEAD MODAL ── */}
      {showAddLeadModal && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm flex items-end sm:items-center justify-center z-50 p-0 sm:p-4 animate-fade-in">
          <div className="bg-white rounded-t-2xl sm:rounded-2xl shadow-2xl w-full sm:max-w-md p-6 max-h-[90vh] overflow-y-auto animate-pop-in">
            <h2 className="m-0 mb-5 text-lg font-semibold text-slate-900">Add Lead</h2>
            <div className="space-y-3">
              <div>
                <label className="text-sm font-medium text-slate-700">Source</label>
                <select
                  value={newLead.source}
                  onChange={(e) => setNewLead((p) => ({ ...p, source: e.target.value }))}
                  className="w-full rounded-[10px] border border-slate-200 bg-white px-3 py-2 mt-1.5 text-sm"
                >
                  <option value="CALL">Phone call</option>
                  <option value="WHATSAPP">WhatsApp</option>
                </select>
              </div>
              <div>
                <label className="text-sm font-medium text-slate-700">Name *</label>
                <input type="text" value={newLead.name}
                  onChange={(e) => setNewLead((p) => ({ ...p, name: e.target.value }))}
                  className="w-full rounded-[10px] border border-slate-200 bg-white px-3 py-2 mt-1.5 text-sm" />
              </div>
              <div>
                <label className="text-sm font-medium text-slate-700">Phone Number *</label>
                <input type="text" value={newLead.phoneNumber}
                  onChange={(e) => setNewLead((p) => ({ ...p, phoneNumber: e.target.value }))}
                  className="w-full rounded-[10px] border border-slate-200 bg-white px-3 py-2 mt-1.5 text-sm" />
              </div>
              <div>
                <label className="text-sm font-medium text-slate-700">Alternative Phone</label>
                <input type="text" value={newLead.alternativePhoneNumber}
                  onChange={(e) => setNewLead((p) => ({ ...p, alternativePhoneNumber: e.target.value }))}
                  className="w-full rounded-[10px] border border-slate-200 bg-white px-3 py-2 mt-1.5 text-sm" />
              </div>
              <div>
                <label className="text-sm font-medium text-slate-700">Product</label>
                <select
                  value={newLead.formName}
                  onChange={(e) => setNewLead((p) => ({ ...p, formName: e.target.value }))}
                  className="w-full border rounded p-2 mt-1 bg-white"
                  disabled={loadingLeadProducts}
                >
                  <option value="">
                    {loadingLeadProducts ? "Loading products..." : "Select a product (optional)"}
                  </option>
                  {leadProducts.map((product) => (
                    <option key={product.id} value={product.name}>
                      {product.name}{product.sku ? ` (${product.sku})` : ""}
                    </option>
                  ))}
                </select>
                {leadProductsError && (
                  <div className="text-xs text-red-600 mt-1">{leadProductsError}</div>
                )}
              </div>
              <div>
                <label className="text-sm font-medium text-slate-700">Location</label>
                <input type="text" value={newLead.location}
                  onChange={(e) => setNewLead((p) => ({ ...p, location: e.target.value }))}
                  className="w-full rounded-[10px] border border-slate-200 bg-white px-3 py-2 mt-1.5 text-sm" />
              </div>
              <div>
                <label className="text-sm font-medium text-slate-700">Additional Info</label>
                <textarea rows={2} value={newLead.additionalInfo}
                  onChange={(e) => setNewLead((p) => ({ ...p, additionalInfo: e.target.value }))}
                  className="w-full rounded-[10px] border border-slate-200 bg-white px-3 py-2 mt-1.5 text-sm" />
              </div>
              <div>
                <label className="text-sm font-medium text-slate-700">Notes</label>
                <textarea rows={2} value={newLead.notes}
                  onChange={(e) => setNewLead((p) => ({ ...p, notes: e.target.value }))}
                  className="w-full rounded-[10px] border border-slate-200 bg-white px-3 py-2 mt-1.5 text-sm" />
              </div>
            </div>
            <div className="flex flex-col-reverse sm:flex-row justify-end gap-3 mt-6">
              <button onClick={() => setShowAddLeadModal(false)}
                className="px-4 py-2.5 border border-slate-200 rounded-[10px] text-sm font-semibold text-slate-700 hover:bg-slate-50">
                Cancel
              </button>
              <button onClick={createManualLead} disabled={savingLead}
                className="px-4 py-2.5 bg-brand-600 text-white rounded-[10px] text-sm font-semibold hover:bg-brand-700">
                {savingLead ? "Saving..." : "Add Lead"}
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
}

// ─── Helpers ───────────────────────────────────────────────────────────────────
const statTones = {
  slate: "bg-slate-100 text-slate-600",
  blue: "bg-sky-50 text-sky-600",
  green: "bg-emerald-50 text-emerald-600",
  violet: "bg-violet-50 text-violet-600",
  red: "bg-rose-50 text-rose-600",
  amber: "bg-amber-50 text-amber-600",
};

function StatCard({ title, value, icon: Icon, tone = "slate" }) {
  return (
    <div className="ui-card p-4 transition-shadow hover:shadow-md">
      <div className="flex items-center justify-between gap-2">
        <div className="text-slate-500 text-xs sm:text-sm font-medium">{title}</div>
        {Icon && (
          <span className={`grid h-8 w-8 place-items-center rounded-lg ${statTones[tone]}`}>
            <Icon size={15} />
          </span>
        )}
      </div>
      <div className="text-xl sm:text-2xl font-bold tracking-tight text-slate-900 mt-1.5">{value}</div>
    </div>
  );
}

const formatDateTime = (dateString) => {
  if (!dateString) return "";
  const date = new Date(dateString);
  return `${date.toLocaleDateString("en-CA")} ${date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}`;
};

const formatPhoneLink = (value) => {
  if (!value) return "";
  const cleaned = String(value).trim().replace(/[^\d+]/g, "");
  return cleaned ? `tel:${cleaned}` : "";
};

const formatProductOptions = (productOptionsJson) => {
  if (!productOptionsJson) return null;
  let parsed;
  try {
    parsed = JSON.parse(productOptionsJson);
  } catch {
    return null;
  }
  if (!parsed || Object.keys(parsed).length === 0) return null;

  const parts = [];
  for (const [key, value] of Object.entries(parsed)) {
    const lowerKey = key.toLowerCase();
    const label = lowerKey.includes("color")
      ? "Colors"
      : lowerKey.includes("size")
      ? "Size"
      : key;
    const displayValue = Array.isArray(value) ? value.join(", ") : value;
    if (displayValue) parts.push(`${label}: ${displayValue}`);
  }
  return parts.length > 0 ? parts.join(" · ") : null;
};
