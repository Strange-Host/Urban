import {
  actionBtn,
  closeBtn,
  dangerActionBtn,
  modalBox,
  primaryBtn,
} from "../../components/styles";

export const addOrderBtn = primaryBtn;

export const editBtn = actionBtn;

export const noteBtn = actionBtn;

export const cashPaymentBtn = {
  ...actionBtn,
  border: "1px solid transparent",
  background: "var(--success)",
  color: "#fff",
};

export const addItemBtn = { ...primaryBtn, height: 42 };

export const createBtn = primaryBtn;

export const removeBtn = dangerActionBtn;

export const modalBoxWide = { ...modalBox, width: 760 };

export const modalBoxSmall = modalBox;

export const xBtn = closeBtn;

export const saveBtn = primaryBtn;

export const sectionTitle = {
  fontSize: 12,
  fontWeight: 600,
  textTransform: "uppercase",
  letterSpacing: "0.05em",
  color: "var(--text-muted)",
  marginBottom: 10,
};
