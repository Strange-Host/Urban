import { navSections } from "./navItems";

// Pages that are not in the menu but still need a permission.
const EXTRA_RULES = {
  "/requisitions/new": { permission: "CAN_CREATE_REQUISITIONS" },
};

const allNavItems = () => navSections.flatMap((section) => section.items);

function allowed(rule, can, user) {
  if (rule.permission && !can(rule.permission)) return false;
  if (rule.anyPermission && !rule.anyPermission.some((p) => can(p))) return false;
  if (rule.adminOnly && user?.role !== "ADMIN") return false;
  return true;
}

/**
 * Whether the signed-in user may open this page. The menu already hides pages a user cannot
 * use, but typing the address (or following an old link) used to open them anyway and show
 * confusing errors from the API.
 */
export function canAccessPath(pathname, can, user) {
  const path = pathname.replace(/\/+$/, "") || "/";

  if (EXTRA_RULES[path]) return allowed(EXTRA_RULES[path], can, user);

  // Longest matching menu entry wins ("/crm/leads/5" belongs to "/crm").
  const match = allNavItems()
    .filter((item) => path === item.to || path.startsWith(`${item.to}/`))
    .sort((a, b) => b.to.length - a.to.length)[0];

  return match ? allowed(match, can, user) : true;
}
