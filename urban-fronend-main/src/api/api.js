import axios from "axios";

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api/v1";

export const api = axios.create({
  baseURL: API_BASE_URL.replace(/\/api\/v1\/?$/, ""),
  headers: {
    "Content-Type": "application/json",
  },
});

// Auth endpoints must never trigger the refresh flow: a 401 there means
// bad credentials, not an expired session.
const AUTH_PATHS = [
  "/api/v1/auth/login",
  "/api/v1/auth/verify-otp",
  "/api/v1/auth/register",
  "/api/v1/auth/refresh",
];

function isAuthRequest(config) {
  // Exact path match (ignoring any query string). A substring match would also
  // catch endpoints such as /auth/logout-check, which must be able to refresh.
  const path = String(config?.url || "").split("?")[0].replace(/\/+$/, "");
  return AUTH_PATHS.some((authPath) => path === authPath);
}

function clearSessionAndRedirect() {
  localStorage.removeItem("accessToken");
  localStorage.removeItem("refreshToken");
  localStorage.removeItem("otp_email");

  if (window.location.pathname !== "/login") {
    window.location.href = "/login";
  }
}

// Single in-flight refresh shared by all queued 401s.
let refreshPromise = null;

function refreshAccessToken() {
  const refreshToken = localStorage.getItem("refreshToken");
  if (!refreshToken || refreshToken === "undefined" || refreshToken === "null") {
    return Promise.reject(new Error("No refresh token available"));
  }

  // Bare axios (not `api`) so a failed refresh cannot re-enter this interceptor.
  refreshPromise = axios
    .post(`${api.defaults.baseURL}/api/v1/auth/refresh`, { refreshToken })
    .then((res) => {
      const accessToken = res.data?.accessToken;
      if (!accessToken) {
        throw new Error("Refresh response missing accessToken");
      }

      localStorage.setItem("accessToken", accessToken);
      if (res.data?.refreshToken) {
        localStorage.setItem("refreshToken", res.data.refreshToken);
      }

      return accessToken;
    })
    .finally(() => {
      refreshPromise = null;
    });

  return refreshPromise;
}

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
  async (error) => {
    const status = error.response?.status;
    const original = error.config;

    if (status !== 401 || !original || original._retry || isAuthRequest(original)) {
      return Promise.reject(error);
    }

    original._retry = true;

    try {
      const accessToken = await (refreshPromise || refreshAccessToken());
      original.headers = original.headers || {};
      original.headers.Authorization = `Bearer ${accessToken}`;
      return api(original);
    } catch (refreshError) {
      console.warn("Session expired. Redirecting to login.");
      clearSessionAndRedirect();
      return Promise.reject(refreshError);
    }
  }
);
