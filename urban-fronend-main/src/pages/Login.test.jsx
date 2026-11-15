import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("../api/authApi", () => ({ loginUser: vi.fn() }));

import { loginUser } from "../api/authApi";
import Login from "./Login";

function renderLogin() {
  return render(
    <MemoryRouter>
      <Login />
    </MemoryRouter>
  );
}

function submit(email, password) {
  fireEvent.change(document.querySelector('input[name="email"]'), { target: { value: email } });
  fireEvent.change(document.querySelector('input[name="password"]'), { target: { value: password } });
  fireEvent.submit(document.querySelector("form"));
}

describe("Login errors", () => {
  beforeEach(() => {
    loginUser.mockReset();
  });

  it("shows the reason the server gives, not a generic message", async () => {
    loginUser.mockRejectedValue({
      response: { status: 401, data: { title: "Unauthorized", detail: "Invalid email or password" } },
    });

    renderLogin();
    submit("jane@example.com", "wrong");

    await waitFor(() => expect(screen.getByText("Invalid email or password")).toBeTruthy());
  });

  it("explains a cooldown instead of saying the login failed", async () => {
    loginUser.mockRejectedValue({
      response: { status: 400, data: { detail: "Please wait before requesting another OTP" } },
    });

    renderLogin();
    submit("jane@example.com", "secret");

    await waitFor(() =>
      expect(screen.getByText("Please wait before requesting another OTP")).toBeTruthy()
    );
  });

  it("says so when the server cannot be reached", async () => {
    loginUser.mockRejectedValue({ request: {} });

    renderLogin();
    submit("jane@example.com", "secret");

    await waitFor(() => expect(screen.getByText(/Cannot reach the server/)).toBeTruthy());
  });
});
