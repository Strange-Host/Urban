import React from "react";
import { FiSearch, FiInbox } from "react-icons/fi";

export function PageHeader({ title, subtitle, children }) {
  return (
    <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div className="min-w-0">
        <h1 className="m-0 text-2xl font-bold tracking-tight text-slate-900">{title}</h1>
        {subtitle && <p className="mt-1 mb-0 text-sm text-slate-500">{subtitle}</p>}
      </div>
      {children && <div className="flex flex-wrap items-center gap-2">{children}</div>}
    </div>
  );
}

export function SearchInput({ value, onChange, placeholder = "Search...", className = "", ...rest }) {
  return (
    <div
      className={`ui-field flex h-[42px] items-center gap-2.5 rounded-[10px] border border-slate-200 bg-white px-3.5 ${className}`}
    >
      <FiSearch className="shrink-0 text-slate-400" size={16} />
      <input
        value={value}
        onChange={onChange}
        placeholder={placeholder}
        className="h-full w-full border-0 bg-transparent text-sm outline-none"
        {...rest}
      />
    </div>
  );
}

export function EmptyState({ title = "Nothing here yet", message, icon: Icon = FiInbox }) {
  return (
    <div className="flex flex-col items-center justify-center px-6 py-12 text-center">
      <div className="mb-3 grid h-12 w-12 place-items-center rounded-full bg-slate-100 text-slate-400">
        <Icon size={22} />
      </div>
      <div className="font-semibold text-slate-700">{title}</div>
      {message && <div className="mt-1 text-sm text-slate-500">{message}</div>}
    </div>
  );
}

export function Spinner({ className = "" }) {
  return <span className={`ui-spinner ${className}`} aria-hidden="true" />;
}

export function LoadingBlock({ label = "Loading..." }) {
  return (
    <div className="flex items-center justify-center gap-3 py-12 text-sm text-slate-500">
      <Spinner className="text-brand-600" />
      {label}
    </div>
  );
}
