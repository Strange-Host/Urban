import { describe, expect, it } from "vitest";
import { getErrorDetail } from "./errors";

describe("getErrorDetail", () => {
  it("prefers the problem-detail text from the backend", () => {
    const error = { response: { data: { title: "Bad Request", detail: "Insufficient stock" } } };
    expect(getErrorDetail(error, "fallback")).toBe("Insufficient stock");
  });

  it("falls back to message, plain text, then the supplied default", () => {
    expect(getErrorDetail({ response: { data: { message: "old style" } } }, "f")).toBe("old style");
    expect(getErrorDetail({ response: { data: "plain" } }, "f")).toBe("plain");
    expect(getErrorDetail({ response: { data: {} } }, "fallback")).toBe("fallback");
    expect(getErrorDetail(new Error("network"), "fallback")).toBe("fallback");
  });
});
