import React from "react";
import { FiShoppingBag, FiTrendingUp, FiUsers } from "react-icons/fi";
import { BrandMark } from "../layout/Sidebar";

const highlights = [
  { icon: FiShoppingBag, text: "Orders, payments and deliveries in one place" },
  { icon: FiTrendingUp, text: "Live revenue, profit and expense reporting" },
  { icon: FiUsers, text: "Customer leads, staff and inventory tracking" },
];

export default function AuthShell({ title, subtitle, children }) {
  return (
    <div className="grid min-h-screen bg-white lg:grid-cols-2">
      {/* Brand panel */}
      <div className="relative hidden overflow-hidden bg-slate-900 p-12 text-white lg:flex lg:flex-col lg:justify-between">
        <div className="pointer-events-none absolute -left-24 -top-24 h-96 w-96 rounded-full bg-brand-600/30 blur-3xl" />
        <div className="pointer-events-none absolute -bottom-32 -right-16 h-[28rem] w-[28rem] rounded-full bg-indigo-400/20 blur-3xl" />

        <div className="relative flex items-center gap-3">
          <BrandMark />
          <span className="text-lg font-bold tracking-tight">Urban Trends</span>
        </div>

        <div className="relative max-w-md">
          <h2 className="text-4xl font-bold leading-tight tracking-tight">
            Run your store with clarity.
          </h2>
          <p className="mt-4 text-slate-300">
            Everything your team needs to sell, ship and grow — from the counter to the courier.
          </p>

          <ul className="mt-10 space-y-4 p-0">
            {highlights.map(({ icon: Icon, text }) => (
              <li key={text} className="flex list-none items-center gap-3 text-sm text-slate-200">
                <span className="grid h-9 w-9 place-items-center rounded-lg bg-white/10 text-brand-300">
                  <Icon size={16} />
                </span>
                {text}
              </li>
            ))}
          </ul>
        </div>

        <div className="relative text-xs text-slate-500">© {new Date().getFullYear()} Urban Trends</div>
      </div>

      {/* Form panel */}
      <div className="flex items-center justify-center bg-[var(--app-bg)] px-4 py-10 sm:px-8 lg:bg-white">
        <div className="w-full max-w-[420px] animate-pop-in">
          <div className="mb-8 flex items-center gap-3 lg:hidden">
            <BrandMark />
            <span className="text-lg font-bold tracking-tight text-slate-900">Urban Trends</span>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8 lg:border-0 lg:p-0 lg:shadow-none">
            <h1 className="m-0 text-2xl font-bold tracking-tight text-slate-900">{title}</h1>
            {subtitle && <p className="mt-2 mb-0 text-sm text-slate-500">{subtitle}</p>}
            <div className="mt-7">{children}</div>
          </div>
        </div>
      </div>
    </div>
  );
}

export function AuthError({ children }) {
  return (
    <div className="mb-5 flex items-start gap-2 rounded-xl border border-rose-200 bg-rose-50 px-3.5 py-3 text-sm font-medium text-rose-700">
      {children}
    </div>
  );
}

export const authInputClass =
  "h-11 w-full rounded-xl border border-slate-200 bg-white px-3.5 text-sm text-slate-900 outline-none";

export const authLabelClass = "mb-1.5 block text-sm font-medium text-slate-700";

export const authButtonClass =
  "flex h-11 w-full items-center justify-center gap-2 rounded-xl bg-brand-600 text-sm font-semibold text-white shadow-sm shadow-brand-600/30 hover:bg-brand-700";
