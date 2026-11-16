const RED = { bg: "#fee2e2", color: "#991b1b" };
const ORANGE = { bg: "#ffedd5", color: "#b45309" };
const GREEN = { bg: "#dcfce7", color: "#166534" };
const GREY = { bg: "#f3f4f6", color: "#374151" };

/**
 * Colour for a stock figure. When the product has its own reorder level that is what decides
 * "low", so the badge agrees with the Low Stock tab; only products without one fall back
 * to the old fixed thresholds.
 */
export function stockBadgeStyle(quantity, reorderLevel) {
  if (quantity === null || quantity === undefined || quantity === "" || quantity === "-") return GREY;

  const qty = Number(quantity);
  if (Number.isNaN(qty)) return GREY;
  if (qty <= 0) return RED;

  const hasReorderLevel =
    reorderLevel !== null && reorderLevel !== undefined && reorderLevel !== "" && Number.isFinite(Number(reorderLevel));

  if (hasReorderLevel) {
    return qty <= Number(reorderLevel) ? ORANGE : GREEN;
  }

  if (qty <= 5) return RED;
  if (qty <= 15) return ORANGE;
  return GREEN;
}
