package com.devsam.urbantrend.Dtos;

import lombok.*;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class StaffOrderSummary {

    private int ordersToday;
    private BigDecimal revenueToday;
    private BigDecimal profitToday;
    private BigDecimal commissionToday;
    private int totalOrders;
    private BigDecimal totalRevenue;
    private BigDecimal totalProfit;
    private BigDecimal totalCommission;
    private BigDecimal averageOrderValue;
}
