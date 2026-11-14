import React, { useCallback, useEffect, useRef, useState } from "react";
import {
  FiMaximize,
  FiCheckCircle,
  FiXCircle,
  FiTruck,
  FiRotateCcw,
  FiRefreshCw,
  FiLock,
} from "react-icons/fi";
import { PageHeader, EmptyState, Spinner } from "../components/ui";
import { labelStyle, inputStyle, actionBtn, tableCard, badgeStyle } from "../components/styles";
import PermissionGate from "../auth/PermissionGate";
import { fetchOrders, scanBarcode, undoScan } from "../api/ordersApi";
import { getErrorDetail } from "../api/stickersApi";
import { useAuth } from "../auth/useAuth";
import { printReceiptToUSB } from "../utils/printer";
import { loadMpesaPaybill } from "../utils/settings";


const DUPLICATE_WINDOW_MS = 500;
const monoFont = "ui-monospace, SFMono-Regular, Menlo, monospace";

// ---- Beeps (Web Audio API) ----
let audioCtx = null;

function tone(freq, start, duration, type = "sine", volume = 0.2) {
  const ctx = audioCtx;
  const osc = ctx.createOscillator();
  const gain = ctx.createGain();
  osc.type = type;
  osc.frequency.value = freq;
  gain.gain.setValueAtTime(volume, ctx.currentTime + start);
  gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + start + duration);
  osc.connect(gain).connect(ctx.destination);
  osc.start(ctx.currentTime + start);
  osc.stop(ctx.currentTime + start + duration);
}

function playSound(kind) {
  try {
    const Ctx = window.AudioContext || window.webkitAudioContext;
    if (!Ctx) return;
    if (!audioCtx) audioCtx = new Ctx();
    if (audioCtx.state === "suspended") audioCtx.resume();

    if (kind === "success") {
      tone(1200, 0, 0.1);
    } else if (kind === "dispatched") {
      tone(660, 0, 0.12);
      tone(880, 0.12, 0.12);
      tone(1320, 0.24, 0.3);
    } else {
      tone(220, 0, 0.18, "square", 0.15);
      tone(180, 0.22, 0.3, "square", 0.15);
    }
  } catch {
    // audio is best-effort
  }
}

function niceStatus(status = "") {
  return status
    .toLowerCase()
    .split("_")
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join(" ");
}

function statusTone(status = "") {
  if (status.endsWith("_PAID")) return "green";
  if (status.endsWith("_PARTIAL")) return "amber";
  if (status.endsWith("_UNPAID")) return "red";
  return "blue";
}

function formatTime(date) {
  return date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", second: "2-digit" });
}

export default function DispatchScanner() {
  return (
    <PermissionGate
      permission="CAN_UPDATE_ORDER_STATUS"
      fallback={
        <div style={tableCard}>
          <EmptyState
            icon={FiLock}
            title="Access restricted"
            message="You need permission to update order status to use the dispatch scanner."
          />
        </div>
      }
    >
      <ScannerView />
    </PermissionGate>
  );
}

function ScannerView() {
  const { user } = useAuth();
  const inputRef = useRef(null);
  const lastScanRef = useRef({ code: "", at: 0 });
  const nextIdRef = useRef(1);

  const [value, setValue] = useState("");
  const [pending, setPending] = useState(0);

  const [awaitingOrders, setAwaitingOrders] = useState([]);
  const [ordersLoading, setOrdersLoading] = useState(false);
  const [selectedOrderId, setSelectedOrderId] = useState("");

  // orderId -> { accountNumber, unitsScanned, unitsRequired, orderStatus, dispatched }
  const [progress, setProgress] = useState({});
  // { kind: "progress" | "dispatched" | "error", orderId?, message }
  const [banner, setBanner] = useState(null);
  const [history, setHistory] = useState([]);

  const focusInput = useCallback(() => {
    setTimeout(() => inputRef.current?.focus(), 0);
  }, []);

  // Print the dispatch receipt for an order that was just dispatched unpaid.
  async function printDispatchReceipt(res) {
    let order = awaitingOrders.find((o) => o.id === res.orderId);

    if (!order) {
      // Not in the local list (e.g. list was stale) — look it up among dispatched-unpaid orders.
      try {
        const page = await fetchOrders({ page: 0, size: 50, sort: "updatedAt,desc", status: res.orderStatus });
        order = (page.content || []).find((o) => o.id === res.orderId);
      } catch (e) {
        console.warn("Could not load dispatched order for receipt", e);
      }
    }

    const name = user?.name || user?.email || "-";
    const servedBy = user?.phoneNumber ? `${name} · ${user.phoneNumber}` : name;

    try {
      await printReceiptToUSB(
        {
          ...(order || { id: res.orderId, accountNumber: res.accountNumber }),
          orderStatus: res.orderStatus,
          servedBy,
        },
        await loadMpesaPaybill()
      );
    } catch (e) {
      console.error("Receipt print failed:", e);
    }
  }

  const loadAwaitingOrders = useCallback(async () => {
    try {
      setOrdersLoading(true);
      const page = await fetchOrders({ page: 0, size: 200, sort: "createdAt,asc", status: "AWAITING_DISPATCH" });
      setAwaitingOrders(page.content || []);
    } catch (e) {
      console.log(e);
    } finally {
      setOrdersLoading(false);
    }
  }, []);

  useEffect(() => {
    loadAwaitingOrders();
  }, [loadAwaitingOrders]);

  // Keep the scanner input focused: on load, after clicks, and when the window regains focus.
  useEffect(() => {
    focusInput();

    function onClick(e) {
      // Let form controls (e.g. the order selector) keep focus while in use.
      if (e.target.closest("select, input, textarea")) return;
      focusInput();
    }

    window.addEventListener("focus", focusInput);
    document.addEventListener("click", onClick);
    return () => {
      window.removeEventListener("focus", focusInput);
      document.removeEventListener("click", onClick);
    };
  }, [focusInput]);

  async function submitScan(code) {
    const orderId = selectedOrderId || undefined;
    const id = nextIdRef.current++;
    setPending((n) => n + 1);

    try {
      const res = await scanBarcode(code, orderId);

      setProgress((prev) => ({
        ...prev,
        [res.orderId]: {
          accountNumber: res.accountNumber,
          unitsScanned: res.unitsScanned,
          unitsRequired: res.unitsRequired,
          orderStatus: res.orderStatus,
          dispatched: res.orderDispatched,
        },
      }));

      setHistory((prev) => [
        {
          id,
          time: new Date(),
          serial: res.serial || code,
          productName: res.productName,
          orderId: res.orderId,
          accountNumber: res.accountNumber,
          message: res.message,
          ok: true,
          undone: false,
        },
        ...prev,
      ]);

      if (res.orderDispatched) {
        setBanner({ kind: "dispatched", orderId: res.orderId });
        playSound("dispatched");
        if (res.orderStatus === "DISPATCHED_UNPAID") {
          printDispatchReceipt(res);
        }
        setAwaitingOrders((prev) => prev.filter((o) => o.id !== res.orderId));
        setSelectedOrderId((cur) => (cur === res.orderId ? "" : cur));
      } else {
        setBanner({ kind: "progress", orderId: res.orderId });
        playSound("success");
      }
    } catch (e) {
      const detail = await getErrorDetail(e, "Scan failed.");
      setBanner({ kind: "error", message: detail, serial: code });
      playSound("error");
      setHistory((prev) => [
        { id, time: new Date(), serial: code, message: detail, ok: false },
        ...prev,
      ]);
    } finally {
      setPending((n) => n - 1);
      focusInput();
    }
  }

  function handleKeyDown(e) {
    if (e.key !== "Enter") return;
    e.preventDefault();

    const code = value.trim();
    setValue("");
    if (!code) return;

    const now = Date.now();
    const last = lastScanRef.current;
    if (last.code === code && now - last.at < DUPLICATE_WINDOW_MS) return;
    lastScanRef.current = { code, at: now };

    submitScan(code);
  }

  async function handleUndo(row) {
    setHistory((prev) => prev.map((h) => (h.id === row.id ? { ...h, undoing: true } : h)));
    try {
      await undoScan(row.serial);
      setHistory((prev) => prev.map((h) => (h.id === row.id ? { ...h, undoing: false, undone: true } : h)));
      setProgress((prev) => {
        const p = prev[row.orderId];
        if (!p) return prev;
        return { ...prev, [row.orderId]: { ...p, unitsScanned: Math.max(0, p.unitsScanned - 1) } };
      });
      setBanner({ kind: "progress", orderId: row.orderId });
    } catch (e) {
      setHistory((prev) => prev.map((h) => (h.id === row.id ? { ...h, undoing: false } : h)));
      const detail = await getErrorDetail(e, "Undo failed.");
      setBanner({ kind: "error", message: detail, serial: row.serial });
      playSound("error");
    } finally {
      focusInput();
    }
  }

  const bannerOrder = banner?.orderId ? progress[banner.orderId] : null;

  return (
    <div>
      <PageHeader
        title="Dispatch Scanner"
        subtitle="Scan sticker barcodes to pack orders. Orders are dispatched automatically once fully packed."
      />

      {/* Scanner input */}
      <div style={{ ...tableCard, padding: 20, overflow: "visible", marginBottom: 16 }}>
        <label style={labelStyle} htmlFor="scanner-input">
          Scan barcode
        </label>
        <div style={{ position: "relative" }}>
          <FiMaximize
            size={22}
            style={{ position: "absolute", left: 16, top: "50%", transform: "translateY(-50%)", color: "var(--brand)" }}
          />
          <input
            id="scanner-input"
            ref={inputRef}
            value={value}
            onChange={(e) => setValue(e.target.value)}
            onKeyDown={handleKeyDown}
            onBlur={(e) => {
              // Focus fell to nothing (e.g. a disabled element); clicks are handled above.
              if (!e.relatedTarget) focusInput();
            }}
            placeholder="Waiting for scan…"
            autoComplete="off"
            autoCorrect="off"
            autoCapitalize="off"
            spellCheck={false}
            style={{
              ...inputStyle,
              height: 64,
              paddingLeft: 52,
              paddingRight: 52,
              fontSize: 24,
              fontFamily: monoFont,
              letterSpacing: 1,
            }}
          />
          {pending > 0 && (
            <span style={{ position: "absolute", right: 18, top: "50%", transform: "translateY(-50%)", color: "var(--brand)" }}>
              <Spinner />
            </span>
          )}
        </div>

        <div style={{ display: "flex", gap: 10, alignItems: "flex-end", marginTop: 14, flexWrap: "wrap" }}>
          <div style={{ flex: "1 1 260px" }}>
            <label style={labelStyle} htmlFor="scanner-order">
              Packing a specific order (optional)
            </label>
            <select
              id="scanner-order"
              value={selectedOrderId}
              onChange={(e) => {
                setSelectedOrderId(e.target.value);
                focusInput();
              }}
              style={inputStyle}
            >
              <option value="">Auto-assign to oldest awaiting order</option>
              {awaitingOrders.map((o) => (
                <option key={o.id} value={o.id}>
                  {o.accountNumber || o.id}
                  {o.customerName ? ` — ${o.customerName}` : ""}
                </option>
              ))}
            </select>
          </div>
          <button onClick={loadAwaitingOrders} style={{ ...actionBtn, height: 42 }} disabled={ordersLoading}>
            {ordersLoading ? <Spinner /> : <FiRefreshCw size={13} />} Refresh orders
          </button>
        </div>
      </div>

      {/* Result banner */}
      {banner?.kind === "error" && (
        <div
          role="alert"
          style={{
            display: "flex",
            gap: 12,
            alignItems: "center",
            padding: "16px 18px",
            borderRadius: 14,
            background: "#fef2f2",
            border: "1px solid #fecaca",
            color: "#b91c1c",
            marginBottom: 16,
          }}
        >
          <FiXCircle size={28} style={{ flexShrink: 0 }} />
          <div>
            <div style={{ fontWeight: 700, fontSize: 16 }}>{banner.message}</div>
            {banner.serial && <div style={{ fontSize: 13, fontFamily: monoFont, marginTop: 2 }}>{banner.serial}</div>}
          </div>
        </div>
      )}

      {banner?.kind === "dispatched" && bannerOrder && (
        <div
          role="status"
          style={{
            display: "flex",
            gap: 16,
            alignItems: "center",
            padding: "22px 22px",
            borderRadius: 14,
            background: "var(--success)",
            color: "#fff",
            marginBottom: 16,
            boxShadow: "var(--shadow-sm)",
          }}
        >
          <FiTruck size={40} style={{ flexShrink: 0 }} />
          <div style={{ minWidth: 0 }}>
            <div style={{ fontWeight: 800, fontSize: 24, lineHeight: 1.2 }}>Order dispatched</div>
            <div style={{ fontSize: 15, marginTop: 4 }}>
              <b>{bannerOrder.accountNumber}</b> · {bannerOrder.unitsRequired} units packed ·{" "}
              <span style={{ background: "rgba(255,255,255,0.2)", padding: "2px 8px", borderRadius: 999, fontWeight: 600 }}>
                {niceStatus(bannerOrder.orderStatus)}
              </span>
            </div>
          </div>
        </div>
      )}

      {banner?.kind === "progress" && bannerOrder && (
        <div style={{ ...tableCard, padding: 18, marginBottom: 16 }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 12, flexWrap: "wrap" }}>
            <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
              <FiCheckCircle size={22} style={{ color: "var(--success)" }} />
              <div>
                <div style={{ fontSize: 13, color: "var(--text-muted)" }}>Packing order</div>
                <div style={{ fontWeight: 700, fontSize: 17 }}>{bannerOrder.accountNumber}</div>
              </div>
            </div>
            <div style={{ fontWeight: 800, fontSize: 22 }}>
              {bannerOrder.unitsScanned} / {bannerOrder.unitsRequired}{" "}
              <span style={{ fontSize: 14, fontWeight: 600, color: "var(--text-muted)" }}>units packed</span>
            </div>
          </div>
          <div style={{ height: 10, background: "#eef0f5", borderRadius: 999, marginTop: 14, overflow: "hidden" }}>
            <div
              style={{
                height: "100%",
                width: `${bannerOrder.unitsRequired ? Math.min(100, (bannerOrder.unitsScanned / bannerOrder.unitsRequired) * 100) : 0}%`,
                background: "var(--brand)",
                borderRadius: 999,
                transition: "width 0.25s ease",
              }}
            />
          </div>
        </div>
      )}

      {/* Scan history */}
      <div style={tableCard}>
        <div
          style={{
            padding: "14px 16px",
            background: "var(--surface-muted)",
            fontWeight: 700,
            display: "flex",
            justifyContent: "space-between",
          }}
        >
          <span>Scan history</span>
          <span style={{ fontWeight: 500, color: "var(--text-muted)", fontSize: 13 }}>
            {history.filter((h) => h.ok && !h.undone).length} packed this session
          </span>
        </div>

        {history.length === 0 && (
          <EmptyState icon={FiMaximize} title="No scans yet" message="Scan a sticker to start packing orders." />
        )}

        {history.map((h) => {
          const orderState = h.orderId ? progress[h.orderId] : null;
          const canUndo = h.ok && !h.undone && orderState && !orderState.dispatched;

          return (
            <div
              key={h.id}
              className="ui-row"
              style={{
                display: "flex",
                gap: 12,
                alignItems: "center",
                padding: "12px 16px",
                borderTop: "1px solid #f3f4f6",
                fontSize: 14,
                opacity: h.undone ? 0.55 : 1,
              }}
            >
              {h.ok ? (
                <FiCheckCircle size={18} style={{ color: "var(--success)", flexShrink: 0 }} />
              ) : (
                <FiXCircle size={18} style={{ color: "var(--danger)", flexShrink: 0 }} />
              )}

              <div style={{ color: "var(--text-muted)", fontSize: 13, width: 72, flexShrink: 0 }}>{formatTime(h.time)}</div>

              <div style={{ minWidth: 0, flex: 1 }}>
                <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
                  <span style={{ fontFamily: monoFont, fontWeight: 600 }}>{h.serial}</span>
                  {h.productName && <span>{h.productName}</span>}
                  {h.accountNumber && <span style={badgeStyle("indigo")}>{h.accountNumber}</span>}
                  {h.undone && <span style={badgeStyle("gray")}>Undone</span>}
                  {h.ok && !h.undone && orderState?.dispatched && (
                    <span style={badgeStyle(statusTone(orderState.orderStatus))}>{niceStatus(orderState.orderStatus)}</span>
                  )}
                </div>
                <div style={{ fontSize: 13, color: h.ok ? "var(--text-muted)" : "#b91c1c", marginTop: 2 }}>{h.message}</div>
              </div>

              {canUndo && (
                <button onClick={() => handleUndo(h)} style={actionBtn} disabled={h.undoing}>
                  {h.undoing ? <Spinner /> : <FiRotateCcw size={13} />} Undo
                </button>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
