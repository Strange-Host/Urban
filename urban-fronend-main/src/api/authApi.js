import axios from "axios";

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api/v1";

const authApi = axios.create({
  baseURL: API_BASE_URL.replace(/\/api\/v1\/?$/, ""),
  headers: {
    "Content-Type": "application/json",
  },
});

export async function loginUser(payload) {
  const res = await authApi.post("/api/v1/auth/login", payload);
  return res.data;
}

export async function verifyOtp(payload) {
  const res = await authApi.post("/api/v1/auth/verify-otp", payload);
  return res.data;
}
