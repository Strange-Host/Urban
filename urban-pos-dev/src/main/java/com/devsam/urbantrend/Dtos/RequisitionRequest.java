package com.devsam.urbantrend.Dtos;

import com.devsam.urbantrend.Enum.RequisitionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class  RequisitionRequest {
    private RequisitionType requisitionType;
    private String title;
    private String description;
    private BigDecimal amountRequested;
}
