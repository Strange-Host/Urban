import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { loginUser } from "../api/authApi";
import { FiEye, FiEyeOff, FiAlertCircle, FiArrowRight } from "react-icons/fi";
import AuthShell, {
  AuthError,
  authInputClass,
  authLabelClass,
  authButtonClass,
} from "../components/AuthShell";
import { Spinner } from "../components/ui";

export default function Login() {
  const navigate = useNavigate();

  const [form, setForm] = useState({
    email: "",
    password: "",
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [showPassword, setShowPassword] = useState(false);

  function handleChange(e) {
    setForm({ ...form, [e.target.name]: e.target.value });
  }

  async function handleLogin(e) {
    e.preventDefault();
    setError("");

    if (!form.email || !form.password) {
      setError("Please enter email and password.");
      return;
    }

    try {
      setLoading(true);

      // Backend call
      await loginUser({
        email: form.email,
        password: form.password,
      });

      // Store email for OTP
      localStorage.setItem("otp_email", form.email);

      // Redirect to OTP verification
      navigate("/verify-otp");
    } catch (err) {
      const msg =
        err?.response?.data?.message ||
        err?.response?.data ||
        "Login failed. Check your credentials.";

      setError(typeof msg === "string" ? msg : "Login failed.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthShell title="Welcome back" subtitle="Sign in to access your POS dashboard.">
      {error && (
        <AuthError>
          <FiAlertCircle className="mt-0.5 shrink-0" />
          <span>{error}</span>
        </AuthError>
      )}

      <form onSubmit={handleLogin} className="grid gap-5">
        <div>
          <label htmlFor="email" className={authLabelClass}>
            Email
          </label>
          <input
            id="email"
            name="email"
            type="email"
            autoComplete="email"
            value={form.email}
            onChange={handleChange}
            placeholder="you@company.com"
            className={authInputClass}
          />
        </div>

        <div>
          <label htmlFor="password" className={authLabelClass}>
            Password
          </label>
          <div className="ui-field flex h-11 items-center rounded-xl border border-slate-200 bg-white pr-2">
            <input
              id="password"
              type={showPassword ? "text" : "password"}
              name="password"
              autoComplete="current-password"
              value={form.password}
              onChange={handleChange}
              placeholder="Enter your password"
              className="h-full min-w-0 flex-1 rounded-xl border-0 bg-transparent px-3.5 text-sm outline-none"
            />
            <button
              type="button"
              onClick={() => setShowPassword((visible) => !visible)}
              aria-label={showPassword ? "Hide password" : "Show password"}
              title={showPassword ? "Hide password" : "Show password"}
              className="grid h-8 w-8 place-items-center rounded-lg text-slate-400 hover:bg-slate-100 hover:text-slate-600"
            >
              {showPassword ? <FiEyeOff size={17} /> : <FiEye size={17} />}
            </button>
          </div>
        </div>

        <button disabled={loading} type="submit" className={`${authButtonClass} mt-1`}>
          {loading ? (
            <>
              <Spinner /> Signing in...
            </>
          ) : (
            <>
              Sign in <FiArrowRight size={16} />
            </>
          )}
        </button>
      </form>

      <p className="mt-6 mb-0 text-center text-xs text-slate-400">
        You'll receive a one-time code by SMS to complete sign in.
      </p>
    </AuthShell>
  );
}
