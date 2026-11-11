import {
  FiGrid,
  FiUsers,
  FiShoppingBag,
  FiTruck,
  FiClipboard,
  FiTag,
  FiBox,
  FiDollarSign,
  FiCreditCard,
  FiArchive,
  FiUserCheck,
  FiActivity,
  FiMaximize,
} from "react-icons/fi";

export const navSections = [
  {
    label: "Overview",
    items: [
      { to: "/dashboard", label: "Dashboard", icon: FiGrid },
      { to: "/crm", label: "CRM", icon: FiUsers },
    ],
  },
  {
    label: "Sales",
    items: [
      { to: "/orders", label: "Orders", icon: FiShoppingBag },
      {
        to: "/dispatch-scanner",
        label: "Dispatch Scanner",
        icon: FiMaximize,
        permission: "CAN_UPDATE_ORDER_STATUS",
      },
      { to: "/couriers", label: "Couriers", icon: FiTruck },
      { to: "/payments", label: "Payments", icon: FiCreditCard },
    ],
  },
  {
    label: "Catalog",
    items: [
      { to: "/products", label: "Products", icon: FiBox },
      { to: "/categories", label: "Categories", icon: FiTag },
      { to: "/inventory", label: "Inventory", icon: FiArchive },
    ],
  },
  {
    label: "Finance",
    items: [
      { to: "/requisitions", label: "Requisitions", icon: FiClipboard },
      { to: "/expenses", label: "Expenses", icon: FiDollarSign },
    ],
  },
  {
    label: "Team",
    items: [
      { to: "/staff", label: "Staff", icon: FiUserCheck },
      { to: "/staff-logs", label: "Staff Logs", icon: FiActivity },
    ],
  },
];

const allItems = navSections.flatMap((s) => s.items);

export function titleForPath(pathname) {
  if (pathname === "/requisitions/new") return "New Requisition";
  if (pathname.startsWith("/staff-details")) return "Staff Details";
  if (/^\/staff\/[^/]+/.test(pathname)) return "Staff Details";

  const match = allItems
    .filter((i) => pathname === i.to || pathname.startsWith(`${i.to}/`))
    .sort((a, b) => b.to.length - a.to.length)[0];

  return match?.label || "Urban Trends";
}
