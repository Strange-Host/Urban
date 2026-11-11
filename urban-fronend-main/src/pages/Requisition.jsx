import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { FiArrowLeft, FiBriefcase, FiUser, FiSend, FiCheckCircle, FiAlertCircle } from "react-icons/fi";
import { createRequisition } from "../api/requisitionApi";
import { PageHeader, Spinner } from "../components/ui";
import { inputStyle, labelStyle, primaryBtn, secondaryBtn, alertError, alertSuccess } from "../components/styles";

const REQUISITION_TYPES = [
  {
    value: "SHOP_OPERATIONAL",
    label: "Shop Operational",
    hint: "Supplies, repairs and running costs",
    icon: FiBriefcase,
  },
  {
    value: "SALARY_ADVANCE",
    label: "Salary Advance",
    hint: "An advance against upcoming salary",
    icon: FiUser,
  },
];

export default function Requisition() {
  const navigate = useNavigate();
  const [type, setType] = useState(REQUISITION_TYPES[0].value);
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [amountRequested, setAmountRequested] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    setSuccess("");

    if (!title || !description || !amountRequested) {
      setError("Please fill all fields before submitting.");
      return;
    }

    const amount = Number(amountRequested);
    if (!amount || amount <= 0) {
      setError("Enter a valid amount.");
      return;
    }

    try {
      setLoading(true);
      await createRequisition({
        requisitionType: type,
        title,
        description,
        amountRequested: amount,
      });
      setSuccess("Requisition created successfully.");
      setTitle("");
      setDescription("");
      setAmountRequested("");
    } catch (err) {
      console.log(err);
      const msg =
        err?.response?.data?.message ||
        err?.response?.data ||
        "Failed to create requisition.";
      setError(typeof msg === "string" ? msg : "Failed to create requisition.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="mx-auto max-w-3xl">
      <button
        type="button"
        onClick={() => navigate(-1)}
        className="mb-4 inline-flex items-center gap-1.5 text-sm font-medium text-slate-500 hover:text-brand-600"
      >
        <FiArrowLeft size={14} /> Back
      </button>

      <PageHeader
        title="Create Requisition"
        subtitle="Submit a request for approval. Once approved, finance can mark it paid and the matching expense or salary advance is created automatically."
      />

      <div className="ui-card p-5 sm:p-8">
        {error && (
          <div style={{ ...alertError, marginBottom: 20 }} className="flex items-center gap-2">
            <FiAlertCircle className="shrink-0" /> {error}
          </div>
        )}

        {success && (
          <div style={{ ...alertSuccess, marginBottom: 20 }} className="flex items-center gap-2">
            <FiCheckCircle className="shrink-0" /> {success}
          </div>
        )}

        <form onSubmit={handleSubmit} className="grid gap-6">
          <div>
            <label style={labelStyle}>Requisition type</label>
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              {REQUISITION_TYPES.map((option) => {
                const active = type === option.value;
                const Icon = option.icon;
                return (
                  <button
                    key={option.value}
                    type="button"
                    onClick={() => setType(option.value)}
                    className={`flex items-start gap-3 rounded-xl border p-4 text-left ${
                      active
                        ? "border-brand-500 bg-brand-50/60 ring-2 ring-brand-500/20"
                        : "border-slate-200 bg-white hover:border-slate-300"
                    }`}
                  >
                    <span
                      className={`grid h-9 w-9 shrink-0 place-items-center rounded-lg ${
                        active ? "bg-brand-600 text-white" : "bg-slate-100 text-slate-500"
                      }`}
                    >
                      <Icon size={16} />
                    </span>
                    <span>
                      <span className="block text-sm font-semibold text-slate-900">{option.label}</span>
                      <span className="mt-0.5 block text-xs text-slate-500">{option.hint}</span>
                    </span>
                  </button>
                );
              })}
            </div>
          </div>

          <div className="grid grid-cols-1 gap-6 sm:grid-cols-[2fr_1fr]">
            <div>
              <label style={labelStyle}>Title</label>
              <input
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="Short title for the requisition"
                style={inputStyle}
              />
            </div>

            <div>
              <label style={labelStyle}>Amount (KES)</label>
              <input
                type="number"
                min="0"
                step="0.01"
                value={amountRequested}
                onChange={(e) => setAmountRequested(e.target.value)}
                placeholder="0.00"
                style={inputStyle}
              />
            </div>
          </div>

          <div>
            <label style={labelStyle}>Description</label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Describe why this requisition is needed"
              style={{ ...inputStyle, minHeight: 140, resize: "vertical" }}
            />
          </div>

          <div className="flex flex-col-reverse gap-3 border-t border-slate-100 pt-6 sm:flex-row sm:justify-end">
            <button type="button" onClick={() => navigate(-1)} style={secondaryBtn}>
              Cancel
            </button>
            <button type="submit" disabled={loading} style={primaryBtn}>
              {loading ? (
                <>
                  <Spinner className="!h-4 !w-4" /> Saving...
                </>
              ) : (
                <>
                  <FiSend size={15} /> Submit Requisition
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
