import { api } from "./api";

export async function fetchOrders({ page = 0, size = 10, sort = "createdAt,desc", status }) {
  const res = await api.get("/api/v1/orders", {
    params: { page, size, sort, status: status || undefined },
  });

  return res.data; // Spring Page<Order>
}

// Scan a sticker barcode to pack it into an order. orderId is optional; without it
// the backend assigns the unit to the oldest awaiting order that needs the product.
export async function scanBarcode(barcode, orderId) {
  const payload = { barcode };
  if (orderId) payload.orderId = orderId;
  const res = await api.post("/api/v1/orders/scan", payload);
  return res.data;
}

export async function undoScan(serial) {
  await api.delete(`/api/v1/orders/scan/${encodeURIComponent(serial)}`);
}
