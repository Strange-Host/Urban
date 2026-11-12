import React from "react";
import { statTones } from "./crmUtils";

export function StatCard({ title, value, icon: Icon, tone = "slate" }) {
  return (
    <div className="ui-card p-4 transition-shadow hover:shadow-md">
      <div className="flex items-center justify-between gap-2">
        <div className="text-slate-500 text-xs sm:text-sm font-medium">{title}</div>
        {Icon && (
          <span className={`grid h-8 w-8 place-items-center rounded-lg ${statTones[tone]}`}>
            <Icon size={15} />
          </span>
        )}
      </div>
      <div className="text-xl sm:text-2xl font-bold tracking-tight text-slate-900 mt-1.5">{value}</div>
    </div>
  );
}
