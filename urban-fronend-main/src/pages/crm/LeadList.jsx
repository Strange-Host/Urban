import React from "react";
import { FiArrowRight } from "react-icons/fi";
import { formatDateTime, formatPhoneLink, formatProductOptions } from "./crmUtils";

const statusColor = {
  NEW: "bg-sky-100 text-sky-800",
  CONTACTED: "bg-violet-100 text-violet-800",
  RESCHEDULED: "bg-amber-100 text-amber-800",
  CANCELLED: "bg-red-100 text-red-800",
  CONVERTED: "bg-green-100 text-green-800",
};

const LEAD_STATUS_OPTIONS = ["NEW", "CONTACTED", "RESCHEDULED", "CANCELLED", "CONVERTED"];

export function LeadMobileList({
  leads,
  highlightedLeadId,
  registerLeadRef,
  noteEdits,
  focusedNoteId,
  followUps,
  onNoteFocus,
  onNoteBlur,
  onNoteChange,
  onSaveNote,
  onStatusChange,
  onFollowUpChange,
  onSaveFollowUp,
  onConvert,
}) {
  return (
    <div className="lg:hidden space-y-3">
      {leads.map((lead) => {
        const badgeColor = statusColor[lead.status] || "bg-slate-100 text-slate-800";

        return (
          <div
            key={lead.id}
            ref={(el) => registerLeadRef(lead.id, el)}
            className={`bg-white p-4 rounded-2xl shadow-sm border transition-all ${
              String(highlightedLeadId) === String(lead.id)
                ? "border-brand-400 ring-4 ring-brand-500/15"
                : "border-slate-200"
            }`}
          >
            {/* Name and Status Badge */}
            <div className="flex items-start justify-between gap-2 mb-2">
              <div className="flex-1 min-w-0">
                <div className="font-semibold text-base truncate">{lead.name}</div>
                {lead.phoneNumber && (
                  <a
                    href={formatPhoneLink(lead.phoneNumber)}
                    className="block text-xs text-brand-600 hover:underline truncate"
                  >
                    {lead.phoneNumber}
                  </a>
                )}
                {lead.alternativePhoneNumber && (
                  <a
                    href={formatPhoneLink(lead.alternativePhoneNumber)}
                    className="block text-xs text-brand-600 hover:underline truncate"
                  >
                    Alt Phone: {lead.alternativePhoneNumber}
                  </a>
                )}
              </div>
              <span className={`px-2.5 py-0.5 rounded-full text-xs font-semibold whitespace-nowrap ${badgeColor}`}>
                {lead.status}
              </span>
            </div>

            {/* Product and Location */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 mb-3 text-xs">
              <div>
                <div className="text-slate-500">Product</div>
                <div className="font-medium truncate">{lead.formName}</div>
              </div>
              <div>
                <div className="text-slate-500">Location</div>
                <div className="font-medium truncate">{lead.location}</div>
              </div>
              <div>
                <div className="text-slate-500">Extra Details</div>
                <div className="font-medium truncate">{lead.additionalInfo}</div>
              </div>
              {formatProductOptions(lead.productOptions) && (
                <div className="sm:col-span-2">
                  <div className="text-slate-500">Product Options</div>
                  <div className="font-medium break-words whitespace-normal p-2 bg-brand-50 text-brand-900 rounded-lg text-sm">{formatProductOptions(lead.productOptions)}</div>
                </div>
              )}
              <div className="sm:col-span-2">
                <div className="text-slate-500">Customer Notes</div>
                <textarea
                  rows={3}
                  value={noteEdits[lead.id] ?? lead.customerNotes ?? lead.notes ?? ""}
                  onFocus={() => onNoteFocus(lead.id)}
                  onBlur={() => onNoteBlur(lead.id)}
                  onChange={(e) => onNoteChange(lead.id, e.target.value)}
                  className="w-full rounded-lg border border-slate-200 p-2 text-sm"
                  placeholder="Add customer notes here"
                />
                {(focusedNoteId === lead.id || noteEdits[lead.id] !== undefined) && (
                  <button
                    onClick={() => onSaveNote(lead)}
                    className="mt-2 w-full bg-brand-600 text-white py-2 rounded-lg text-sm font-medium hover:bg-brand-700"
                  >
                    Save Notes
                  </button>
                )}
              </div>
            </div>

            {/* Timestamp */}
            <div className="text-xs text-slate-400 mb-3">{formatDateTime(lead.created)}</div>

            {/* Follow-up date picker — only shown when RESCHEDULED */}
            {lead.status === "RESCHEDULED" && lead.followUpAt && (
              <div className="mb-3 p-2.5 bg-amber-50 rounded-lg text-xs border border-amber-200">
                <div className="text-slate-600 font-medium">Follow-up: {formatDateTime(lead.followUpAt)}</div>
              </div>
            )}

            {/* Status and Date Actions */}
            <div className="space-y-2">
              <div>
                <label className="text-xs font-medium text-slate-700 block mb-1">Update Status</label>
                <select
                  value={lead.status}
                  onChange={(e) => onStatusChange(lead, e.target.value)}
                  className="rounded-lg border border-slate-200 bg-white px-2.5 py-2 w-full text-sm"
                >
                  {LEAD_STATUS_OPTIONS.map((status) => (
                    <option key={status} value={status}>{status}</option>
                  ))}
                </select>
              </div>

              {/* Date picker appears only when RESCHEDULED is selected */}
              {(lead.status === "RESCHEDULED" || followUps[lead.id] !== undefined) && (
                <div>
                  <label className="text-xs font-medium text-slate-700 block mb-1">
                    Follow-up Date & Time
                  </label>
                  <input
                    type="datetime-local"
                    value={followUps[lead.id] || ""}
                    min={new Date().toISOString().slice(0, 16)}
                    onChange={(e) => onFollowUpChange(lead.id, e.target.value)}
                    className="rounded-lg border border-slate-200 bg-white px-2.5 py-2 w-full text-sm"
                  />
                  {followUps[lead.id] && (
                    <button
                      onClick={() => onSaveFollowUp(lead.id)}
                      className="mt-1.5 w-full bg-brand-600 text-white py-1.5 rounded text-sm font-medium hover:bg-brand-700"
                    >
                      Save Follow-up
                    </button>
                  )}
                </div>
              )}
            </div>

            {/* Convert Button */}
            {lead.status !== "CONVERTED" && (
              <button
                onClick={() => onConvert(lead)}
                className="mt-3 w-full bg-emerald-600 text-white py-2.5 rounded-[10px] text-sm font-semibold hover:bg-emerald-700"
              >
                Convert to Order
              </button>
            )}
          </div>
        );
      })}
    </div>
  );
}

export function LeadTable({
  leads,
  highlightedLeadId,
  registerLeadRef,
  noteEdits,
  focusedNoteId,
  followUps,
  onNoteFocus,
  onNoteBlur,
  onNoteChange,
  onSaveNote,
  onStatusChange,
  onFollowUpChange,
  onSaveFollowUp,
  onConvert,
}) {
  return (
    <div className={`${leads.length === 0 ? "hidden" : "hidden lg:block"} ui-card overflow-x-auto`}>
      <table className="ui-table min-w-full">
        <thead>
          <tr>
            <th>Name</th>
            <th>Phone</th>
            <th>Product</th>
            <th>Options</th>
            <th>Location</th>
            <th>Alt Phone No</th>
            <th>Extra Details</th>
            <th>Notes</th>
            <th>Status</th>
            <th>Follow-up Date</th>
            <th>Timestamp</th>
            <th>Action</th>
          </tr>
        </thead>
        <tbody>
          {leads.map((lead) => (
            <tr
              key={lead.id}
              ref={(el) => registerLeadRef(lead.id, el)}
              className={`align-top ${String(highlightedLeadId) === String(lead.id) ? "!bg-brand-50" : ""}`}
            >
              <td className="font-medium text-slate-900 whitespace-nowrap">{lead.name}</td>
              <td>
                {lead.phoneNumber ? (
                  <a href={formatPhoneLink(lead.phoneNumber)} className="text-brand-600 hover:underline">
                    {lead.phoneNumber}
                  </a>
                ) : (
                  "—"
                )}
              </td>
              <td>{lead.formName}</td>
              <td>{formatProductOptions(lead.productOptions) || "—"}</td>
              <td>{lead.location}</td>
              <td>
                {lead.alternativePhoneNumber ? (
                  <a href={formatPhoneLink(lead.alternativePhoneNumber)} className="text-brand-600 hover:underline">
                    {lead.alternativePhoneNumber}
                  </a>
                ) : (
                  "—"
                )}
              </td>
              <td>{lead.additionalInfo || "—"}</td>
              <td>
                <textarea
                  rows={2}
                  value={noteEdits[lead.id] ?? lead.customerNotes ?? lead.notes ?? ""}
                  onFocus={() => onNoteFocus(lead.id)}
                  onBlur={() => onNoteBlur(lead.id)}
                  onChange={(e) => onNoteChange(lead.id, e.target.value)}
                  className="w-full min-w-[180px] rounded-lg border border-slate-200 p-1.5 text-sm"
                  placeholder="Add notes..."
                />
                {(focusedNoteId === lead.id || noteEdits[lead.id] !== undefined) && (
                  <button
                    onClick={() => onSaveNote(lead)}
                    className="mt-2 bg-brand-600 text-white px-3 py-1 rounded-lg text-xs font-medium hover:bg-brand-700"
                  >
                    Save
                  </button>
                )}
              </td>

              <td>
                <select
                  value={lead.status}
                  onChange={(e) => onStatusChange(lead, e.target.value)}
                  className="rounded-lg border border-slate-200 bg-white px-2 py-1.5 text-sm"
                >
                  {LEAD_STATUS_OPTIONS.map((status) => (
                    <option key={status} value={status}>{status}</option>
                  ))}
                </select>
              </td>

              {/* Follow-up date column */}
              <td className="min-w-[220px]">
                {lead.status === "RESCHEDULED" || followUps[lead.id] !== undefined ? (
                  <div className="flex items-center gap-2">
                    <input
                      type="datetime-local"
                      value={followUps[lead.id] ?? (lead.followUpAt ? lead.followUpAt.slice(0, 16) : "")}
                      min={new Date().toISOString().slice(0, 16)}
                      onChange={(e) => onFollowUpChange(lead.id, e.target.value)}
                      className="rounded-lg border border-slate-200 p-1.5 text-xs"
                    />
                    {followUps[lead.id] && (
                      <button
                        onClick={() => onSaveFollowUp(lead.id)}
                        className="bg-brand-600 text-white px-2 py-1 rounded-lg text-xs font-medium whitespace-nowrap hover:bg-brand-700"
                      >
                        Save
                      </button>
                    )}
                  </div>
                ) : (
                  <span className="text-slate-400 text-xs">
                    {lead.followUpAt ? formatDateTime(lead.followUpAt) : "—"}
                  </span>
                )}
              </td>

              <td className="whitespace-nowrap text-slate-500">{formatDateTime(lead.created)}</td>

              <td>
                {lead.status !== "CONVERTED" && (
                  <button
                    onClick={() => onConvert(lead)}
                    className="inline-flex items-center gap-1 whitespace-nowrap rounded-lg bg-emerald-600 px-3 py-1.5 text-xs font-semibold text-white hover:bg-emerald-700"
                  >
                    Convert <FiArrowRight size={12} />
                  </button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
