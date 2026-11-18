package com.devsam.urbantrend.Dtos;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** Payroll for every non-admin staff member added together, for the profit report. */
@Data
@Builder
public class PayrollTotalsResponse {

    private BigDecimal commission;
    private BigDecimal dailyPay;

    /** commission + dailyPay: the cost of staff for the period. */
    private BigDecimal staffPay;

    /** Advances already paid out in the period (a prepayment of staff pay, not extra cost). */
    private BigDecimal salaryAdvances;
}
