package com.devsam.urbantrend.Dtos;

import com.devsam.urbantrend.Enum.RequisitionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class  RequisitionRequest {

    @NotNull
    private RequisitionType requisitionType;

    @NotBlank
    private String title;

    private String description;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amountRequested;
}