import { api } from "./api";

export async function fetchUnmatchedPayments(page = 0, size = 10) {
  const res = await api.get("/api/v1/payments/unmatched", {
    params: { page, size, sort: "paymentDate,desc" },
  });
  return res.data;
}

/** Orders that could receive a payment, found by account number, customer, phone... */
export async function searchOrdersForPayment(query) {
  const res = await api.get("/api/v1/orders", {
    params: { query: query || undefined, size: 8, sort: "createdAt,desc" },
  });
  return res.data?.content || [];
}

export async function matchPaymentToOrder(paymentId, orderId) {
  const res = await api.post(`/api/v1/payments/${paymentId}/match/${orderId}`);
  return res.data;
}
