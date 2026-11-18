import { describe, expect, it } from "vitest";
import { buildStaffRequest, splitName } from "./staffForm";

const valid = {
  fullName: "Jane Wanjiku",
  email: " Jane@Company.com ",
  phone: "0712 345 678",
  password: "longenough1",
  role: "STAFF",
};

describe("buildStaffRequest", () => {
  it("builds a clean request: lower-cased email, canonical phone, split name", () => {
    expect(buildStaffRequest(valid).request).toEqual({
      firstName: "Jane",
      lastName: "Wanjiku",
      email: "jane@company.com",
      phoneNumber: "0712345678",
      password: "longenough1",
      role: "STAFF",
    });
  });

  it("accepts a one-word name with an empty last name", () => {
    const result = buildStaffRequest({ ...valid, fullName: "Jane" });
    expect(result.error).toBeUndefined();
    expect(result.request.firstName).toBe("Jane");
    expect(result.request.lastName).toBe("");
  });

  it("still asks for a name when the field is blank", () => {
    expect(buildStaffRequest({ ...valid, fullName: "  " }).error).toContain("name");
  });

  it("keeps multi-part surnames together", () => {
    expect(splitName("Jane  Wanjiku   Kamau")).toEqual({ firstName: "Jane", lastName: "Wanjiku Kamau" });
  });

  it("rejects a bad email, a phone that cannot receive the login code, and a short password", () => {
    expect(buildStaffRequest({ ...valid, email: "jane" }).error).toContain("email");
    expect(buildStaffRequest({ ...valid, phone: "12345" }).error).toContain("Kenyan mobile");
    expect(buildStaffRequest({ ...valid, password: "short" }).error).toContain("8 characters");
    expect(buildStaffRequest({ ...valid, password: "onlyletters" }).error).toContain("a letter and a number");
  });
});
