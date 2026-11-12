package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.ProfitReportResponse;
import com.devsam.urbantrend.Entity.Order;
import com.devsam.urbantrend.Entity.OrderItem;
import com.devsam.urbantrend.Repository.ExpenseRepository;
import com.devsam.urbantrend.Repository.OrderItemRepository;
import com.devsam.urbantrend.Repository.OrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportServiceImpl implements ReportService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ExpenseRepository expenseRepository;

    @Override
    public ProfitReportResponse getProfitReport(LocalDateTime from, LocalDateTime to) {

        List<Order> orders = orderRepository.findAllByCreatedAtBetweenAndDeletedAtIsNull(from, to);

        BigDecimal revenue = BigDecimal.ZERO;
        BigDecimal cogs = BigDecimal.ZERO;

        for (Order order : orders) {

            if (order.getOrderStatus() == null || !order.getOrderStatus().isRevenueRecognized()) continue;

            List<OrderItem> items = orderItemRepository.findByOrder_Id(order.getId());

            for (OrderItem item : items) {
                revenue = revenue.add(item.getLineTotal());
                cogs = cogs.add(item.getCostPrice().multiply(item.getQuantity()));
            }
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
}
