import React, { Suspense, useEffect, useState } from "react";
import { Link, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "../auth/useAuth";
import { canAccessPath } from "./routeAccess";
import Sidebar from "./Sidebar";
import Topbar from "./Topbar";
import LogoutModal from "../pages/logoutModal";

export default function AppLayout() {
  const [mobileOpen, setMobileOpen] = useState(false);
  const [showLogoutModal, setShowLogoutModal] = useState(false);
  const { pathname } = useLocation();
  const { can, user } = useAuth();
  const hasAccess = canAccessPath(pathname, can, user);

  // Close the drawer whenever the route changes
  useEffect(() => {
    setMobileOpen(false);
  }, [pathname]);

  return (
    <div className="flex h-screen w-full min-w-0 max-w-full overflow-hidden bg-[var(--app-bg)]">
      <Sidebar onShowLogoutModal={() => setShowLogoutModal(true)} />

      {/* Mobile Sidebar Drawer */}
      {mobileOpen && (
        <div
          className="fixed inset-0 z-50 animate-fade-in bg-slate-900/50 backdrop-blur-sm lg:hidden"
          onClick={() => setMobileOpen(false)}
        >
          <div className="h-full w-[260px] shadow-2xl" onClick={(e) => e.stopPropagation()}>
            <Sidebar
              mobile={true}
              onClose={() => setMobileOpen(false)}
              onShowLogoutModal={() => setShowLogoutModal(true)}
            />
          </div>
        </div>
      )}

      {/* Right side */}
      <div className="flex min-w-0 flex-1 flex-col">
        <Topbar onMenuClick={() => setMobileOpen(true)} />

        <main className="min-w-0 flex-1 overflow-y-auto overflow-x-hidden">
          <div className="mx-auto w-full max-w-[1600px] animate-fade-in p-4 sm:p-6 lg:p-8">
            <Suspense
              fallback={
                <div className="grid min-h-[60vh] place-items-center text-sm font-medium text-slate-500">
                  Loading...
                </div>
              }
            >
              {hasAccess ? (
                <Outlet />
              ) : (
                <div className="grid min-h-[50vh] place-items-center text-center">
                  <div>
                    <h2 className="m-0 text-xl font-semibold text-slate-900">You do not have access to this page</h2>
                    <p className="mt-2 text-sm text-slate-500">
                      Ask an admin to grant you the permission if you need it.
                    </p>
                    <Link to="/dashboard" className="mt-4 inline-block text-sm font-medium text-indigo-600">
                      Back to the dashboard
                    </Link>
                  </div>
                </div>
              )}
            </Suspense>
          </div>
        </main>
      </div>

      {/* Logout Modal - rendered at top level */}
      <LogoutModal isOpen={showLogoutModal} onClose={() => setShowLogoutModal(false)} />
    </div>
  );
}
