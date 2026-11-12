import { expect, test } from "@playwright/test";
import { mockApi } from "./support";

test.describe("sign-in journey", () => {
  test("shows a validation error when submitted empty", async ({ page }) => {
    await mockApi(page);
    await page.goto("/login");

    await page.getByRole("button", { name: /sign in/i }).click();

    await expect(page.getByText("Please enter email and password.")).toBeVisible();
  });

  test("surfaces a rejected sign-in", async ({ page }) => {
    await mockApi(page, { loginStatus: 401, loginBody: { detail: "Authentication failed" } });
    await page.goto("/login");

    await page.getByLabel("Email").fill("cashier@urban.test");
    await page.getByLabel("Password", { exact: true }).fill("wrong-password");
    await page.getByRole("button", { name: /sign in/i }).click();

    await expect(page.getByText("Login failed.")).toBeVisible();
    await expect(page).toHaveURL(/\/login$/);
  });

  test("moves to the OTP screen after a correct password", async ({ page }) => {
    await mockApi(page);
    await page.goto("/login");

    await page.getByLabel("Email").fill("cashier@urban.test");
    await page.getByLabel("Password", { exact: true }).fill("Password123!");
    await page.getByRole("button", { name: /sign in/i }).click();

    await expect(page).toHaveURL(/\/verify-otp$/);
    await expect(page.getByRole("heading", { name: "Verify it's you" })).toBeVisible();
    await expect(page.getByText("cashier@urban.test")).toBeVisible();
  });

  test("lands on the dashboard after verifying the OTP", async ({ page }) => {
    await mockApi(page);
    await page.addInitScript(() => localStorage.setItem("otp_email", "cashier@urban.test"));
    await page.goto("/verify-otp");

    await page.getByLabel("Verification code").fill("123456");
    await page.getByRole("button", { name: /verify code/i }).click();

    await expect(page).toHaveURL(/\/dashboard$/);
    await expect(page.getByRole("heading", { name: "Dashboard" })).toBeVisible();
    await expect(page.getByText("Urban Trends").first()).toBeVisible();
  });
});
