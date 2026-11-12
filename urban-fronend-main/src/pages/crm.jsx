import React, { useEffect, useState, useRef, useCallback } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { api } from "../api/api";
import { loadProducts as fetchProductCatalog } from "../api/referenceData";
import { publishNotification } from "../utils/notificationBus.js";
import {
  FiPlus,
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
import { Toast } from "./crm/Toast";
import { StatCard } from "./crm/StatCard";
import { LeadMobileList, LeadTable } from "./crm/LeadList";
import { ConvertLeadModal } from "./crm/ConvertLeadModal";
import { AddLeadModal } from "./crm/AddLeadModal";

// ─── Main CRM Component ────────────────────────────────────────────────────────
export default function CRM() {
  const navigate = useNavigate();
  const { id: leadId } = useParams();

  const [leads, setLeads] = useState([]);
  const [pageData, setPageData] = useState(null);
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
      setError("");
      const res = await api.get("/api/v1/leads", {
        params: { page, size, status: statusFilter || undefined },
      });
      setLeads(res.data.content);
      setPageData(res.data);
    } catch (err) {
      console.error(err);
      setError("Failed to load leads");
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

  const registerLeadRef = (id, el) => {
    if (el) leadRefs.current[id] = el;
  };

  const handleNoteFocus = (id) => setFocusedNoteId(id);

  const handleNoteBlur = (id) =>
    setTimeout(() => setFocusedNoteId((prev) => (prev === id ? null : prev)), 100);

  const handleNoteChange = (id, value) =>
    setNoteEdits((prev) => ({ ...prev, [id]: value }));

  const handleSaveNote = async (lead) => {
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
  };

  const handleStatusChange = (lead, newStatus) => {
    if (newStatus === "RESCHEDULED") {
      // Just mark in local state — date picker will appear
      setFollowUps((prev) => ({
        ...prev,
        [lead.id]: prev[lead.id] || "",
      }));
    } else {
      updateLeadStatus(lead.id, newStatus);
    }
  };

  const handleFollowUpChange = (id, value) =>
    setFollowUps((prev) => ({ ...prev, [id]: value }));

  const handleSaveFollowUp = (id) => updateLeadStatus(id, "RESCHEDULED");

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

      <LeadMobileList
        leads={leads}
        highlightedLeadId={highlightedLeadId}
        registerLeadRef={registerLeadRef}
        noteEdits={noteEdits}
        focusedNoteId={focusedNoteId}
        followUps={followUps}
        onNoteFocus={handleNoteFocus}
        onNoteBlur={handleNoteBlur}
        onNoteChange={handleNoteChange}
        onSaveNote={handleSaveNote}
        onStatusChange={handleStatusChange}
        onFollowUpChange={handleFollowUpChange}
        onSaveFollowUp={handleSaveFollowUp}
        onConvert={openConvertModal}
      />

      <LeadTable
        leads={leads}
        highlightedLeadId={highlightedLeadId}
        registerLeadRef={registerLeadRef}
        noteEdits={noteEdits}
        focusedNoteId={focusedNoteId}
        followUps={followUps}
        onNoteFocus={handleNoteFocus}
        onNoteBlur={handleNoteBlur}
        onNoteChange={handleNoteChange}
        onSaveNote={handleSaveNote}
        onStatusChange={handleStatusChange}
        onFollowUpChange={handleFollowUpChange}
        onSaveFollowUp={handleSaveFollowUp}
        onConvert={openConvertModal}
      />

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

      {selectedLead && (
        <ConvertLeadModal
          selectedLead={selectedLead}
          conversionItems={conversionItems}
          setConversionItems={setConversionItems}
          productPickerMode={productPickerMode}
          productSearch={productSearch}
          pickerProductId={pickerProductId}
          productsForConversion={productsForConversion}
          productSearchLoading={productSearchLoading}
          deliveryFee={deliveryFee}
          discount={discount}
          customerNotes={customerNotes}
          deliveryCity={deliveryCity}
          selectedCourierId={selectedCourierId}
          couriers={couriers}
          subtotal={subtotal}
          safeDeliveryFee={safeDeliveryFee}
          safeDiscount={safeDiscount}
          totalAmount={totalAmount}
          converting={converting}
          onOpenProductPicker={openProductPicker}
          onProductSearchChange={(value) => {
            setProductSearch(value);
            setPickerProductId("");
          }}
          onPickerSelect={setPickerProductId}
          onApplyPickedProduct={applyPickedProduct}
          onCancelPicker={() => {
            setProductPickerMode("");
            setProductSearch("");
            setPickerProductId("");
          }}
          onDeliveryFeeChange={setDeliveryFee}
          onDiscountChange={setDiscount}
          onCustomerNotesChange={setCustomerNotes}
          onDeliveryCityChange={setDeliveryCity}
          onCourierChange={setSelectedCourierId}
          onConvert={convertLead}
          onClose={() => {
            setSelectedLead(null);
            setSelectedCourierId("");
          }}
        />
      )}

      {showAddLeadModal && (
        <AddLeadModal
          newLead={newLead}
          setNewLead={setNewLead}
          leadProducts={leadProducts}
          loadingLeadProducts={loadingLeadProducts}
          leadProductsError={leadProductsError}
          savingLead={savingLead}
          onCreate={createManualLead}
          onClose={() => setShowAddLeadModal(false)}
        />
      )}
    </div>
  );
}

