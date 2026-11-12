import { expect, test } from "@playwright/test";
import { ADMIN_TOKEN, mockApi, signIn } from "./support";

test.describe("route protection", () => {
  test("sends signed-out visitors to the login screen", async ({ page }) => {
    await page.goto("/orders");

    await expect(page).toHaveURL(/\/login$/);
    await expect(page.getByRole("heading", { name: "Welcome back" })).toBeVisible();
  });

  test("keeps signed-in visitors inside the app shell", async ({ page }) => {
    await signIn(page, ADMIN_TOKEN);
    await mockApi(page);
    await page.goto("/");

    await expect(page).toHaveURL(/\/dashboard$/);
    await expect(page.getByRole("heading", { name: "Dashboard" })).toBeVisible();
  });

  test("does not send signed-in visitors back to login", async ({ page }) => {
    await signIn(page, ADMIN_TOKEN);
    await mockApi(page);
    await page.goto("/login");

    await expect(page).toHaveURL(/\/dashboard$/);
  });
});
