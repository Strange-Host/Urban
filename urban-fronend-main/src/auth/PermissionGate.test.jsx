import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";
import PermissionGate from "./PermissionGate";

function signIn(authorities, role) {
  const payload = { sub: "user@urban.test", role, authorities };
  localStorage.setItem("accessToken", `header.${btoa(JSON.stringify(payload))}.signature`);
}

describe("PermissionGate", () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it("renders children when the permission is granted", () => {
    signIn(["ROLE_MANAGER", "CAN_VIEW_ORDERS"], "MANAGER");

    render(
      <PermissionGate permission="CAN_VIEW_ORDERS">
        <button>Open orders</button>
      </PermissionGate>
    );

    expect(screen.getByRole("button", { name: "Open orders" })).toBeInTheDocument();
  });

  it("renders the fallback when the permission is missing", () => {
    signIn(["ROLE_CASHIER"], "CASHIER");

    render(
      <PermissionGate permission="CAN_VIEW_STAFF" fallback={<span>No access</span>}>
        <button>Open staff</button>
      </PermissionGate>
    );

    expect(screen.queryByRole("button", { name: "Open staff" })).not.toBeInTheDocument();
    expect(screen.getByText("No access")).toBeInTheDocument();
  });

  it("gates on role independently of permissions", () => {
    signIn(["ROLE_ADMIN"], "ADMIN");

    const { container } = render(
      <PermissionGate role="MANAGER" fallback={<span>Managers only</span>}>
        <span>Manager area</span>
      </PermissionGate>
    );

    expect(screen.getByText("Managers only")).toBeInTheDocument();
    expect(container.textContent).not.toContain("Manager area");
  });
});
