import axios from "axios";

const authApi = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL.replace(/\/api\/v1\/?$/, ""),
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
