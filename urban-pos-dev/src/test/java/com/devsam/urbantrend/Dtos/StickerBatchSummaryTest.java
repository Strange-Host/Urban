package com.devsam.urbantrend.Dtos;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StickerBatchSummaryTest {

    @Test
    void mapsARepositoryRowIncludingTheCountAsALong() {
        UUID batch = UUID.randomUUID();
        UUID product = UUID.randomUUID();
        LocalDateTime printed = LocalDateTime.of(2026, 11, 14, 9, 30);

        StickerBatchSummary summary =
                StickerBatchSummary.from(new Object[]{batch, product, "Leather Wallet", 24L, printed});

        assertThat(summary.batchId()).isEqualTo(batch);
        assertThat(summary.productId()).isEqualTo(product);
        assertThat(summary.productName()).isEqualTo("Leather Wallet");
        assertThat(summary.units()).isEqualTo(24);
        assertThat(summary.createdAt()).isEqualTo(printed);
    }

    @Test
    void acceptsANumberTypeOtherThanLongForTheCount() {
        StickerBatchSummary summary = StickerBatchSummary.from(
                new Object[]{UUID.randomUUID(), UUID.randomUUID(), "Shirt", 5, LocalDateTime.now()});

        assertThat(summary.units()).isEqualTo(5);
    }
}
