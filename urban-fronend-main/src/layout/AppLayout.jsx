import React, { useEffect, useState } from "react";
import { Outlet, useLocation } from "react-router-dom";
import Sidebar from "./Sidebar";
import Topbar from "./Topbar";
import LogoutModal from "../pages/logoutModal";

export default function AppLayout() {
  const [mobileOpen, setMobileOpen] = useState(false);
  const [showLogoutModal, setShowLogoutModal] = useState(false);
  const { pathname } = useLocation();

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
            <Outlet />
          </div>
        </main>
      </div>

      {/* Logout Modal - rendered at top level */}
      <LogoutModal isOpen={showLogoutModal} onClose={() => setShowLogoutModal(false)} />
    </div>
  );
}
