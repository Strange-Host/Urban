import { renderHook } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";
import { useAuth } from "./useAuth";

function makeToken(payload) {
  return `header.${btoa(JSON.stringify(payload))}.signature`;
}

describe("useAuth", () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it("returns no user without a token", () => {
    const { result } = renderHook(() => useAuth());

    expect(result.current.user).toBeNull();
    expect(result.current.can("CAN_VIEW_ORDERS")).toBe(false);
    expect(result.current.isAdmin()).toBe(false);
  });

  it("returns no user for an undecodable token", () => {
    localStorage.setItem("accessToken", "not-a-jwt");

    const { result } = renderHook(() => useAuth());

    expect(result.current.user).toBeNull();
  });

  it("reads the role and permissions from the authorities claim", () => {
    localStorage.setItem(
      "accessToken",
      makeToken({
        sub: "manager@urban.test",
        firstName: "Mia",
        lastName: "Manager",
        authorities: ["ROLE_MANAGER", "CAN_VIEW_ORDERS", "CAN_CREATE_ORDERS"],
      })
    );

    const { result } = renderHook(() => useAuth());

    expect(result.current.user).toMatchObject({
      email: "manager@urban.test",
      name: "Mia Manager",
      role: "MANAGER",
      permissions: ["CAN_VIEW_ORDERS", "CAN_CREATE_ORDERS"],
    });
    expect(result.current.can("CAN_VIEW_ORDERS")).toBe(true);
    expect(result.current.can("CAN_VIEW_STAFF")).toBe(false);
    expect(result.current.isManager()).toBe(true);
    expect(result.current.isAdmin()).toBe(false);
  });

  it("accepts authorities serialized as objects", () => {
    localStorage.setItem(
      "accessToken",
      makeToken({
        sub: "admin@urban.test",
        authorities: [{ authority: "ROLE_ADMIN" }, { authority: "CAN_DELETE_ORDERS" }],
      })
    );

    const { result } = renderHook(() => useAuth());

    expect(result.current.isAdmin()).toBe(true);
    expect(result.current.can("CAN_DELETE_ORDERS")).toBe(true);
  });
});
