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
    private BigDecimal netProfit;

    /** Sales commission earned by staff in the period. */
    private BigDecimal staffCommission;

    /** Daily pay earned by staff in the period. */
    private BigDecimal staffDailyPay;

    /** staffCommission + staffDailyPay. */
    private BigDecimal staffPay;

    /** Salary advances paid out in the period. Already part of staffPay, so not deducted twice. */
    private BigDecimal salaryAdvances;

    /** Staff pay still to be paid after advances: staffPay - salaryAdvances. */
    private BigDecimal staffPayOutstanding;

    /** netProfit - staffPay: what the shop keeps once staff are paid. */
    private BigDecimal netAfterPay;
}
