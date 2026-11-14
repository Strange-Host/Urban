// Single source of truth for shop-level settings used by the UI.
//
// The M-Pesa paybill is configured per deployment (VITE_MPESA_PAYBILL) until
// it moves into server-side settings. It is deliberately NOT hard-coded: a
// wrong default on a printed receipt sends customer money to the wrong place.
export function getMpesaPaybill() {
  const value = String(import.meta.env.VITE_MPESA_PAYBILL ?? "").trim();
  return /^\d{4,10}$/.test(value) ? value : "";
}
