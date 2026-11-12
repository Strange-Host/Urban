import { expect, test } from "@playwright/test";
import { ADMIN_TOKEN, CASHIER_TOKEN, ORDER, mockApi, signIn } from "./support";

test.describe("orders journey", () => {
  test("lists the customer's order", async ({ page }) => {
    await signIn(page, ADMIN_TOKEN);
    await mockApi(page, { orders: [ORDER] });

    await page.goto("/orders");

    await expect(page.getByRole("heading", { name: "Orders" })).toBeVisible();
    await expect(page.getByText("Amina Yusuf").first()).toBeVisible();
    await expect(page.getByText("Leather Bag (×1)").first()).toBeVisible();
    await expect(page.getByText("Nairobi").first()).toBeVisible();
  });

  test("opens the create-order form", async ({ page }) => {
    await signIn(page, ADMIN_TOKEN);
    await mockApi(page, { orders: [] });

    await page.goto("/orders");
    await page.getByRole("button", { name: /add order/i }).click();

    await expect(page.getByRole("heading", { name: "Create Order" })).toBeVisible();
    await expect(page.getByRole("button", { name: /create order/i })).toBeVisible();
  });

  test("filters the list by status", async ({ page }) => {
    await signIn(page, ADMIN_TOKEN);
    await mockApi(page, { orders: [ORDER] });

    await page.goto("/orders");
    await page.locator(".orders-filter select").selectOption("DISPATCHED_PAID");

    await expect(page).toHaveURL(/\/orders$/);
    await expect(page.getByText("Amina Yusuf").first()).toBeVisible();
  });

  test("hides navigation the cashier has no permission for", async ({ page }) => {
    await signIn(page, CASHIER_TOKEN);
    await mockApi(page, { orders: [ORDER] });

    await page.goto("/orders");

    await expect(page.getByRole("link", { name: "Orders" })).toBeVisible();
    await expect(page.getByRole("link", { name: "Staff", exact: true })).toHaveCount(0);
    await expect(page.getByRole("link", { name: "Expenses" })).toHaveCount(0);
  });
});
