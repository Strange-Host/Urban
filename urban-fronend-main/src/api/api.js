import axios from "axios";

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL.replace(/\/api\/v1\/?$/, ""),
  headers: {
    "Content-Type": "application/json",
  },
});

console.log("API BASE URL:", import.meta.env.VITE_API_BASE_URL);

// REQUEST INTERCEPTOR
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("accessToken");

    // 🔥 ALWAYS attach token if it exists
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
  },
  (error) => Promise.reject(error)
);

// RESPONSE INTERCEPTOR
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;

    // only log out when the user is truly unauthorized or token expired
    if (status === 401) {
      console.warn("Session expired. Redirecting to login.");

      localStorage.removeItem("accessToken");
      localStorage.removeItem("refreshToken");

      window.location.href = "/login";
    }

    return Promise.reject(error);
  }
);