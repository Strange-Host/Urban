package com.devsam.urbantrend.Dtos;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {

    private UUID id;

    private UUID productId;
    private String productName;

    private BigDecimal quantity;
    private BigDecimal sellingPrice;
}
