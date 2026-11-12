import React, { lazy, Suspense } from "react";
import { Routes, Route, Navigate, useLocation } from "react-router-dom";

import AppLayout from "./layout/AppLayout";

const Dashboard = lazy(() => import("./pages/Dashboard"));
const Orders = lazy(() => import("./pages/Orders"));
const Inventory = lazy(() => import("./pages/Inventory"));
const StaffLogs = lazy(() => import("./pages/StaffLogs"));
const Payment = lazy(() => import("./pages/Payment"));
const Staff = lazy(() => import("./pages/Staff"));
const Requisition = lazy(() => import("./pages/Requisition"));
const RequisitionList = lazy(() => import("./pages/RequisitionList"));
const Login = lazy(() => import("./pages/Login"));
const OTPVerification = lazy(() => import("./pages/OTPVerification"));
const Category = lazy(() => import("./pages/Category"));
const Product = lazy(() => import("./pages/Product"));
const Expenses = lazy(() => import("./pages/Expense"));
const StaffDetails = lazy(() => import("./pages/StaffDetails"));
const CRM = lazy(() => import("./pages/crm"));
const Courier = lazy(() => import("./pages/Courier"));
const DispatchScanner = lazy(() => import("./pages/DispatchScanner"));

function RouteFallback() {
  return (
    <div className="grid min-h-[60vh] place-items-center text-sm font-medium text-slate-500">
      Loading...
    </div>
  );
}

// Helper function to validate token presence and expiry.
// An expired access token is still accepted when a refresh token exists:
// the axios interceptor refreshes it on the first request.
function isTokenExpired(token) {
  try {
    const base64 = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
    const payload = JSON.parse(atob(base64));
    return Boolean(payload?.exp) && payload.exp * 1000 <= Date.now();
  } catch {
    return true;
  }
}

const isAuthenticated = () => {
  const token = localStorage.getItem("accessToken");
  if (!token || token === "undefined" || token === "null") return false;
  if (!isTokenExpired(token)) return true;

  const refreshToken = localStorage.getItem("refreshToken");
  return Boolean(refreshToken) && refreshToken !== "undefined" && refreshToken !== "null";
};

function PrivateRoute({ children }) {
  const location = useLocation();

  if (!isAuthenticated()) {
    // Redirect to login and save the attempted location
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return children;
}

function PublicRoute({ children }) {
  if (isAuthenticated()) {
    return <Navigate to="/dashboard" replace />;
  }

  return children;
}

export default function App() {
  return (
    <Suspense fallback={<RouteFallback />}>
      <Routes>
      {/* 1. Root Redirect Logic: 
          Instead of a static check, we redirect to /dashboard 
          and let the PrivateRoute handle the logic. 
      */}
      <Route path="/" element={<Navigate to="/dashboard" replace />} />

      {/* 2. Public Routes */}
      <Route
        path="/login"
        element={
          <PublicRoute>
            <Login />
          </PublicRoute>
        }
      />
      <Route
        path="/verify-otp"
        element={
          <PublicRoute>
            <OTPVerification />
          </PublicRoute>
        }
      />

      {/* 3. Protected Routes (Wrapped in Layout) */}
      <Route
        element={
          <PrivateRoute>
            <AppLayout />
          </PrivateRoute>
        }
      >
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/crm" element={<CRM />} />
        <Route path="/crm/leads/:id" element={<CRM />} />
        <Route path="/orders" element={<Orders />} />
        <Route path="/dispatch-scanner" element={<DispatchScanner />} />
        <Route path="/couriers" element={<Courier />} />
        <Route path="/requisitions" element={<RequisitionList />} />
        <Route path="/requisitions/new" element={<Requisition />} />
        <Route path="/categories" element={<Category />} />
        <Route path="/products" element={<Product />} />
        <Route path="/payments" element={<Payment />} />
        <Route path="/expenses" element={<Expenses />} />
        <Route path="/inventory" element={<Inventory />} />
        <Route path="/staff" element={<Staff />} />
        <Route path="/staff/:id" element={<StaffDetails />} />
        <Route path="/staff-details" element={<StaffDetails />} />
        <Route path="/staff-logs" element={<StaffLogs />} />
      </Route>

      {/* 4. Catch-all */}
      <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Suspense>
  );
}