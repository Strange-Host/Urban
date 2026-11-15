package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Entity.Lead;

import java.util.Map;

/** Maps the fields of a WPForms submission onto a lead. */
public final class WpFormsFieldMapper {

    static final String ADDITIONAL_INFO =
            "Additional information or any Enquiry. (e.g. exact address, number of pieces).";

    private WpFormsFieldMapper() {
    }

    public static void apply(Lead lead, Map<String, Object> fields) {
        if (fields == null) {
            return;
        }

        String name = text(fields.get("Name"));
        if (name == null) {
            String first = text(fields.get("First"));
            String last = text(fields.get("Last"));
            if (first != null || last != null) {
                name = ((first == null ? "" : first) + " " + (last == null ? "" : last)).trim();
            }
        }
        if (name != null) {
            lead.setName(name);
        }

        String phone = text(fields.get("Phone Number"));
        if (phone != null) {
            lead.setPhoneNumber(phone);
        }

        String alternative = text(fields.get("Alternative Phone Number"));
        if (alternative != null) {
            lead.setAlternativePhoneNumber(alternative);
        }

        // "Location" wins. Some forms only have "Delivery County", which is a fallback and
        // must never replace a real Location (it used to, and was skipped entirely whenever
        // the additional-information field was present).
        String location = text(fields.get("Location"));
        if (location == null) {
            location = text(fields.get("Delivery County"));
        }
        if (location != null) {
            lead.setLocation(location);
        }

        String additionalInfo = text(fields.get(ADDITIONAL_INFO));
        if (additionalInfo != null) {
            lead.setAdditionalInfo(additionalInfo);
        }
    }

    private static String text(Object value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.toString().trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
