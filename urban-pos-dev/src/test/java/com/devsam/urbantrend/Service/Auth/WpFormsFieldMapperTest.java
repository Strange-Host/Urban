package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Entity.Lead;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WpFormsFieldMapperTest {

    private static Map<String, Object> fields(Object... keyValues) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    @Test
    void deliveryCountyIsUsedWhenThereIsNoLocationEvenWithAdditionalInfo() {
        Lead lead = new Lead();

        WpFormsFieldMapper.apply(lead, fields(
                "Delivery County", "Kiambu",
                WpFormsFieldMapper.ADDITIONAL_INFO, "Gate 4"));

        // Used to be skipped entirely whenever the additional-information field was present.
        assertThat(lead.getLocation()).isEqualTo("Kiambu");
        assertThat(lead.getAdditionalInfo()).isEqualTo("Gate 4");
    }

    @Test
    void deliveryCountyNeverReplacesARealLocation() {
        Lead lead = new Lead();

        WpFormsFieldMapper.apply(lead, fields("Location", "Westlands", "Delivery County", "Nairobi"));

        assertThat(lead.getLocation()).isEqualTo("Westlands");
    }

    @Test
    void firstAndLastNamesAreJoinedWithoutPrintingNull() {
        Lead onlyFirst = new Lead();
        WpFormsFieldMapper.apply(onlyFirst, fields("First", "Amina"));
        assertThat(onlyFirst.getName()).isEqualTo("Amina");

        Lead both = new Lead();
        WpFormsFieldMapper.apply(both, fields("First", "Amina", "Last", "Njeri"));
        assertThat(both.getName()).isEqualTo("Amina Njeri");
    }

    @Test
    void aSingleNameFieldWinsAndNullValuesDoNotCrash() {
        Lead lead = new Lead();
        Map<String, Object> withNull = fields("Name", "Amina Njeri", "Phone Number", null);

        WpFormsFieldMapper.apply(lead, withNull);

        assertThat(lead.getName()).isEqualTo("Amina Njeri");
        assertThat(lead.getPhoneNumber()).isNull();
    }

    @Test
    void phonesAreTrimmed() {
        Lead lead = new Lead();

        WpFormsFieldMapper.apply(lead, fields("Phone Number", " 0712345678 ", "Alternative Phone Number", " "));

        assertThat(lead.getPhoneNumber()).isEqualTo("0712345678");
        assertThat(lead.getAlternativePhoneNumber()).isNull();
    }
}
