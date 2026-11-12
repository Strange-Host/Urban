package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.ProfitReportResponse;
import com.devsam.urbantrend.Repository.ExpenseRepository;
import com.devsam.urbantrend.Repository.OrderItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    private ReportServiceImpl reportService;

    private final LocalDateTime from = LocalDateTime.of(2025, 1, 1, 0, 0);
    private final LocalDateTime to = LocalDateTime.of(2025, 1, 31, 23, 59, 59);

    @BeforeEach
    void setUp() {
        reportService = new ReportServiceImpl(orderItemRepository, expenseRepository);
    }

    @Test
    void netProfitSubtractsCogsAndExpensesFromRevenue() {
        when(orderItemRepository.sumRevenueAndCogs(any(), any(), anyCollection()))
                .thenReturn(new Object[]{new BigDecimal("1000.00"), new BigDecimal("400.00")});
        when(expenseRepository.sumExpensesBetween(from, to)).thenReturn(new BigDecimal("200.00"));

        ProfitReportResponse report = reportService.getProfitReport(from, to);

        assertThat(report.getRevenue()).isEqualByComparingTo("1000");
        assertThat(report.getCogs()).isEqualByComparingTo("400");
        assertThat(report.getGrossProfit()).isEqualByComparingTo("600");
        assertThat(report.getExpenses()).isEqualByComparingTo("200");
        assertThat(report.getNetProfit()).isEqualByComparingTo("400");
    }

    @Test
    void missingTotalsProduceZeroReport() {
        when(orderItemRepository.sumRevenueAndCogs(any(), any(), anyCollection())).thenReturn(null);
        when(expenseRepository.sumExpensesBetween(from, to)).thenReturn(BigDecimal.ZERO);

        ProfitReportResponse report = reportService.getProfitReport(from, to);

        assertThat(report.getRevenue()).isEqualByComparingTo("0");
        assertThat(report.getCogs()).isEqualByComparingTo("0");
        assertThat(report.getGrossProfit()).isEqualByComparingTo("0");
        assertThat(report.getExpenses()).isEqualByComparingTo("0");
        assertThat(report.getNetProfit()).isEqualByComparingTo("0");
    }

    @Test
    void numericAggregatesFromTheDatabaseAreConverted() {
        when(orderItemRepository.sumRevenueAndCogs(any(), any(), anyCollection()))
                .thenReturn(new Object[]{1500L, 500.0d});
        when(expenseRepository.sumExpensesBetween(from, to)).thenReturn(new BigDecimal("250.5"));

        ProfitReportResponse report = reportService.getProfitReport(from, to);

        assertThat(report.getRevenue()).isEqualByComparingTo("1500");
        assertThat(report.getCogs()).isEqualByComparingTo("500");
        assertThat(report.getNetProfit()).isEqualByComparingTo("749.5");
    }
}
