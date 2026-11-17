import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";

vi.mock("../auth/useAuth", () => ({ useAuth: vi.fn() }));
vi.mock("./Sidebar", () => ({ default: () => <nav /> }));
vi.mock("./Topbar", () => ({ default: () => <header /> }));
vi.mock("../pages/logoutModal", () => ({ default: () => null }));

import { useAuth } from "../auth/useAuth";
import AppLayout from "./AppLayout";

function renderAt(path) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route element={<AppLayout />}>
          <Route path="/crm" element={<div>CRM page</div>} />
          <Route path="/dashboard" element={<div>Dashboard page</div>} />
        </Route>
      </Routes>
    </MemoryRouter>
  );
}

describe("AppLayout access guard", () => {
  it("shows a clear message instead of the page when the user lacks the permission", () => {
    useAuth.mockReturnValue({ can: () => false, user: { role: "STAFF" } });

    renderAt("/crm");

    expect(screen.getByText("You do not have access to this page")).toBeTruthy();
    expect(screen.queryByText("CRM page")).toBeNull();
  });

  it("renders the page when the user has the permission", () => {
    useAuth.mockReturnValue({ can: (p) => p === "CAN_VIEW_LEADS", user: { role: "STAFF" } });

    renderAt("/crm");

    expect(screen.getByText("CRM page")).toBeTruthy();
  });

  it("always renders the dashboard", () => {
    useAuth.mockReturnValue({ can: () => false, user: { role: "STAFF" } });

    renderAt("/dashboard");

    expect(screen.getByText("Dashboard page")).toBeTruthy();
  });
});
