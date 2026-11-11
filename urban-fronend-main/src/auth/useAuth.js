import { useMemo } from "react";

function parseJwt(token) {
  try {
    const base64 = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
    return JSON.parse(atob(base64));
  } catch {
    return null;
  }
}

export function useAuth() {
  const token = localStorage.getItem("accessToken");

  const user = useMemo(() => {
    if (!token) return null;
    const decoded = parseJwt(token);
    if (!decoded) return null;

    // Spring Security puts authorities as array of {authority: "..."} objects
    const authorities = (decoded.authorities || decoded.roles || []).map(
      (a) => (typeof a === "string" ? a : a.authority)
    );

    const role = authorities.find((a) => a.startsWith("ROLE_"))?.replace("ROLE_", "") || null;
    const permissions = authorities.filter((a) => !a.startsWith("ROLE_"));

    return {
      id: decoded.id || decoded.userId || decoded.user_id || null,
      email: decoded.sub,
      name:
        decoded.name ||
        decoded.fullName ||
        [decoded.firstName || decoded.given_name, decoded.lastName || decoded.family_name]
          .filter(Boolean)
          .join(" ") ||
        decoded.preferred_username ||
        decoded.sub,
      phoneNumber:
        decoded.phoneNumber ||
        decoded.phone_number ||
        decoded.phone ||
        decoded.mobile ||
        decoded.mobileNumber ||
        "",
      role,
      permissions,
    };
  }, [token]);

  const can = (permission) => user?.permissions?.includes(permission) ?? false;
  const isAdmin = () => user?.role === "ADMIN";
  const isManager = () => user?.role === "MANAGER";
  const isCashier = () => user?.role === "CASHIER";

  return { user, can, isAdmin, isManager, isCashier };
}