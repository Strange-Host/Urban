import { describe, expect, it } from "vitest";
import { buildReceiptHtml } from "./printer";

const baseOrder = {
  id: "abcdef12-0000-0000-0000-000000000000",
  createdAt: "2026-11-10T10:00:00",
  orderStatus: "DISPATCHED_UNPAID",
  customerName: "Jane",
  phoneNumber: "0712345678",
  deliveryCity: "Nairobi",
  accountNumber: "8025631#A01BC",
  totalAmount: 1500,
  servedBy: "Staff",
  items: [{ productName: "Shirt", quantity: 1 }],
};

describe("buildReceiptHtml", () => {
  it("escapes every customer-controlled field", () => {
    const payload = '<img src=x onerror="alert(1)">';
    const html = buildReceiptHtml(
      {
        ...baseOrder,
        customerName: payload,
        phoneNumber: payload,
        deliveryCity: payload,
        accountNumber: payload,
        orderStatus: payload,
        servedBy: payload,
      },
      "123456"
    );

    expect(html).not.toContain("<img");
    expect(html).toContain("&lt;img");
  });

  it("escapes product names", () => {
    const html = buildReceiptHtml(
      { ...baseOrder, items: [{ productName: "<script>x()</script>", quantity: 1 }] },
      "123456"
    );
    expect(html).not.toContain("<script>x()");
  });

  it("shows the configured paybill, escaped", () => {
    const html = buildReceiptHtml(baseOrder, "123456");
    expect(html).toContain("M-Pesa Paybill");
    expect(html).toContain("123456");
  });

  it("omits the paybill row instead of printing a guessed number", () => {
    const html = buildReceiptHtml(baseOrder, "");
    expect(html).not.toContain("M-Pesa Paybill");
    expect(html).not.toContain("522522");
  });
});
