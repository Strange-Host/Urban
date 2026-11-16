const toNumber = (value) => {
  const n = Number(value);
  return Number.isFinite(n) ? n : 0;
};

/**
 * What the customer is charged. A discount comes off the goods; the delivery fee is added
 * on top and only applies to delivered (ONLINE) orders.
 */
export function orderTotals(cartTotal, discount, deliveryFee, orderType) {
  const goods = toNumber(cartTotal);
  const discountAmount = toNumber(discount);
  const fee = orderType === "ONLINE" ? toNumber(deliveryFee) : 0;
  const afterDiscount = Math.max(0, goods - discountAmount);

  return {
    discount: discountAmount,
    deliveryFee: fee,
    grandTotal: Math.round((afterDiscount + fee) * 100) / 100,
  };
}
