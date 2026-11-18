// Mirrors PasswordPolicy on the server so the form can explain the rule before submitting.
export const PASSWORD_MIN_LENGTH = 8;
export const PASSWORD_HINT = `At least ${PASSWORD_MIN_LENGTH} characters, with a letter and a number.`;

export function isAcceptablePassword(password) {
  const value = String(password || "");
  if (value.trim().length < PASSWORD_MIN_LENGTH) return false;
  return /\p{L}/u.test(value) && /\p{N}/u.test(value);
}
