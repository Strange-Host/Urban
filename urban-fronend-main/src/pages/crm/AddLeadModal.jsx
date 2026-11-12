import React from "react";

export function AddLeadModal({
  newLead,
  setNewLead,
  leadProducts,
  loadingLeadProducts,
  leadProductsError,
  savingLead,
  onCreate,
  onClose,
}) {
  return (
    <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm flex items-end sm:items-center justify-center z-50 p-0 sm:p-4 animate-fade-in">
      <div className="bg-white rounded-t-2xl sm:rounded-2xl shadow-2xl w-full sm:max-w-md p-6 max-h-[90vh] overflow-y-auto animate-pop-in">
        <h2 className="m-0 mb-5 text-lg font-semibold text-slate-900">Add Lead</h2>
        <div className="space-y-3">
          <div>
            <label className="text-sm font-medium text-slate-700">Source</label>
            <select
              value={newLead.source}
              onChange={(e) => setNewLead((p) => ({ ...p, source: e.target.value }))}
              className="w-full rounded-[10px] border border-slate-200 bg-white px-3 py-2 mt-1.5 text-sm"
            >
              <option value="CALL">Phone call</option>
              <option value="WHATSAPP">WhatsApp</option>
            </select>
          </div>
          <div>
            <label className="text-sm font-medium text-slate-700">Name *</label>
            <input type="text" value={newLead.name}
              onChange={(e) => setNewLead((p) => ({ ...p, name: e.target.value }))}
              className="w-full rounded-[10px] border border-slate-200 bg-white px-3 py-2 mt-1.5 text-sm" />
          </div>
          <div>
            <label className="text-sm font-medium text-slate-700">Phone Number *</label>
            <input type="text" value={newLead.phoneNumber}
              onChange={(e) => setNewLead((p) => ({ ...p, phoneNumber: e.target.value }))}
              className="w-full rounded-[10px] border border-slate-200 bg-white px-3 py-2 mt-1.5 text-sm" />
          </div>
          <div>
            <label className="text-sm font-medium text-slate-700">Alternative Phone</label>
            <input type="text" value={newLead.alternativePhoneNumber}
              onChange={(e) => setNewLead((p) => ({ ...p, alternativePhoneNumber: e.target.value }))}
              className="w-full rounded-[10px] border border-slate-200 bg-white px-3 py-2 mt-1.5 text-sm" />
          </div>
          <div>
            <label className="text-sm font-medium text-slate-700">Product</label>
            <select
              value={newLead.formName}
              onChange={(e) => setNewLead((p) => ({ ...p, formName: e.target.value }))}
              className="w-full border rounded p-2 mt-1 bg-white"
              disabled={loadingLeadProducts}
            >
              <option value="">
                {loadingLeadProducts ? "Loading products..." : "Select a product (optional)"}
              </option>
              {leadProducts.map((product) => (
                <option key={product.id} value={product.name}>
                  {product.name}{product.sku ? ` (${product.sku})` : ""}
                </option>
              ))}
            </select>
            {leadProductsError && (
              <div className="text-xs text-red-600 mt-1">{leadProductsError}</div>
            )}
          </div>
          <div>
            <label className="text-sm font-medium text-slate-700">Location</label>
            <input type="text" value={newLead.location}
              onChange={(e) => setNewLead((p) => ({ ...p, location: e.target.value }))}
              className="w-full rounded-[10px] border border-slate-200 bg-white px-3 py-2 mt-1.5 text-sm" />
          </div>
          <div>
            <label className="text-sm font-medium text-slate-700">Additional Info</label>
            <textarea rows={2} value={newLead.additionalInfo}
              onChange={(e) => setNewLead((p) => ({ ...p, additionalInfo: e.target.value }))}
              className="w-full rounded-[10px] border border-slate-200 bg-white px-3 py-2 mt-1.5 text-sm" />
          </div>
          <div>
            <label className="text-sm font-medium text-slate-700">Notes</label>
            <textarea rows={2} value={newLead.notes}
              onChange={(e) => setNewLead((p) => ({ ...p, notes: e.target.value }))}
              className="w-full rounded-[10px] border border-slate-200 bg-white px-3 py-2 mt-1.5 text-sm" />
          </div>
        </div>
        <div className="flex flex-col-reverse sm:flex-row justify-end gap-3 mt-6">
          <button onClick={onClose}
            className="px-4 py-2.5 border border-slate-200 rounded-[10px] text-sm font-semibold text-slate-700 hover:bg-slate-50">
            Cancel
          </button>
          <button onClick={onCreate} disabled={savingLead}
            className="px-4 py-2.5 bg-brand-600 text-white rounded-[10px] text-sm font-semibold hover:bg-brand-700">
            {savingLead ? "Saving..." : "Add Lead"}
          </button>
        </div>
      </div>
    </div>
  );
}
