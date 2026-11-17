package com.devsam.urbantrend.Dtos;

import java.time.LocalDateTime;
import java.util.UUID;

/** One printed run of stickers, so a reprint can be chosen from a list instead of typed. */
public record StickerBatchSummary(UUID batchId, UUID productId, String productName, long units, LocalDateTime createdAt) {

    /** Maps a row of: batchId, productId, productName, count, firstCreatedAt. */
    public static StickerBatchSummary from(Object[] row) {
        return new StickerBatchSummary(
                (UUID) row[0],
                (UUID) row[1],
                (String) row[2],
                ((Number) row[3]).longValue(),
                (LocalDateTime) row[4]
        );
    }
}
