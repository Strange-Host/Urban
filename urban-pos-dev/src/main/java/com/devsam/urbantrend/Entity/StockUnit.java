package com.devsam.urbantrend.Entity;

import com.devsam.urbantrend.Enum.StockUnitStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "stock_units")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockUnit extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Value encoded in the sticker barcode, e.g. UT00000123
    @Column(nullable = false, unique = true, length = 32)
    private String serial;

    // All stickers printed together share a batch id (used for reprinting)
    @Column(name = "batch_id", nullable = false)
    private UUID batchId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StockUnitStatus status;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "scanned_at")
    private LocalDateTime scannedAt;

    @Column(name = "scanned_by")
    private UUID scannedBy;
}
