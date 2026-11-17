import { normalizeKenyanPhone } from "./phone";

/** Splits "Jane Wanjiku Kamau" into first name and the rest. */
export function splitName(fullName) {
  const parts = String(fullName || "").trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return { firstName: "", lastName: "" };
  return { firstName: parts[0], lastName: parts.slice(1).join(" ") };
}

/**
 * Checks the "Add staff" form and returns either the request to send or a message that tells
 * the person exactly what to fix (the server only said "Failed to add staff").
 */
export function buildStaffRequest(form) {
  const { firstName, lastName } = splitName(form.fullName);

  if (!firstName || !lastName) {
    return { error: "Enter both a first and a last name, for example Jane Wanjiku." };
  }

  const email = String(form.email || "").trim();
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    return { error: "Enter a valid email address." };
  }

  // The login code is sent by SMS to this number, so it has to be a real mobile number.
  const phoneNumber = normalizeKenyanPhone(form.phone);
  if (!phoneNumber) {
    return { error: "Enter a valid Kenyan mobile number, for example 0712 345 678. Login codes are sent to it." };
  }

  if (String(form.password || "").length < 8) {
    return { error: "Use a password of at least 8 characters." };
  }

  return {
    request: {
      firstName,
      lastName,
      email: email.toLowerCase(),
      phoneNumber,
      password: form.password,
      role: form.role,
    },
  };
}
