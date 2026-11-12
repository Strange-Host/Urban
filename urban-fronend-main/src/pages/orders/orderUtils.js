export const MPESA_PAYBILL = "522522";

export const ORDER_STATUSES = [
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

export function formatMoney(value) {
  return `KES ${Number(value || 0).toLocaleString()}`;
}

export function formatDateTime(dateString) {
  if (!dateString) return "-";

  const date = new Date(dateString);

  return `${date.toLocaleDateString("en-CA")} ${date.toLocaleTimeString([], {
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  })}`;
}

export function statusStyle(status) {
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

export function niceStatus(status) {
  return String(status || "")
    .replaceAll("_", " ")
    .toLowerCase()
    .replace(/\b\w/g, (c) => c.toUpperCase());
}

// Return a color hex for highlighting rows/cards based on age and status
export function getParcelAgeHighlight(order) {
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

export function previewText(text, maxLength = 60) {
  if (!text) return "No delivery notes";
  return text.length <= maxLength ? text : `${text.slice(0, maxLength)}…`;
}

export function getOrderProductLines(order) {
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
