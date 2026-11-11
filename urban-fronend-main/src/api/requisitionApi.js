import { api } from "./api";

export async function createRequisition(payload) {
  const res = await api.post("/api/v1/requisitions", payload);
  return res.data;
}

export async function getRequisitions(params) {
  const res = await api.get("/api/v1/requisitions", { params });
  return res.data;
}

export async function approveRequisition(id) {
  const res = await api.patch(`/api/v1/requisitions/${id}/approve`);
  return res.data;
}

export async function rejectRequisition(id, reason) {
  const res = await api.patch(`/api/v1/requisitions/${id}/reject`, { reason });
  return res.data;
}

export async function markPaidRequisition(id) {
  const res = await api.patch(`/api/v1/requisitions/${id}/mark-paid`);
  return res.data;
}
