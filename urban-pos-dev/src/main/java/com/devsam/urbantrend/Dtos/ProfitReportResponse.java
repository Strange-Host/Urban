package com.devsam.urbantrend.Dtos;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ProfitReportResponse {
    private BigDecimal revenue;
    private BigDecimal cogs;
    private BigDecimal grossProfit;
    private BigDecimal expenses;
    private  BigDecimal netProfit;
}
