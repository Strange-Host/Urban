import { api } from "./api";

export const STICKER_LAYOUTS = ["A4", "ROLL"];
const LAYOUT_KEY = "stickerLayout";

export function getSavedLayout() {
  try {
    const saved = localStorage.getItem(LAYOUT_KEY);
    return STICKER_LAYOUTS.includes(saved) ? saved : "A4";
  } catch {
    return "A4";
  }
}

export function saveLayout(layout) {
  try {
    localStorage.setItem(LAYOUT_KEY, layout);
  } catch {
    // ignore storage errors (private mode etc.)
  }
}

function toPdfResult(res) {
  return {
    blob: new Blob([res.data], { type: "application/pdf" }),
    batchId: res.headers["x-sticker-batch-id"] || null,
  };
}

// Restock inventory and get stickers for the new units. Returns { blob, batchId }.
export async function restockWithStickers({ productId, quantity, reorderLevel }, layout = "A4") {
  const res = await api.post(
    "/api/v1/stickers/restock",
    { productId, quantity, reorderLevel },
    { params: { layout }, responseType: "blob" }
  );
  return toPdfResult(res);
}

// Stickers for stock already on the shelf (does not change inventory).
export async function printProductStickers(productId, quantity, layout = "A4") {
  const res = await api.post(`/api/v1/stickers/products/${productId}`, null, {
    params: { quantity, layout },
    responseType: "blob",
  });
  return toPdfResult(res);
}

export async function reprintBatch(batchId, layout = "A4") {
  const res = await api.get(`/api/v1/stickers/batches/${encodeURIComponent(batchId)}`, {
    params: { layout },
    responseType: "blob",
  });
  return toPdfResult(res);
}

// Backend errors are { title, detail }. With responseType "blob" the body arrives as a Blob.
export async function getErrorDetail(error, fallback = "Something went wrong.") {
  let data = error?.response?.data;
  if (data instanceof Blob) {
    try {
      data = JSON.parse(await data.text());
    } catch {
      data = null;
    }
  }
  return data?.detail || data?.message || fallback;
}
