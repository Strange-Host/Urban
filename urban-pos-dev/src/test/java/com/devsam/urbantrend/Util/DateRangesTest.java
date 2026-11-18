package com.devsam.urbantrend.Util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DateRangesTest {

    private final LocalDate day = LocalDate.of(2026, 11, 18);

    @Test
    void theLastSecondOfTheDayIsInsideTheRange() {
        LocalDateTime lastSecond = LocalDateTime.of(2026, 11, 18, 23, 59, 59, 500_000_000);

        assertThat(lastSecond).isBefore(DateRanges.endOfDay(day));
    }

    @Test
    void midnightOfTheNextDayIsOutsideTheRange() {
        assertThat(LocalDateTime.of(2026, 11, 19, 0, 0)).isAfter(DateRanges.endOfDay(day));
    }

    @Test
    void theRangeStartsAtMidnight() {
        assertThat(DateRanges.startOfDay(day)).isEqualTo(LocalDateTime.of(2026, 11, 18, 0, 0));
    }
}
