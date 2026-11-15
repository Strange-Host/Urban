package com.devsam.urbantrend.Util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PhoneNumbersTest {

    @Test
    void everyKenyanSpellingNormalisesToTheSameNumber() {
        for (String raw : new String[]{
                "0712345678", "0712 345 678", "0712-345-678", "+254712345678",
                "254712345678", "+254 712 345 678", "712345678", " (0712) 345678 "}) {
            assertThat(PhoneNumbers.normalize(raw)).as(raw).isEqualTo("0712345678");
        }
    }

    @Test
    void oldAirtelAndTelkomPrefixesStayRecognised() {
        assertThat(PhoneNumbers.normalize("+254 110 123 456")).isEqualTo("0110123456");
    }

    @Test
    void blankAndPlaceholderValuesAreNoPhoneAtAll() {
        assertThat(PhoneNumbers.normalize(null)).isNull();
        assertThat(PhoneNumbers.normalize("   ")).isNull();
        assertThat(PhoneNumbers.normalize("UNKNOWN")).isNull();
        assertThat(PhoneNumbers.variants("UNKNOWN")).isEmpty();
    }

    @Test
    void numbersThatAreNotKenyanMobilesAreOnlyCleaned() {
        assertThat(PhoneNumbers.normalize("+1 (415) 555-0100")).isEqualTo("+14155550100");
        assertThat(PhoneNumbers.normalize("020 123 4567")).isEqualTo("0201234567");
    }

    @Test
    void variantsCoverEveryFormThatMayAlreadyBeStored() {
        assertThat(PhoneNumbers.variants("+254 712 345 678"))
                .contains("0712345678", "712345678", "254712345678", "+254712345678", "+254 712 345 678")
                .first().isEqualTo("0712345678");
    }
}
