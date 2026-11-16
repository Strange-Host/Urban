package com.devsam.urbantrend.Security;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Decides whether an API call is a change worth putting in the staff activity log and, if so,
 * what to call it. Reads are never logged; neither are sign-in, token and webhook traffic.
 */
public final class ActivityLogClassifier {

    public record Entry(String action, String entity, String entityId, String description) {
    }

    private static final String PREFIX = "/api/v1/";
    private static final Pattern UUID_SEGMENT =
            Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    private static final Map<String, String> ENTITIES = Map.ofEntries(
            Map.entry("orders", "ORDER"),
            Map.entry("payments", "PAYMENT"),
            Map.entry("products", "PRODUCT"),
            Map.entry("category", "CATEGORY"),
            Map.entry("inventory", "INVENTORY"),
            Map.entry("customers", "CUSTOMER"),
            Map.entry("couriers", "COURIER"),
            Map.entry("requisitions", "REQUISITION"),
            Map.entry("expenses", "EXPENSE"),
            Map.entry("stickers", "STICKER"),
            Map.entry("settings", "SETTINGS")
    );

    private ActivityLogClassifier() {
    }

    /** @return the log entry, or null when this request should not be logged */
    public static Entry classify(String method, String path, String query) {
        if (method == null || path == null || !path.startsWith(PREFIX)) {
            return null;
        }

        String action = switch (method.toUpperCase(Locale.ROOT)) {
            case "POST" -> "POST";
            case "PUT", "PATCH" -> "UPDATE";
            case "DELETE" -> "DELETE";
            default -> null; // GET, HEAD, OPTIONS: nothing changed
        };
        if (action == null) {
            return null;
        }

        String[] segments = path.substring(PREFIX.length()).split("/");
        String first = segments[0];

        String entity;
        if ("admin".equals(first) && segments.length > 1 && "permissions".equals(segments[1])) {
            entity = "PERMISSIONS";
        } else if ("auth".equals(first)) {
            // Only staff account changes; sign-in, OTP, refresh and logout are not activity.
            if (segments.length > 1 && ("register".equals(segments[1]) || "user".equals(segments[1]))) {
                entity = "USER";
            } else {
                return null;
            }
        } else if ("orders".equals(first) && segments.length > 1 && "scan".equals(segments[1])) {
            // Packing scans are frequent; keep them apart from real order edits.
            entity = "DISPATCH";
        } else {
            entity = ENTITIES.get(first);
            if (entity == null) {
                return null; // leads log themselves; webhooks, notifications, reports: skip
            }
        }

        String entityId = null;
        for (String segment : segments) {
            if (UUID_SEGMENT.matcher(segment).matches()) {
                entityId = segment;
                break;
            }
        }

        // POST to a collection creates; POST to an action path (restock, scan, match,
        // approve...) changes something that already exists.
        if ("POST".equals(action)) {
            String last = segments[segments.length - 1];
            boolean collectionRoot = segments.length == 1
                    || ("auth".equals(first) && "register".equals(last))
                    || ("admin".equals(first) && segments.length == 2);
            action = collectionRoot ? "CREATE" : "UPDATE";
        }

        String description = method.toUpperCase(Locale.ROOT) + " " + path
                + (query == null || query.isBlank() ? "" : "?" + query);

        return new Entry(action, entity, entityId, description);
    }
}
