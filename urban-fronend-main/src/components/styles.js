// Shared inline-style objects for pages that style with `style={}`.
// Colors reference the CSS variables defined in index.css.

export const inputStyle = {
  width: "100%",
  boxSizing: "border-box",
  border: "1px solid var(--border)",
  borderRadius: 10,
  padding: "10px 12px",
  background: "#fff",
  fontSize: 14,
  outline: "none",
};

export const labelStyle = {
  display: "block",
  marginBottom: 6,
  fontSize: 13,
  fontWeight: 600,
  color: "#334155",
};

const btnBase = {
  display: "inline-flex",
  alignItems: "center",
  justifyContent: "center",
  gap: 8,
  borderRadius: 10,
  padding: "10px 16px",
  fontSize: 14,
  fontWeight: 600,
  lineHeight: 1.2,
  whiteSpace: "nowrap",
};

export const primaryBtn = {
  ...btnBase,
  border: "1px solid transparent",
  background: "var(--brand)",
  color: "#fff",
  boxShadow: "0 1px 2px rgba(79,70,229,0.25)",
};

export const secondaryBtn = {
  ...btnBase,
  border: "1px solid var(--border)",
  background: "#fff",
  color: "#334155",
};

export const dangerBtn = {
  ...btnBase,
  border: "1px solid transparent",
  background: "var(--danger)",
  color: "#fff",
};

export const successBtn = {
  ...btnBase,
  border: "1px solid transparent",
  background: "var(--success)",
  color: "#fff",
};

export const actionBtn = {
  display: "inline-flex",
  alignItems: "center",
  gap: 6,
  border: "1px solid var(--border)",
  background: "#fff",
  color: "#334155",
  padding: "6px 10px",
  borderRadius: 8,
  fontWeight: 600,
  fontSize: 12.5,
  lineHeight: 1.2,
  whiteSpace: "nowrap",
};

export const dangerActionBtn = {
  ...actionBtn,
  background: "#fef2f2",
  borderColor: "#fecaca",
  color: "#b91c1c",
};

export const pageBtnStyle = {
  display: "inline-flex",
  alignItems: "center",
  gap: 4,
  border: "1px solid var(--border)",
  background: "#fff",
  color: "#334155",
  borderRadius: 8,
  padding: "7px 12px",
  fontSize: 13,
  fontWeight: 600,
};

export const modalOverlay = {
  position: "fixed",
  inset: 0,
  background: "rgba(15,23,42,0.5)",
  backdropFilter: "blur(3px)",
  WebkitBackdropFilter: "blur(3px)",
  display: "grid",
  placeItems: "center",
  padding: 16,
  zIndex: 999,
  animation: "ui-fade-in 0.18s ease-out",
};

export const modalBox = {
  width: 520,
  maxWidth: "100%",
  maxHeight: "calc(100vh - 32px)",
  overflowY: "auto",
  boxSizing: "border-box",
  background: "#fff",
  borderRadius: 18,
  padding: 24,
  boxShadow: "var(--shadow-lg)",
  animation: "ui-pop-in 0.2s cubic-bezier(0.16, 1, 0.3, 1)",
};

export const modalTitle = {
  margin: "0 0 16px",
  fontSize: 18,
  fontWeight: 700,
  color: "var(--text)",
};

export const modalFooter = {
  display: "flex",
  justifyContent: "flex-end",
  gap: 10,
  marginTop: 20,
  flexWrap: "wrap",
};

export const closeBtn = {
  display: "grid",
  placeItems: "center",
  width: 34,
  height: 34,
  border: "1px solid var(--border)",
  background: "#fff",
  color: "#64748b",
  borderRadius: 10,
  fontSize: 16,
};

export const tableCard = {
  background: "#fff",
  border: "1px solid var(--border)",
  borderRadius: 16,
  overflow: "hidden",
  boxShadow: "var(--shadow-sm)",
};

export const searchBox = {
  display: "flex",
  alignItems: "center",
  gap: 10,
  border: "1px solid var(--border)",
  background: "#fff",
  padding: "0 14px",
  height: 42,
  borderRadius: 10,
  minWidth: 280,
  boxSizing: "border-box",
};

export const searchInput = {
  border: "none",
  outline: "none",
  background: "transparent",
  width: "100%",
  fontSize: 14,
  height: "100%",
};

export const alertError = {
  background: "#fef2f2",
  color: "#b91c1c",
  border: "1px solid #fecaca",
  padding: "10px 14px",
  borderRadius: 10,
  fontSize: 14,
  fontWeight: 500,
};

export const alertSuccess = {
  background: "#f0fdf4",
  color: "#15803d",
  border: "1px solid #bbf7d0",
  padding: "10px 14px",
  borderRadius: 10,
  fontSize: 14,
  fontWeight: 500,
};

const badgeTones = {
  green: { background: "#dcfce7", color: "#166534" },
  red: { background: "#fee2e2", color: "#991b1b" },
  amber: { background: "#fef3c7", color: "#92400e" },
  blue: { background: "#dbeafe", color: "#1e40af" },
  indigo: { background: "#e0e7ff", color: "#3730a3" },
  gray: { background: "#f1f5f9", color: "#475569" },
};

export function badgeStyle(tone = "gray") {
  return {
    display: "inline-flex",
    alignItems: "center",
    gap: 4,
    padding: "3px 10px",
    borderRadius: 999,
    fontSize: 12,
    fontWeight: 600,
    whiteSpace: "nowrap",
    ...(badgeTones[tone] || badgeTones.gray),
  };
}
