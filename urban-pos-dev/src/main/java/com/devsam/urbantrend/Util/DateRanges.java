package com.devsam.urbantrend.Util;

import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

/**
 * Inclusive day boundaries for {@code BETWEEN} queries. Ending a day at 23:59:59 dropped every
 * record created in the last second of that day; ending it one microsecond before the next
 * midnight (PostgreSQL's timestamp precision) keeps the whole day without spilling into the next.
 */
public final class DateRanges {

    private DateRanges() {
    }

    public static LocalDateTime startOfDay(LocalDate date) {
        return date.atStartOfDay();
    }

    public static LocalDateTime endOfDay(LocalDate date) {
        return date.plusDays(1).atStartOfDay().minusNanos(1_000);
    }

    /** The Monday of the week (Monday to Sunday) that contains the date. */
    public static LocalDate startOfWeek(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}
