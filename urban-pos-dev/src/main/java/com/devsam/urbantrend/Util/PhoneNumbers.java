package com.devsam.urbantrend.Util;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Kenyan phone numbers arrive as 0712345678, 0712 345 678, 254712345678 and +254712345678 for
 * the same person. Matching customers on the raw text created duplicates, so phones are
 * compared in one canonical form.
 */
public final class PhoneNumbers {

    private PhoneNumbers() {
    }

    /** Canonical form: 07XXXXXXXX / 01XXXXXXXX for Kenyan mobiles, cleaned text otherwise. */
    public static String normalize(String raw) {
        String cleaned = clean(raw);
        if (cleaned == null) {
            return null;
        }

        String local = kenyanLocalDigits(cleaned);
        return local != null ? "0" + local : cleaned;
    }

    /**
     * Every spelling of the same number that may already be stored, canonical form first, so
     * existing customers are still found after normalisation was introduced.
     */
    public static List<String> variants(String raw) {
        String cleaned = clean(raw);
        if (cleaned == null) {
            return List.of();
        }

        Set<String> variants = new LinkedHashSet<>();
        String local = kenyanLocalDigits(cleaned);

        if (local != null) {
            variants.add("0" + local);
            variants.add(local);
            variants.add("254" + local);
            variants.add("+254" + local);
        } else {
            variants.add(cleaned);
        }
        variants.add(raw.trim());
        return List.copyOf(variants);
    }

    /** Null for blank values and the placeholder the old Shopify import used. */
    private static String clean(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty() || "UNKNOWN".equalsIgnoreCase(trimmed)) {
            return null;
        }

        String compact = trimmed.replaceAll("[\\s\\-().]", "");
        return compact.isEmpty() ? null : compact;
    }

    /** The 9 digits after the leading 0 / 254 for a Kenyan mobile, or null if it is not one. */
    private static String kenyanLocalDigits(String cleaned) {
        String digits = cleaned.startsWith("+") ? cleaned.substring(1) : cleaned;
        if (!digits.matches("\\d+")) {
            return null;
        }

        String nine;
        if (digits.startsWith("254") && digits.length() == 12) {
            nine = digits.substring(3);
        } else if (digits.startsWith("0") && digits.length() == 10) {
            nine = digits.substring(1);
        } else if (digits.length() == 9) {
            nine = digits;
        } else {
            return null;
        }

        // Kenyan mobile numbers start with 7 or 1 (Safaricom, Airtel, Telkom, ...).
        return nine.startsWith("7") || nine.startsWith("1") ? nine : null;
    }
}
