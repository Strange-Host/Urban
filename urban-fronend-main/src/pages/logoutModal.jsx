import { useState } from "react";
import { FiLogOut, FiX } from "react-icons/fi";
import { api } from "../api/api";
import { useNavigate } from "react-router-dom";
import { Spinner } from "../components/ui";

function YesNo({ value, onChange }) {
  const opt = (v, label) => (
    <button
      type="button"
      onClick={() => onChange(v)}
      className={`flex-1 rounded-lg px-4 py-2 text-sm font-semibold ${
        value === v
          ? v
            ? "bg-white text-emerald-700 shadow-sm"
            : "bg-white text-rose-700 shadow-sm"
          : "text-slate-500 hover:text-slate-700"
      }`}
    >
      {label}
    </button>
  );

  return (
    <div className="flex w-full gap-1 rounded-xl bg-slate-100 p-1 sm:w-44">
      {opt(true, "Yes")}
      {opt(false, "No")}
    </div>
  );
}

function Question({ label, done, setDone, reason, setReason }) {
  return (
    <div className="rounded-xl border border-slate-200 p-4">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <p className="m-0 text-sm font-medium text-slate-800">{label}</p>
        <YesNo value={done} onChange={setDone} />
      </div>

      {!done && (
        <input
          className="mt-3 w-full animate-fade-in rounded-lg border border-slate-200 px-3 py-2 text-sm"
          placeholder="Why not?"
          value={reason}
          onChange={(e) => setReason(e.target.value)}
        />
      )}
    </div>
  );
}

export default function LogoutModal({ isOpen, onClose }) {
  const navigate = useNavigate();

  const [ordersDone, setOrdersDone] = useState(true);
  const [ordersReason, setOrdersReason] = useState("");

  const [parcelsDone, setParcelsDone] = useState(true);
  const [parcelsReason, setParcelsReason] = useState("");

  const [notes, setNotes] = useState("");
  const [loading, setLoading] = useState(false);

  if (!isOpen) return null;

  const handleSubmit = async () => {
    // ✅ validation
    if (!ordersDone && !ordersReason.trim()) {
      return alert("Please provide reason for orders");
    }

    if (!parcelsDone && !parcelsReason.trim()) {
      return alert("Please provide reason for parcels");
    }

    try {
      setLoading(true);

      // 1. logout-check
      await api.post("/api/v1/auth/logout-check", {
        ordersDone,
        ordersReason: ordersDone ? null : ordersReason,
        parcelsDone,
        parcelsReason: parcelsDone ? null : parcelsReason,
        notes,
      });

      // 2. logout
      await api.post("/api/v1/auth/logout");

      // 3. clear storage (CONSISTENT)
      localStorage.removeItem("accessToken");
      localStorage.removeItem("refreshToken");
      localStorage.removeItem("otp_email");

      // 4. redirect
      navigate("/login");
    } catch (error) {
      console.error("Logout request failed:", error);
      alert(
        error.response?.data?.message ||
          "Logout failed. Check that the backend is running and try again."
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-[9999] flex animate-fade-in items-end justify-center bg-slate-900/50 p-0 backdrop-blur-sm sm:items-center sm:p-4"
      onClick={onClose}
    >
      <div
        className="max-h-[90vh] w-full animate-pop-in overflow-y-auto rounded-t-2xl bg-white shadow-2xl sm:max-w-md sm:rounded-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-start gap-3 border-b border-slate-100 p-5">
          <span className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-rose-50 text-rose-600">
            <FiLogOut size={18} />
          </span>
          <div className="min-w-0 flex-1">
            <h2 className="m-0 text-base font-semibold text-slate-900">End of shift checklist</h2>
            <p className="m-0 mt-0.5 text-sm text-slate-500">Quick check-in before you log out.</p>
          </div>
          <button
            onClick={onClose}
            aria-label="Close"
            className="grid h-8 w-8 place-items-center rounded-lg text-slate-400 hover:bg-slate-100 hover:text-slate-600"
          >
            <FiX size={18} />
          </button>
        </div>

        <div className="space-y-3 p-5">
          <Question
            label="Were all orders made?"
            done={ordersDone}
            setDone={setOrdersDone}
            reason={ordersReason}
            setReason={setOrdersReason}
          />
          <Question
            label="Were all parcels dispatched?"
            done={parcelsDone}
            setDone={setParcelsDone}
            reason={parcelsReason}
            setReason={setParcelsReason}
          />

          <div>
            <label className="mb-1.5 block text-sm font-medium text-slate-800">
              Anything worth noting? <span className="font-normal text-slate-400">(optional)</span>
            </label>
            <textarea
              rows={3}
              className="w-full resize-none rounded-xl border border-slate-200 px-3 py-2 text-sm"
              placeholder="Handover notes for the next shift…"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
            />
          </div>
        </div>

        {/* Actions */}
        <div className="flex flex-col-reverse gap-2 border-t border-slate-100 bg-slate-50/60 p-4 sm:flex-row sm:justify-end">
          <button
            onClick={onClose}
            className="rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-semibold text-slate-700 hover:bg-slate-50"
          >
            Cancel
          </button>

          <button
            onClick={handleSubmit}
            disabled={loading}
            className="flex items-center justify-center gap-2 rounded-xl bg-rose-600 px-4 py-2.5 text-sm font-semibold text-white hover:bg-rose-700"
          >
            {loading ? (
              <>
                <Spinner /> Submitting...
              </>
            ) : (
              "Submit & log out"
            )}
          </button>
        </div>
      </div>
    </div>
  );
}
