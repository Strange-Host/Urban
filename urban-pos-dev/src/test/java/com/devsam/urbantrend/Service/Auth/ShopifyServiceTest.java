package com.devsam.urbantrend.Service.Auth;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ShopifyServiceTest {

    @Test
    void lineDescriptionKeepsQuantityVariantAndSku() {
        Map<String, String> options = ShopifyService.describeLine(Map.of(
                "quantity", 3,
                "variant_title", " Large / Red ",
                "sku", "TS-1"));

        assertThat(options)
                .containsEntry("Quantity", "3")
                .containsEntry("Variant", "Large / Red")
                .containsEntry("SKU", "TS-1");
    }

    @Test
    void lineDescriptionSkipsMissingParts() {
        assertThat(ShopifyService.describeLine(Map.of("quantity", 1))).containsOnlyKeys("Quantity");
        assertThat(ShopifyService.describeLine(Map.of("title", "Shirt"))).isEmpty();
    }
}
