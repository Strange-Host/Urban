package com.devsam.urbantrend.Controller;

import com.devsam.urbantrend.Dtos.InventoryRequest;
import com.devsam.urbantrend.Dtos.StickerBatch;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Enum.StickerLayout;
import com.devsam.urbantrend.Repository.UserRepository;
import com.devsam.urbantrend.Service.Auth.StickerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stickers")
@RequiredArgsConstructor
public class StickerController {

    private final StickerService stickerService;
    private final UserRepository userRepository;

    // Restock N units and download N barcode stickers as a PDF
    @PostMapping("/restock")
    @PreAuthorize("hasAuthority('CAN_RESTOCK_INVENTORY')")
    public ResponseEntity<byte[]> restockWithStickers(
            @RequestBody InventoryRequest request,
            @RequestParam(defaultValue = "A4") StickerLayout layout,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return pdf(stickerService.restockWithStickers(request, layout, user));
    }

    // Print stickers for stock already on the shelf (does not change inventory)
    @PostMapping("/products/{productId}")
    @PreAuthorize("hasAuthority('CAN_RESTOCK_INVENTORY')")
    public ResponseEntity<byte[]> createStickers(
            @PathVariable UUID productId,
            @RequestParam int quantity,
            @RequestParam(defaultValue = "A4") StickerLayout layout
    ) {
        return pdf(stickerService.createStickers(productId, quantity, layout));
    }

    @GetMapping("/batches/{batchId}")
    @PreAuthorize("hasAuthority('CAN_RESTOCK_INVENTORY')")
    public ResponseEntity<byte[]> reprintBatch(
            @PathVariable UUID batchId,
            @RequestParam(defaultValue = "A4") StickerLayout layout
    ) {
        return pdf(stickerService.reprintBatch(batchId, layout));
    }

    private ResponseEntity<byte[]> pdf(StickerBatch batch) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=stickers-" + batch.batchId() + ".pdf")
                .header("X-Sticker-Batch-Id", batch.batchId().toString())
                .body(batch.pdf());
    }
}
