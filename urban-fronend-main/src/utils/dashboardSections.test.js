import { describe, expect, it } from "vitest";
import { summariseSections } from "./dashboardSections";

const ok = { status: "fulfilled", value: {} };
const forbidden = { status: "rejected", reason: { response: { status: 403, data: { detail: "Access Denied" } } } };
const broken = { status: "rejected", reason: { response: { status: 500, data: { detail: "Database is down" } } } };

describe("summariseSections", () => {
  it("quietly hides sections the user may not see", () => {
    const { denied, error } = summariseSections(["orders", "profit", "topSelling"], [ok, forbidden, forbidden]);

    expect(denied).toEqual({ profit: true, topSelling: true });
    expect(error).toBe("");
  });

  it("reports real failures with the server's reason", () => {
    const { denied, error } = summariseSections(["orders", "profit"], [broken, ok]);

    expect(denied).toEqual({});
    expect(error).toBe("Database is down");
  });

  it("is empty when everything loaded", () => {
    expect(summariseSections(["a"], [ok])).toEqual({ denied: {}, error: "" });
  });
});
