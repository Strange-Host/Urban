// src/auth/PermissionGate.jsx

import { useAuth } from "./useAuth";

export default function PermissionGate({ permission, role, children, fallback = null }) {
  const { can, user } = useAuth();

  if (role && user?.role !== role) return fallback;
  if (permission && !can(permission)) return fallback;

  return children;
}