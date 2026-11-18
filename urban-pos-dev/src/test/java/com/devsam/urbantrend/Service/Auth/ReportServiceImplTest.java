package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.PayrollTotalsResponse;
import com.devsam.urbantrend.Dtos.ProfitReportResponse;
import com.devsam.urbantrend.Repository.ExpenseRepository;
import com.devsam.urbantrend.Repository.OrderItemRepository;
import com.devsam.urbantrend.Repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PayrollService payrollService;

    private ReportServiceImpl reportService;

    private final LocalDateTime from = LocalDateTime.of(2025, 1, 1, 0, 0);
    private final LocalDateTime to = LocalDateTime.of(2025, 1, 31, 23, 59, 59);

    @BeforeEach
    void setUp() {
        // Most tests are about revenue and expenses, so staff pay defaults to nothing.
        lenient().when(payrollService.getPayrollTotals(any(), any())).thenReturn(pay("0", "0", "0"));
        reportService = new ReportServiceImpl(orderItemRepository, expenseRepository, orderRepository, payrollService);
    }

    @Test
    void netProfitSubtractsCogsAndExpensesFromRevenue() {
        when(orderItemRepository.sumRevenueAndCogs(any(), any(), anyCollection()))
                .thenReturn(List.<Object[]>of(new Object[]{new BigDecimal("1000.00"), new BigDecimal("400.00")}));
        when(expenseRepository.sumExpensesBetween(from, to)).thenReturn(new BigDecimal("200.00"));

        ProfitReportResponse report = reportService.getProfitReport(from, to);

        assertThat(report.getRevenue()).isEqualByComparingTo("1000");
        assertThat(report.getCogs()).isEqualByComparingTo("400");
        assertThat(report.getGrossProfit()).isEqualByComparingTo("600");
        assertThat(report.getExpenses()).isEqualByComparingTo("200");
        assertThat(report.getNetProfit()).isEqualByComparingTo("400");
    }

    @Test
    void discountsReduceRevenueAndProfit() {
        when(orderItemRepository.sumRevenueAndCogs(any(), any(), anyCollection()))
                .thenReturn(List.<Object[]>of(new Object[]{new BigDecimal("1000.00"), new BigDecimal("400.00")}));
        when(orderRepository.sumDiscounts(any(), any(), anyCollection())).thenReturn(new BigDecimal("150.00"));
        when(expenseRepository.sumExpensesBetween(from, to)).thenReturn(new BigDecimal("200.00"));

        ProfitReportResponse report = reportService.getProfitReport(from, to);

        assertThat(report.getRevenue()).isEqualByComparingTo("850");
        assertThat(report.getGrossProfit()).isEqualByComparingTo("450");
        assertThat(report.getNetProfit()).isEqualByComparingTo("250");
    }

    @Test
    void missingTotalsProduceZeroReport() {
        when(orderItemRepository.sumRevenueAndCogs(any(), any(), anyCollection())).thenReturn(List.of());
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
                .thenReturn(List.<Object[]>of(new Object[]{1500L, 500.0d}));
        when(expenseRepository.sumExpensesBetween(from, to)).thenReturn(new BigDecimal("250.5"));

        ProfitReportResponse report = reportService.getProfitReport(from, to);

        assertThat(report.getRevenue()).isEqualByComparingTo("1500");
        assertThat(report.getCogs()).isEqualByComparingTo("500");
        assertThat(report.getNetProfit()).isEqualByComparingTo("749.5");
    }

    private static PayrollTotalsResponse pay(String commission, String dailyPay, String advances) {
        BigDecimal c = new BigDecimal(commission);
        BigDecimal d = new BigDecimal(dailyPay);
        return PayrollTotalsResponse.builder()
                .commission(c)
                .dailyPay(d)
                .staffPay(c.add(d))
                .salaryAdvances(new BigDecimal(advances))
                .build();
    }

    @Test
    void staffPayIsDeductedToGiveNetProfitAfterPay() {
        when(orderItemRepository.sumRevenueAndCogs(any(), any(), anyCollection()))
                .thenReturn(List.<Object[]>of(new Object[]{new BigDecimal("1000.00"), new BigDecimal("400.00")}));
        when(expenseRepository.sumExpensesBetween(from, to)).thenReturn(new BigDecimal("200.00"));
        when(payrollService.getPayrollTotals(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31)))
                .thenReturn(pay("90", "210", "100"));

        ProfitReportResponse report = reportService.getProfitReport(from, to);

        assertThat(report.getNetProfit()).isEqualByComparingTo("400");      // before staff pay
        assertThat(report.getStaffCommission()).isEqualByComparingTo("90");
        assertThat(report.getStaffDailyPay()).isEqualByComparingTo("210");
        assertThat(report.getStaffPay()).isEqualByComparingTo("300");
        assertThat(report.getNetAfterPay()).isEqualByComparingTo("100");    // 400 - 300
    }

    @Test
    void advancesAreShownButNotDeductedASecondTime() {
        when(orderItemRepository.sumRevenueAndCogs(any(), any(), anyCollection()))
                .thenReturn(List.<Object[]>of(new Object[]{new BigDecimal("1000.00"), new BigDecimal("400.00")}));
        when(expenseRepository.sumExpensesBetween(from, to)).thenReturn(new BigDecimal("200.00"));
        when(payrollService.getPayrollTotals(any(), any())).thenReturn(pay("90", "210", "100"));

        ProfitReportResponse report = reportService.getProfitReport(from, to);

        assertThat(report.getSalaryAdvances()).isEqualByComparingTo("100");
        assertThat(report.getStaffPayOutstanding()).isEqualByComparingTo("200"); // 300 - 100
        assertThat(report.getNetAfterPay()).isEqualByComparingTo("100");         // advances not subtracted again
    }

    @Test
    void thePayrollWindowIsTheCalendarDaysOfTheReportRange() {
        when(orderItemRepository.sumRevenueAndCogs(any(), any(), anyCollection())).thenReturn(List.of());
        when(expenseRepository.sumExpensesBetween(any(), any())).thenReturn(BigDecimal.ZERO);

        // The dashboard sends the end of the last day, which must still count as that day.
        reportService.getProfitReport(
                LocalDateTime.of(2025, 1, 1, 0, 0), LocalDateTime.of(2025, 1, 31, 23, 59, 59, 999_999_000));

        verify(payrollService).getPayrollTotals(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31));
    }
}
