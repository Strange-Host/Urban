import { expect, test } from "@playwright/test";
import { ADMIN_TOKEN, LEAD, mockApi, signIn } from "./support";

test.describe("crm journey", () => {
  test("lists leads with their status", async ({ page }) => {
    await signIn(page, ADMIN_TOKEN);
    await mockApi(page, { leads: [LEAD] });

    await page.goto("/crm");

    await expect(page.getByRole("heading", { name: "CRM" })).toBeVisible();
    // Desktop viewport shows the table; the card list is lg:hidden.
    await expect(page.getByRole("cell", { name: "Brian Otieno", exact: true })).toBeVisible();
    await expect(page.getByRole("cell", { name: "Mombasa", exact: true })).toBeVisible();
  });

  test("opens the add-lead form", async ({ page }) => {
    await signIn(page, ADMIN_TOKEN);
    await mockApi(page, { leads: [] });

    await page.goto("/crm");
    await page.getByRole("button", { name: /add lead/i }).click();

    await expect(page.getByRole("heading", { name: "Add Lead" })).toBeVisible();
    await expect(page.getByText("Name *")).toBeVisible();
    await expect(page.getByText("Phone Number *")).toBeVisible();
  });

  test("filters leads by tab", async ({ page }) => {
    await signIn(page, ADMIN_TOKEN);
    await mockApi(page, { leads: [LEAD] });

    await page.goto("/crm");
    await page.getByRole("button", { name: "Converted" }).click();

    await expect(page.getByRole("button", { name: "Converted" })).toHaveClass(/bg-brand-600/);
  });
});
