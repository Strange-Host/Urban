import React from "react";
import { FiArrowRight, FiBell, FiX } from "react-icons/fi";

export function Toast({ notifications, onDismiss, onNavigate }) {
  return (
    <div className="fixed top-4 right-4 z-[100] flex flex-col gap-2 max-w-sm w-full">
      {notifications.map((n) => (
        <div
          key={n.id}
          className="bg-white border border-slate-200 shadow-xl rounded-xl p-4 flex items-start gap-3 animate-slide-in"
        >
          <span className="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-emerald-50 text-emerald-600">
            <FiBell size={16} />
          </span>
          <div className="flex-1 min-w-0">
            <div className="font-semibold text-sm text-slate-800">{n.title}</div>
            <div className="text-xs text-slate-500 mt-1">{n.message}</div>
            <button
              onClick={() => onNavigate(n)}
              className="mt-2 inline-flex items-center gap-1 text-xs font-semibold text-brand-600 hover:text-brand-700"
            >
              View lead <FiArrowRight size={12} />
            </button>
          </div>
          <button
            onClick={() => onDismiss(n.id)}
            aria-label="Dismiss"
            className="grid h-7 w-7 place-items-center rounded-lg text-slate-400 hover:bg-slate-100 hover:text-slate-600"
          >
            <FiX size={15} />
          </button>
        </div>
      ))}
    </div>
  );
}
