package com.devsam.urbantrend.Service.Auth.Messaging;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class ZettatelSmsProviderTest {

    private final ZettatelSmsProvider provider = new ZettatelSmsProvider();

    @Test
    void missingPhoneNumberIsSkippedInsteadOfThrowing() {
        // A customer without a phone used to cause a NullPointerException that rolled
        // back the surrounding payment transaction.
        assertThatCode(() -> provider.sendSms(null, "hello")).doesNotThrowAnyException();
    }

    @Test
    void unrecognisedPhoneNumbersAreSkipped() {
        assertThatCode(() -> provider.sendSms("UNKNOWN", "hello")).doesNotThrowAnyException();
        assertThatCode(() -> provider.sendSms("   ", "hello")).doesNotThrowAnyException();
    }
}
