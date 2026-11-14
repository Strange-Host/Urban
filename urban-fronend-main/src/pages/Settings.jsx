import React, { useEffect, useState } from "react";
import { fetchSettings, updateMpesaPaybill } from "../api/settingsApi";
import { PageHeader, LoadingBlock } from "../components/ui";
import {
  alertError,
  alertSuccess,
  inputStyle,
  labelStyle,
  primaryBtn as saveBtn,
  tableCard,
} from "../components/styles";
import { isValidPaybill } from "../utils/paybill";
import { setCachedMpesaPaybill } from "../utils/settings";

function errorText(error, fallback) {
  const data = error?.response?.data;
  return (typeof data?.detail === "string" && data.detail) || fallback;
}

export default function Settings() {
  const [paybill, setPaybill] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  useEffect(() => {
    let active = true;
    fetchSettings()
      .then((settings) => {
        if (active) setPaybill(String(settings?.mpesaPaybill ?? ""));
      })
      .catch((e) => {
        if (active) setError(errorText(e, "Could not load settings."));
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  async function handleSave() {
    setError("");
    setSuccess("");

    if (!isValidPaybill(paybill)) {
      setError("Paybill must be 4 to 10 digits.");
      return;
    }

    try {
      setSaving(true);
      const saved = await updateMpesaPaybill(paybill.trim());
      const value = String(saved?.mpesaPaybill ?? "");
      setPaybill(value);
      setCachedMpesaPaybill(value);
      setSuccess(
        value
          ? "Paybill saved. It now appears on receipts, dispatch slips and order SMS."
          : "Paybill cleared. Receipts and SMS will not show a paybill until you set one."
      );
    } catch (e) {
      setError(errorText(e, "Could not save the paybill."));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div>
      <PageHeader title="Settings" subtitle="Shop-wide settings used on receipts and customer messages." />

      <div style={{ ...tableCard, padding: 24, maxWidth: 560 }}>
        {loading ? (
          <LoadingBlock label="Loading settings..." />
        ) : (
          <>
            {error && <div style={{ ...alertError, marginBottom: 16 }}>{error}</div>}
            {success && <div style={{ ...alertSuccess, marginBottom: 16 }}>{success}</div>}

            <label style={labelStyle} htmlFor="mpesa-paybill">
              M-Pesa paybill number
            </label>
            <input
              id="mpesa-paybill"
              inputMode="numeric"
              value={paybill}
              onChange={(e) => setPaybill(e.target.value)}
              placeholder="e.g. 522533"
              style={inputStyle}
            />
            <p style={{ margin: "8px 0 16px", fontSize: 13, color: "#64748b" }}>
              Customers pay to this paybill using their order's account number. Leave it empty
              to hide the paybill line from receipts and SMS.
            </p>

            <button onClick={handleSave} disabled={saving} style={saveBtn}>
              {saving ? "Saving..." : "Save"}
            </button>
          </>
        )}
      </div>
    </div>
  );
}
