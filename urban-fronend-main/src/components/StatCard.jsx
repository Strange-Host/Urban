import React from "react";

const tones = {
  green: { icon: "bg-emerald-50 text-emerald-600", value: "text-slate-900" },
  blue: { icon: "bg-blue-50 text-blue-600", value: "text-slate-900" },
  orange: { icon: "bg-amber-50 text-amber-600", value: "text-slate-900" },
  red: { icon: "bg-rose-50 text-rose-600", value: "text-slate-900" },
  indigo: { icon: "bg-brand-50 text-brand-600", value: "text-slate-900" },
  slate: { icon: "bg-slate-100 text-slate-600", value: "text-slate-900" },
};

export default function StatCard({ title, value, tone = "green", icon: Icon, hint }) {
  const t = tones[tone] || tones.green;

  return (
    <div className="ui-card p-5 transition-shadow hover:shadow-md">
      <div className="flex items-start justify-between gap-3">
        <p className="m-0 text-sm font-medium text-slate-500">{title}</p>
        {Icon && (
          <span className={`grid h-9 w-9 shrink-0 place-items-center rounded-xl ${t.icon}`}>
            <Icon size={18} />
          </span>
        )}
      </div>
      <div className={`mt-2 text-2xl font-bold tracking-tight break-words ${t.value}`}>{value}</div>
      {hint && <div className="mt-1 text-xs text-slate-500">{hint}</div>}
    </div>
  );
}
