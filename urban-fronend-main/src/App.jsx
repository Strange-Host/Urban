import React from "react";
import { Routes, Route, Navigate, useLocation } from "react-router-dom";

import AppLayout from "./layout/AppLayout";
import Dashboard from "./pages/Dashboard";
import Orders from "./pages/Orders";
import Inventory from "./pages/Inventory";
import StaffLogs from "./pages/StaffLogs";
import Payment from "./pages/Payment";
import Staff from "./pages/Staff";
import Requisition from "./pages/Requisition";
import RequisitionList from "./pages/RequisitionList";
import Login from "./pages/Login";
import OTPVerification from "./pages/OTPVerification";
import Category from "./pages/Category";
import Product from "./pages/Product";
import Expenses from "./pages/Expense";
import StaffDetails from "./pages/StaffDetails";
import CRM from "./pages/crm";
import Courier from "./pages/Courier";
import DispatchScanner from "./pages/DispatchScanner";

// Helper function to validate token presence and basic integrity
const isAuthenticated = () => {
  const token = localStorage.getItem("accessToken");
  return token && token !== "undefined" && token !== "null";
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
  );
}