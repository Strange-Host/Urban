package com.devsam.urbantrend.Util;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

class AppTimeZoneTest {

    @Test
    void defaultsToNairobiWhenNothingIsConfigured() {
        assertThat(AppTimeZone.resolve(null)).isEqualTo(ZoneId.of("Africa/Nairobi"));
        assertThat(AppTimeZone.resolve("")).isEqualTo(ZoneId.of("Africa/Nairobi"));
        assertThat(AppTimeZone.resolve("   ")).isEqualTo(ZoneId.of("Africa/Nairobi"));
    }

    @Test
    void anInvalidZoneFallsBackToNairobiInsteadOfFailingStartUp() {
        assertThat(AppTimeZone.resolve("Not/AZone")).isEqualTo(ZoneId.of("Africa/Nairobi"));
    }

    @Test
    void aValidOverrideIsHonoured() {
        assertThat(AppTimeZone.resolve(" Europe/London ")).isEqualTo(ZoneId.of("Europe/London"));
    }

    @Test
    void nairobiIsThreeHoursAheadOfUtcAllYear() {
        ZoneId nairobi = ZoneId.of(AppTimeZone.DEFAULT_ID);

        assertThat(nairobi.getRules().getOffset(Instant.parse("2026-01-15T00:00:00Z")))
                .isEqualTo(ZoneOffset.ofHours(3));
        assertThat(nairobi.getRules().getOffset(Instant.parse("2026-07-15T00:00:00Z")))
                .isEqualTo(ZoneOffset.ofHours(3));
    }

    @Test
    void applyChangesTheJvmDefaultZone() {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

            AppTimeZone.apply();

            // APP_TIMEZONE is unset in CI, so this is the Nairobi default.
            assertThat(TimeZone.getDefault().toZoneId()).isEqualTo(AppTimeZone.zone());
        } finally {
            TimeZone.setDefault(original);
        }
    }
}
