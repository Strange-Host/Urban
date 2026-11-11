package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.InventoryRequest;
import com.devsam.urbantrend.Dtos.StickerBatch;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Enum.StickerLayout;

import java.util.UUID;

public interface StickerService {

    // Restocks inventory and prints one sticker per unit added
    StickerBatch restockWithStickers(InventoryRequest request, StickerLayout layout, User user);

    // Prints stickers for stock already on the shelf (inventory quantity is not changed)
    StickerBatch createStickers(UUID productId, int quantity, StickerLayout layout);

    // Reprints an existing batch
    StickerBatch reprintBatch(UUID batchId, StickerLayout layout);
}
