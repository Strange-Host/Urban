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

    /** Everything earned in the period: commission plus daily pay, before advances. */
    private BigDecimal totalPayable;

    /** Salary advances paid out to this person during the period. */
    private BigDecimal salaryAdvances;

    /**
     * What is still owed: {@code totalPayable - salaryAdvances}. Negative when the person was
     * advanced more than they earned in the period (the difference is carried to the next one).
     */
    private BigDecimal netPayable;
}
