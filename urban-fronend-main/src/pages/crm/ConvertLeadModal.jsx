import React from "react";

export function ConvertLeadModal({
  selectedLead,
  conversionItems,
  setConversionItems,
  productPickerMode,
  productSearch,
  pickerProductId,
  productsForConversion,
  productSearchLoading,
  deliveryFee,
  discount,
  customerNotes,
  deliveryCity,
  selectedCourierId,
  couriers,
  subtotal,
  safeDeliveryFee,
  safeDiscount,
  totalAmount,
  converting,
  onOpenProductPicker,
  onProductSearchChange,
  onPickerSelect,
  onApplyPickedProduct,
  onCancelPicker,
  onDeliveryFeeChange,
  onDiscountChange,
  onCustomerNotesChange,
  onDeliveryCityChange,
  onCourierChange,
  onConvert,
  onClose,
}) {
  return (
    <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm flex items-end sm:items-center justify-center z-50 p-0 sm:p-4 animate-fade-in">
      <div className="bg-white rounded-t-2xl sm:rounded-2xl shadow-2xl w-full sm:max-w-md p-6 max-h-[90vh] overflow-y-auto animate-pop-in">
        <h2 className="m-0 mb-5 text-lg font-semibold text-slate-900">Convert Lead</h2>
        <div className="space-y-4">
          <div>
            <label className="text-sm font-medium text-slate-700">Customer</label>
            <div className="font-semibold">{selectedLead.name}</div>
          </div>
          <div>
            <label className="text-sm font-medium text-slate-700">Product</label>
            <div className="space-y-3 mt-1">
              {conversionItems.map((item, index) => (
                <div key={`${item.productId || item.productName}-${index}`} className="rounded-lg border border-slate-200 p-3">
                  <div className="flex items-start justify-between gap-2">
                    <div className="min-w-0">
                      <div className="font-medium truncate">{item.productName}</div>
                      <div className="text-sm text-slate-500">
                        KES {Number(item.sellingPrice).toLocaleString()} each
                      </div>
                    </div>
                    {conversionItems.length > 1 && (
                      <button
                        type="button"
                        onClick={() => setConversionItems((items) => items.filter((_, itemIndex) => itemIndex !== index))}
                        className="text-sm text-red-600 hover:text-red-700"
                        aria-label={`Remove ${item.productName}`}
                      >
                        Remove
                      </button>
                    )}
                  </div>
                  <label className="block text-xs text-slate-600 mt-2">Quantity</label>
                  <input
                    type="number"
                    min="1"
                    value={item.quantity}
                    onChange={(e) => setConversionItems((items) => items.map((line, lineIndex) =>
                      lineIndex === index ? { ...line, quantity: e.target.value } : line
                    ))}
                    className="w-full rounded-[10px] border border-slate-200 bg-white px-3 py-2 mt-1.5 text-sm"
                  />
                </div>
              ))}
            </div>
            <div className="flex flex-wrap gap-2 mt-3">
              <button
                type="button"
                onClick={() => onOpenProductPicker("change")}
                className="px-3 py-2 border border-green-600 text-green-700 rounded-lg text-sm font-medium hover:bg-green-50"
              >
                Change Product
              </button>
              <button
                type="button"
                onClick={() => onOpenProductPicker("add")}
                className="px-3 py-2 bg-green-600 text-white rounded-lg text-sm font-medium hover:bg-green-700"
              >
                Add Product
              </button>
            </div>
            {productPickerMode && (
              <div className="mt-3 rounded-lg bg-slate-50 p-3">
                <label className="text-sm font-medium text-slate-700">
                  {productPickerMode === "change" ? "Choose replacement product" : "Choose product to add"}
                </label>
                <input
                  type="search"
                  value={productSearch}
                  onChange={(e) => onProductSearchChange(e.target.value)}
                  placeholder="Type a product name or SKU..."
                  aria-label="Search products"
                  className="w-full border rounded p-2 mt-1 bg-white"
                />
                <div className="mt-2 max-h-48 overflow-y-auto rounded border bg-white" role="listbox" aria-label="Matching products">
                  {productSearchLoading ? (
                    <div className="px-3 py-4 text-sm text-slate-500" role="status">Searching products...</div>
                  ) : productsForConversion.length ? productsForConversion.map((product) => (
                    <button
                      key={product.id}
                      type="button"
                      role="option"
                      aria-selected={String(pickerProductId) === String(product.id)}
                      onClick={() => onPickerSelect(String(product.id))}
                      className={`block w-full border-b last:border-b-0 px-3 py-2 text-left text-sm hover:bg-green-50 ${String(pickerProductId) === String(product.id) ? "bg-green-100" : ""}`}
                    >
                      <span className="block font-medium">{product.name}</span>
                      <span className="text-xs text-slate-500">
                        {product.sku ? `${product.sku} · ` : ""}KES {Number(product.sellingPrice).toLocaleString()}
                      </span>
                    </button>
                  )) : (
                    <div className="px-3 py-4 text-sm text-slate-500" role="status">
                      {productSearch.trim() ? "No matching products found." : "No products available."}
                    </div>
                  )}
                </div>
                <div className="flex justify-end gap-2 mt-2">
                  <button
                    type="button"
                    onClick={onCancelPicker}
                    className="px-3 py-1.5 border rounded text-sm"
                  >
                    Cancel
                  </button>
                  <button
                    type="button"
                    onClick={onApplyPickedProduct}
                    disabled={!pickerProductId}
                    className="px-3 py-1.5 bg-green-600 text-white rounded text-sm disabled:opacity-50"
                  >
                    {productPickerMode === "change" ? "Change" : "Add"}
                  </button>
                </div>
              </div>
            )}
          </div>
          <div>
            <label className="text-sm font-medium text-slate-700">Delivery Fee</label>
            <input type="number" min="0" value={deliveryFee}
              onChange={(e) => onDeliveryFeeChange(e.target.value)}
              className="w-full rounded-[10px] border border-slate-200 bg-white px-3 py-2 mt-1.5 text-sm" />
          </div>
          <div>
            <label className="text-sm font-medium text-slate-700">Discount</label>
            <input type="number" min="0" value={discount}
              onChange={(e) => onDiscountChange(e.target.value)}
              className="w-full rounded-[10px] border border-slate-200 bg-white px-3 py-2 mt-1.5 text-sm" />
          </div>
          <div>
            <label className="text-sm font-medium text-slate-700">Customer Notes</label>
            <textarea
              value={customerNotes}
              onChange={(e) => onCustomerNotesChange(e.target.value)}
              rows={4}
              className="w-full border rounded p-2 mt-1 resize-none"
              placeholder="Add what the customer said..."
            />
          </div>
          <div>
            <label className="text-sm font-medium text-slate-700">
              Total (KES {subtotal.toLocaleString()} products + KES {Number(safeDeliveryFee).toLocaleString()} delivery − KES {Number(safeDiscount).toLocaleString()} discount)
            </label>
            <div className="text-lg font-bold text-green-600">
              KES {totalAmount.toLocaleString()}
            </div>
          </div>
          <div>
            <label className="text-sm font-medium text-slate-700">Delivery Location</label>
            <input type="text" value={deliveryCity}
              onChange={(e) => onDeliveryCityChange(e.target.value)}
              className="w-full rounded-[10px] border border-slate-200 bg-white px-3 py-2 mt-1.5 text-sm" />
          </div>
          <div>
            <label className="text-sm font-medium text-slate-700">Courier</label>
            <select
              value={selectedCourierId}
              onChange={(e) => onCourierChange(e.target.value)}
              className="w-full rounded-[10px] border border-slate-200 bg-white px-3 py-2 mt-1.5 text-sm"
            >
              <option value="">No courier (assign later)</option>
              {couriers.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name} — {c.phoneNumber}
                </option>
              ))}
            </select>
            {couriers.length === 0 && (
              <div className="text-xs text-slate-400 mt-1">
                No active couriers found. Add one under the Couriers page.
              </div>
            )}
          </div>
        </div>
        <div className="flex flex-col-reverse sm:flex-row justify-end gap-3 mt-6">
          <button onClick={onClose}
            className="px-4 py-2.5 border border-slate-200 rounded-[10px] text-sm font-semibold text-slate-700 hover:bg-slate-50">
            Cancel
          </button>
          <button onClick={onConvert}
            disabled={converting || conversionItems.length === 0}
            className="px-4 py-2.5 bg-brand-600 text-white rounded-[10px] text-sm font-semibold hover:bg-brand-700">
            {converting ? "Converting..." : "Confirm Convert"}
          </button>
        </div>
      </div>
    </div>
  );
}
