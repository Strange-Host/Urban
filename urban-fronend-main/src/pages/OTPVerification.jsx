import React, { useEffect, useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { FiAlertCircle, FiArrowLeft, FiShield } from "react-icons/fi";
import { api } from "../api/api";
import AuthShell, { AuthError, authLabelClass, authButtonClass } from "../components/AuthShell";
import { Spinner } from "../components/ui";

export default function OTPVerification() {
  const navigate = useNavigate();

  const [otp, setOtp] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [email, setEmail] = useState("");

  useEffect(() => {
    const savedEmail = localStorage.getItem("otp_email");
    if (!savedEmail) {
      navigate("/login");
      return;
    }
    setEmail(savedEmail);
  }, [navigate]);

  const handleVerifyOtp = async (e) => {
    if (e) e.preventDefault();
    setError("");

    if (!otp) {
      setError("Please enter the OTP.");
      return;
    }

    if (!email) {
      setError("Unable to verify OTP. Please login again.");
      return;
    }

    try {
      setLoading(true);

      const res = await api.post("/api/v1/auth/verify-otp", {
        email,
        otp,
      });

      localStorage.setItem("accessToken", res.data.accessToken);
      localStorage.setItem("refreshToken", res.data.refreshToken);
      localStorage.removeItem("otp_email");

      navigate("/dashboard");
    } catch (err) {
      const msg =
        err?.response?.data?.message ||
        err?.response?.data ||
        "OTP verification failed.";

      setError(typeof msg === "string" ? msg : "OTP verification failed.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthShell
      title="Verify it's you"
      subtitle={
        <>
          Enter the code we sent by SMS for{" "}
          <span className="font-medium text-slate-700">{email || "your account"}</span>.
        </>
      }
    >
      {error && (
        <AuthError>
          <FiAlertCircle className="mt-0.5 shrink-0" />
          <span>{error}</span>
        </AuthError>
      )}

      <form onSubmit={handleVerifyOtp} className="grid gap-5">
        <div>
          <label htmlFor="otp" className={authLabelClass}>
            Verification code
          </label>
          <input
            id="otp"
            type="text"
            inputMode="numeric"
            autoComplete="one-time-code"
            pattern="[0-9]*"
            maxLength={8}
            autoFocus
            value={otp}
            // Basic regex to ensure only numbers are typed
            onChange={(e) => setOtp(e.target.value.replace(/\D/g, ""))}
            placeholder="••••••"
            className="h-14 w-full rounded-xl border border-slate-200 bg-white px-4 text-center font-mono text-2xl tracking-[0.5em] text-slate-900 outline-none placeholder:tracking-[0.5em]"
          />
        </div>

        <button type="submit" disabled={loading} className={authButtonClass}>
          {loading ? (
            <>
              <Spinner /> Verifying...
            </>
          ) : (
            <>
              <FiShield size={16} /> Verify code
            </>
          )}
        </button>
      </form>

      <Link
        to="/login"
        onClick={() => localStorage.removeItem("otp_email")}
        className="mt-6 flex items-center justify-center gap-1.5 text-sm font-medium text-slate-500 no-underline hover:text-brand-600"
      >
        <FiArrowLeft size={14} /> Back to sign in
      </Link>
    </AuthShell>
  );
}
