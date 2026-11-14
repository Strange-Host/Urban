package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.ProfitReportResponse;
import com.devsam.urbantrend.Enum.OrderStatus;
import com.devsam.urbantrend.Repository.ExpenseRepository;
import com.devsam.urbantrend.Repository.OrderItemRepository;
import com.devsam.urbantrend.Repository.OrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportServiceImpl implements ReportService {

    private static final List<OrderStatus> REVENUE_STATUSES =
            Arrays.stream(OrderStatus.values())
                    .filter(OrderStatus::isRevenueRecognized)
                    .toList();

    private final OrderItemRepository orderItemRepository;
    private final ExpenseRepository expenseRepository;
    private final OrderRepository orderRepository;

    @Override
    public ProfitReportResponse getProfitReport(LocalDateTime from, LocalDateTime to) {

        List<Object[]> rows = orderItemRepository.sumRevenueAndCogs(from, to, REVENUE_STATUSES);

        Object[] totals = rows == null || rows.isEmpty() ? null : rows.get(0);

        BigDecimal revenue = totals != null && totals.length > 0 ? toBigDecimal(totals[0]) : BigDecimal.ZERO;
        BigDecimal cogs = totals != null && totals.length > 1 ? toBigDecimal(totals[1]) : BigDecimal.ZERO;

        // Revenue is what customers were actually charged for the goods, i.e. after any
        // order-level discount.
        BigDecimal discounts = orderRepository.sumDiscounts(from, to, REVENUE_STATUSES);
        if (discounts != null) {
            revenue = revenue.subtract(discounts);
        }

        BigDecimal grossProfit = revenue.subtract(cogs);

        BigDecimal expenses = expenseRepository.sumExpensesBetween(from, to);

        BigDecimal netProfit = grossProfit.subtract(expenses);

        return ProfitReportResponse.builder()
                .revenue(revenue)
                .cogs(cogs)
                .grossProfit(grossProfit)
                .expenses(expenses)
                .netProfit(netProfit)
                .build();
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        return new BigDecimal(value.toString());
    }
}
