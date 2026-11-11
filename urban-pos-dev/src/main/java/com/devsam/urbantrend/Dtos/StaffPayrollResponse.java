package com.devsam.urbantrend.Dtos;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class StaffPayrollResponse {

    private UUID staffId;
    private String staffName;

    private long paidOrders;
    private BigDecimal commission;

    private long daysWorked;
    private BigDecimal dailySalary;

    private BigDecimal totalPayable;
}
