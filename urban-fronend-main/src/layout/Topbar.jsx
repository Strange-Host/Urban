import React from "react";
import { useLocation } from "react-router-dom";
import { FiMenu, FiCalendar } from "react-icons/fi";
import { titleForPath } from "./navItems";

export default function Topbar({ onMenuClick }) {
  const { pathname } = useLocation();
  const today = new Date().toLocaleDateString("en-KE", {
    weekday: "short",
    day: "numeric",
    month: "short",
    year: "numeric",
  });

  return (
    <header className="sticky top-0 z-30 flex h-16 shrink-0 items-center gap-3 border-b border-slate-200/80 bg-white/80 px-4 backdrop-blur sm:px-6">
      <button
        onClick={onMenuClick}
        aria-label="Open menu"
        className="grid h-9 w-9 place-items-center rounded-lg border border-slate-200 text-slate-600 hover:bg-slate-50 lg:hidden"
      >
        <FiMenu size={18} />
      </button>

      <div className="min-w-0 flex-1">
        <div className="text-xs font-medium text-slate-400">Urban Trends</div>
        <div className="truncate text-[15px] font-semibold text-slate-800">{titleForPath(pathname)}</div>
      </div>

      <div className="hidden items-center gap-2 rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-sm text-slate-600 sm:flex">
        <FiCalendar size={14} className="text-slate-400" />
        {today}
      </div>
    </header>
  );
}
