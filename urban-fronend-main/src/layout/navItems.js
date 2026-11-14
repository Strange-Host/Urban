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
  FiSettings,
} from "react-icons/fi";

export const navSections = [
  {
    label: "Overview",
    items: [
      { to: "/dashboard", label: "Dashboard", icon: FiGrid },
      { to: "/crm", label: "CRM", icon: FiUsers, permission: "CAN_VIEW_LEADS" },
    ],
  },
  {
    label: "Sales",
    items: [
      {
        to: "/orders",
        label: "Orders",
        icon: FiShoppingBag,
        permission: "CAN_VIEW_ORDERS",
      },
      {
        to: "/dispatch-scanner",
        label: "Dispatch Scanner",
        icon: FiMaximize,
        permission: "CAN_UPDATE_ORDER_STATUS",
      },
      {
        to: "/couriers",
        label: "Couriers",
        icon: FiTruck,
        permission: "CAN_VIEW_COURIERS",
      },
      {
        to: "/payments",
        label: "Payments",
        icon: FiCreditCard,
        permission: "CAN_VIEW_PAYMENTS",
      },
    ],
  },
  {
    label: "Catalog",
    items: [
      {
        to: "/products",
        label: "Products",
        icon: FiBox,
        permission: "CAN_VIEW_PRODUCTS",
      },
      {
        to: "/categories",
        label: "Categories",
        icon: FiTag,
        permission: "CAN_VIEW_CATEGORIES",
      },
      {
        to: "/inventory",
        label: "Inventory",
        icon: FiArchive,
        permission: "CAN_VIEW_INVENTORY",
      },
    ],
  },
  {
    label: "Finance",
    items: [
      {
        to: "/requisitions",
        label: "Requisitions",
        icon: FiClipboard,
        permission: "CAN_VIEW_REQUISITIONS",
      },
      {
        to: "/expenses",
        label: "Expenses",
        icon: FiDollarSign,
        permission: "CAN_VIEW_EXPENSES",
      },
    ],
  },
  {
    label: "Team",
    items: [
      {
        to: "/staff",
        label: "Staff",
        icon: FiUserCheck,
        permission: "CAN_VIEW_STAFF",
      },
      {
        to: "/staff-logs",
        label: "Staff Logs",
        icon: FiActivity,
        permission: "CAN_VIEW_STAFF_LOGS",
      },
    ],
  },
  {
    label: "Admin",
    items: [{ to: "/settings", label: "Settings", icon: FiSettings, adminOnly: true }],
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
