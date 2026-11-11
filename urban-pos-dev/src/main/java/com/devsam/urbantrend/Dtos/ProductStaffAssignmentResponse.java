package com.devsam.urbantrend.Dtos;

import lombok.*;
import java.util.UUID;

@Data
@Builder
@Getter
@Setter
public class ProductStaffAssignmentResponse {
    private UUID productId;
    private String productName;
    private UUID staffId;
    private String staffName;
}