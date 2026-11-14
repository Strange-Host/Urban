import { api } from "./api";

export async function fetchSettings() {
  const res = await api.get("/api/v1/settings");
  return res.data;
}

export async function updateMpesaPaybill(mpesaPaybill) {
  const res = await api.put("/api/v1/settings/mpesa-paybill", { mpesaPaybill });
  return res.data;
}
