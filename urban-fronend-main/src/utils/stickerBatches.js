/** Label for a batch in the reprint list, e.g. "Leather Wallet · 24 stickers · 14 Nov 09:30". */
export function batchLabel(batch, showProduct = true) {
  const date = batch.createdAt
    ? new Date(batch.createdAt).toLocaleString("en-KE", {
        day: "2-digit",
        month: "short",
        hour: "2-digit",
        minute: "2-digit",
        hour12: false,
      })
    : "";
  const units = `${batch.units} sticker${batch.units === 1 ? "" : "s"}`;

  return [showProduct ? batch.productName : null, units, date].filter(Boolean).join(" · ");
}
