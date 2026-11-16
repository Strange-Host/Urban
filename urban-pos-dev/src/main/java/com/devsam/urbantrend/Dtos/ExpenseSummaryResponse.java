package com.devsam.urbantrend.Dtos;

import java.math.BigDecimal;

/** Totals over every expense, not just the page being shown. */
public record ExpenseSummaryResponse(BigDecimal total, long count) {
}
