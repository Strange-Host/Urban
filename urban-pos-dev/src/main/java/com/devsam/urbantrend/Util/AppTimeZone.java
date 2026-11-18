package com.devsam.urbantrend.Util;

import java.time.ZoneId;
import java.util.TimeZone;

/**
 * The shop's business time zone. The JVM used to run in the host's zone (UTC on the cloud
 * host), so "today", daily-pay days, weekly payroll windows and CRM follow-up reminders were
 * all three hours behind Nairobi. The zone is applied once at start-up and can be overridden
 * with the {@code APP_TIMEZONE} environment variable.
 */
public final class AppTimeZone {

    public static final String DEFAULT_ID = "Africa/Nairobi";

    private AppTimeZone() {
    }

    /** The configured zone id, falling back to Nairobi when unset, blank or invalid. */
    public static ZoneId zone() {
        return resolve(System.getenv("APP_TIMEZONE"));
    }

    static ZoneId resolve(String configured) {
        if (configured != null && !configured.isBlank()) {
            try {
                return ZoneId.of(configured.trim());
            } catch (RuntimeException ignored) {
                // fall through to the default
            }
        }
        return ZoneId.of(DEFAULT_ID);
    }

    /** Makes the JVM default zone the business zone. Must run before Spring starts. */
    public static void apply() {
        TimeZone.setDefault(TimeZone.getTimeZone(zone()));
    }
}
