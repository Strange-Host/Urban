package com.devsam.urbantrend.Dtos;

import java.util.UUID;

public record StickerBatch(UUID batchId, int count, byte[] pdf) {
}
