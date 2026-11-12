export const statTones = {
  slate: "bg-slate-100 text-slate-600",
  blue: "bg-sky-50 text-sky-600",
  green: "bg-emerald-50 text-emerald-600",
  violet: "bg-violet-50 text-violet-600",
  red: "bg-rose-50 text-rose-600",
  amber: "bg-amber-50 text-amber-600",
};

export const formatDateTime = (dateString) => {
  if (!dateString) return "";
  const date = new Date(dateString);
  return `${date.toLocaleDateString("en-CA")} ${date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}`;
};

export const formatPhoneLink = (value) => {
  if (!value) return "";
  const cleaned = String(value).trim().replace(/[^\d+]/g, "");
  return cleaned ? `tel:${cleaned}` : "";
};

export const formatProductOptions = (productOptionsJson) => {
  if (!productOptionsJson) return null;
  let parsed;
  try {
    parsed = JSON.parse(productOptionsJson);
  } catch {
    return null;
  }
  if (!parsed || Object.keys(parsed).length === 0) return null;

  const parts = [];
  for (const [key, value] of Object.entries(parsed)) {
    const lowerKey = key.toLowerCase();
    const label = lowerKey.includes("color")
      ? "Colors"
      : lowerKey.includes("size")
      ? "Size"
      : key;
    const displayValue = Array.isArray(value) ? value.join(", ") : value;
    if (displayValue) parts.push(`${label}: ${displayValue}`);
  }
  return parts.length > 0 ? parts.join(" · ") : null;
};
