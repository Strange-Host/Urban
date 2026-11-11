import React from "react";
import { NavLink } from "react-router-dom";
import { FiLogOut, FiX } from "react-icons/fi";
import { navSections } from "./navItems";
import { useAuth } from "../auth/useAuth";

export function BrandMark({ className = "" }) {
  return (
    <span
      className={`grid h-9 w-9 shrink-0 place-items-center rounded-xl bg-gradient-to-br from-brand-500 to-brand-700 text-white shadow-lg shadow-brand-900/30 ${className}`}
    >
      <svg viewBox="0 0 64 64" className="h-5 w-5" aria-hidden="true">
        <path
          d="M20 16v19a12 12 0 0 0 24 0V16"
          fill="none"
          stroke="currentColor"
          strokeWidth="7"
          strokeLinecap="round"
        />
      </svg>
    </span>
  );
}

function initials(name = "") {
  const parts = name.replace(/@.*/, "").split(/[\s._-]+/).filter(Boolean);
  return (parts[0]?.[0] || "U").toUpperCase() + (parts[1]?.[0] || "").toUpperCase();
}

export default function Sidebar({ onClose, mobile = false, onShowLogoutModal }) {
  const { user, can } = useAuth();

  const linkClass = ({ isActive }) =>
    `group relative flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
      isActive
        ? "bg-white/10 text-white"
        : "text-slate-400 hover:bg-white/5 hover:text-slate-100"
    }`;

  return (
    <aside
      className={`h-full w-[260px] shrink-0 flex-col bg-slate-900 text-white ${
        mobile ? "flex animate-drawer-in" : "hidden lg:flex"
      }`}
    >
      {/* Brand */}
      <div className="flex h-16 items-center justify-between px-5">
        <div className="flex items-center gap-3">
          <BrandMark />
          <div className="leading-tight">
            <div className="text-[15px] font-bold tracking-tight">Urban Trends</div>
            <div className="text-[11px] font-medium text-slate-400">Point of Sale</div>
          </div>
        </div>

        {mobile && onClose && (
          <button
            onClick={onClose}
            aria-label="Close menu"
            className="grid h-8 w-8 place-items-center rounded-lg text-slate-400 hover:bg-white/10 hover:text-white lg:hidden"
          >
            <FiX size={18} />
          </button>
        )}
      </div>

      {/* Navigation */}
      <nav className="flex-1 overflow-y-auto px-3 pb-4 pt-2">
        {navSections.map((section) => ({
          ...section,
          items: section.items.filter((item) => !item.permission || can(item.permission)),
        })).filter((section) => section.items.length > 0).map((section) => (
          <div key={section.label} className="mb-4">
            <div className="px-3 pb-1.5 text-[11px] font-semibold uppercase tracking-wider text-slate-500">
              {section.label}
            </div>
            <div className="space-y-0.5">
              {section.items.map(({ to, label, icon: Icon }) => (
                <NavLink key={to} to={to} className={linkClass} onClick={onClose}>
                  {({ isActive }) => (
                    <>
                      {isActive && (
                        <span className="absolute inset-y-1.5 left-0 w-[3px] rounded-r-full bg-brand-400" />
                      )}
                      <Icon
                        size={17}
                        className={isActive ? "text-brand-300" : "text-slate-500 group-hover:text-slate-300"}
                      />
                      {label}
                    </>
                  )}
                </NavLink>
              ))}
            </div>
          </div>
        ))}
      </nav>

      {/* User + logout */}
      <div className="border-t border-white/10 p-3">
        <div className="flex items-center gap-3 rounded-lg px-2 py-2">
          <span className="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-slate-700 text-xs font-semibold text-slate-100">
            {initials(user?.name || user?.email)}
          </span>
          <div className="min-w-0 flex-1 leading-tight">
            <div className="truncate text-sm font-medium text-slate-100">{user?.name || "Signed in"}</div>
            <div className="truncate text-xs text-slate-400">
              {user?.role ? user.role.charAt(0) + user.role.slice(1).toLowerCase() : user?.email}
            </div>
          </div>
          <button
            onClick={() => {
              onShowLogoutModal();
              if (onClose) onClose();
            }}
            title="Log out"
            aria-label="Log out"
            className="grid h-8 w-8 place-items-center rounded-lg text-slate-400 hover:bg-white/10 hover:text-rose-300"
          >
            <FiLogOut size={16} />
          </button>
        </div>
      </div>
    </aside>
  );
}
