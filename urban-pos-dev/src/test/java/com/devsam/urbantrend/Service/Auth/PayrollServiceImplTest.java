package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.PayrollTotalsResponse;
import com.devsam.urbantrend.Dtos.StaffPayrollResponse;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Repository.LeadRepository;
import com.devsam.urbantrend.Enum.RequisitionStatus;
import com.devsam.urbantrend.Repository.OrderRepository;
import com.devsam.urbantrend.Repository.SalaryAdvanceRepository;
import com.devsam.urbantrend.Repository.UserRepository;
import com.devsam.urbantrend.Roles.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayrollServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LeadRepository leadRepository;

    @Mock
    private SalaryAdvanceRepository salaryAdvanceRepository;

    private PayrollServiceImpl payroll;

    private final UUID staffId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        payroll = new PayrollServiceImpl(orderRepository, userRepository, leadRepository, salaryAdvanceRepository);
    }

    private User staff(UUID id, String first, String last, UserRole role) {
        User user = User.builder()
                .firstName(first)
                .lastName(last)
                .email(first.toLowerCase() + "@example.com")
                .phoneNumber("0700000000")
                .password("encoded")
                .role(role)
                .build();
        user.setId(id);
        return user;
    }

    private void stubWeek(UUID id, long assigned, long converted, long paid) {
        when(leadRepository.countAssignedLeads(eq(id), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(assigned);
        when(leadRepository.countConvertedAndPaidLeads(eq(id), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(converted);
        when(orderRepository.countPaidOrders(eq(id), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(paid);
    }

    @Test
    void highKpiPaysFiftyPerPaidOrderPlusDailySalary() {
        when(userRepository.findById(staffId))
                .thenReturn(Optional.of(staff(staffId, "Jane", "Doe", UserRole.STAFF)));
        stubWeek(staffId, 10, 8, 4); // 80% KPI -> high tier
        when(orderRepository.countPaidDaysWorked(eq(staffId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(5L);

        StaffPayrollResponse result = payroll.getStaffPayroll(
                staffId, LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 12));

        assertThat(result.getStaffName()).isEqualTo("Jane Doe");
        assertThat(result.getPaidOrders()).isEqualTo(4);
        assertThat(result.getCommission()).isEqualByComparingTo("200");
        assertThat(result.getDaysWorked()).isEqualTo(5);
        assertThat(result.getDailySalary()).isEqualByComparingTo("1500");
        assertThat(result.getTotalPayable()).isEqualByComparingTo("1700");
    }

    @Test
    void belowThresholdPaysThirtyPerPaidOrder() {
        when(userRepository.findById(staffId))
                .thenReturn(Optional.of(staff(staffId, "Jane", "Doe", UserRole.STAFF)));
        stubWeek(staffId, 10, 7, 2); // 70% KPI -> low tier
        when(orderRepository.countPaidDaysWorked(eq(staffId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(1L);

        StaffPayrollResponse result = payroll.getStaffPayroll(
                staffId, LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 12));

        assertThat(result.getCommission()).isEqualByComparingTo("60");
        assertThat(result.getTotalPayable()).isEqualByComparingTo("360");
    }

    @Test
    void noAssignedLeadsFallsBackToTheLowTier() {
        when(userRepository.findById(staffId))
                .thenReturn(Optional.of(staff(staffId, "Jane", "Doe", UserRole.CASHIER)));
        stubWeek(staffId, 0, 0, 3);
        when(orderRepository.countPaidDaysWorked(eq(staffId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(0L);

        StaffPayrollResponse result = payroll.getStaffPayroll(
                staffId, LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 12));

        assertThat(result.getCommission()).isEqualByComparingTo("90");
        assertThat(result.getDailySalary()).isEqualByComparingTo("0");
        assertThat(result.getTotalPayable()).isEqualByComparingTo("90");
    }

    @Test
    void multiWeekRangeAccumulatesEachWeekSeparately() {
        when(userRepository.findById(staffId))
                .thenReturn(Optional.of(staff(staffId, "Jane", "Doe", UserRole.STAFF)));
        stubWeek(staffId, 10, 10, 1);
        when(orderRepository.countPaidDaysWorked(eq(staffId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(2L);

        StaffPayrollResponse result = payroll.getStaffPayroll(
                staffId, LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 19));

        assertThat(result.getPaidOrders()).isEqualTo(2);
        assertThat(result.getCommission()).isEqualByComparingTo("100");
        assertThat(result.getTotalPayable()).isEqualByComparingTo("700");
        verify(orderRepository, times(2))
                .countPaidOrders(eq(staffId), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void allStaffPayrollCombinesGroupedQueriesPerWeek() {
        UUID secondId = UUID.randomUUID();
        when(userRepository.findAllByDeletedAtIsNullAndRoleNot(UserRole.ADMIN))
                .thenReturn(List.of(
                        staff(staffId, "Jane", "Doe", UserRole.STAFF),
                        staff(secondId, "John", "Roe", UserRole.CASHIER)));

        when(leadRepository.countAssignedLeadsByStaff(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(new Object[]{staffId, 10L}, new Object[]{secondId, 4L}));
        when(leadRepository.countConvertedAndPaidLeadsByStaff(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(new Object[]{staffId, 8L}, new Object[]{secondId, 4L}));
        when(orderRepository.countPaidOrdersByStaff(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(new Object[]{staffId, 4L}, new Object[]{secondId, 2L}));
        when(orderRepository.countPaidDaysWorkedByStaff(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(new Object[]{staffId, 5L}, new Object[]{secondId, 1L}));

        List<StaffPayrollResponse> result = payroll.getAllStaffPayroll(
                LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 12));

        assertThat(result).hasSize(2);

        StaffPayrollResponse jane = result.get(0);
        assertThat(jane.getCommission()).isEqualByComparingTo("200");
        assertThat(jane.getTotalPayable()).isEqualByComparingTo("1700");

        StaffPayrollResponse john = result.get(1);
        assertThat(john.getCommission()).isEqualByComparingTo("100");
        assertThat(john.getDaysWorked()).isEqualTo(1);
        assertThat(john.getTotalPayable()).isEqualByComparingTo("400");
    }

    @Test
    void emptyStaffListSkipsAllMetricQueries() {
        when(userRepository.findAllByDeletedAtIsNullAndRoleNot(UserRole.ADMIN))
                .thenReturn(List.of());

        assertThat(payroll.getAllStaffPayroll(
                LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 12))).isEmpty();

        verifyNoInteractions(orderRepository, leadRepository);
        verify(userRepository, times(1)).findAllByDeletedAtIsNullAndRoleNot(UserRole.ADMIN);
    }

    // ---- fixed Monday-to-Sunday weeks (#25) ------------------------------------------------

    @Test
    void aRangeStartingMidWeekMeasuresTheKpiOverTheWholeWeek() {
        when(userRepository.findById(staffId))
                .thenReturn(Optional.of(staff(staffId, "Jane", "Doe", UserRole.STAFF)));
        stubWeek(staffId, 10, 9, 2);
        when(orderRepository.countPaidDaysWorked(eq(staffId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(1L);

        // Wednesday 8 Jan to Sunday 12 Jan, inside the week of Monday 6 Jan.
        payroll.getStaffPayroll(staffId, LocalDate.of(2025, 1, 8), LocalDate.of(2025, 1, 12));

        ArgumentCaptor<LocalDateTime> kpiFrom = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> kpiTo = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(leadRepository).countAssignedLeads(eq(staffId), kpiFrom.capture(), kpiTo.capture());
        assertThat(kpiFrom.getValue()).isEqualTo(LocalDateTime.of(2025, 1, 6, 0, 0));
        assertThat(kpiTo.getValue()).isAfter(LocalDateTime.of(2025, 1, 12, 23, 59, 59));

        // ...but only orders inside the requested range are paid commission on.
        ArgumentCaptor<LocalDateTime> paidFrom = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(orderRepository).countPaidOrders(eq(staffId), paidFrom.capture(), any(LocalDateTime.class));
        assertThat(paidFrom.getValue()).isEqualTo(LocalDateTime.of(2025, 1, 8, 0, 0));
    }

    @Test
    void theSameWeekEarnsTheSameCommissionWhateverRangeCoversIt() {
        when(userRepository.findById(staffId))
                .thenReturn(Optional.of(staff(staffId, "Jane", "Doe", UserRole.STAFF)));
        stubWeek(staffId, 10, 9, 2); // 90% -> high tier
        when(orderRepository.countPaidDaysWorked(eq(staffId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(0L);

        StaffPayrollResponse midWeek = payroll.getStaffPayroll(
                staffId, LocalDate.of(2025, 1, 8), LocalDate.of(2025, 1, 12));
        StaffPayrollResponse wholeWeek = payroll.getStaffPayroll(
                staffId, LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 12));

        assertThat(midWeek.getCommission()).isEqualByComparingTo(wholeWeek.getCommission());
    }

    @Test
    void aRangeCrossingAWeekBoundaryWalksEachCalendarWeek() {
        when(userRepository.findById(staffId))
                .thenReturn(Optional.of(staff(staffId, "Jane", "Doe", UserRole.STAFF)));
        stubWeek(staffId, 10, 10, 1);
        when(orderRepository.countPaidDaysWorked(eq(staffId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(0L);

        // Wednesday 8 Jan to Tuesday 14 Jan touches two calendar weeks; the old code saw one
        // 7-day block starting on the Wednesday.
        payroll.getStaffPayroll(staffId, LocalDate.of(2025, 1, 8), LocalDate.of(2025, 1, 14));

        ArgumentCaptor<LocalDateTime> weekStarts = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(leadRepository, times(2))
                .countAssignedLeads(eq(staffId), weekStarts.capture(), any(LocalDateTime.class));
        assertThat(weekStarts.getAllValues()).containsExactly(
                LocalDateTime.of(2025, 1, 6, 0, 0),
                LocalDateTime.of(2025, 1, 13, 0, 0));
    }

    @Test
    void aSingleDayRangeUsesThatDaysWeeklyKpiNotAOneDayKpi() {
        when(userRepository.findById(staffId))
                .thenReturn(Optional.of(staff(staffId, "Jane", "Doe", UserRole.STAFF)));
        // 9 of 10 leads converted across the week; today alone had none assigned.
        stubWeek(staffId, 10, 9, 1);
        when(orderRepository.countPaidDaysWorked(eq(staffId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(1L);

        StaffPayrollResponse today = payroll.getStaffPayroll(
                staffId, LocalDate.of(2025, 1, 8), LocalDate.of(2025, 1, 8));

        assertThat(today.getCommission()).isEqualByComparingTo("50");
    }

    // ---- salary advances (#52) -------------------------------------------------------------

    @Test
    void advancesAreSubtractedFromWhatIsStillOwed() {
        when(userRepository.findById(staffId))
                .thenReturn(Optional.of(staff(staffId, "Jane", "Doe", UserRole.STAFF)));
        stubWeek(staffId, 10, 8, 4);
        when(orderRepository.countPaidDaysWorked(eq(staffId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(5L);
        when(salaryAdvanceRepository.sumAdvancesForUser(
                eq(staffId), any(LocalDateTime.class), any(LocalDateTime.class), eq(RequisitionStatus.PAID)))
                .thenReturn(new BigDecimal("500"));

        StaffPayrollResponse result = payroll.getStaffPayroll(
                staffId, LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 12));

        assertThat(result.getTotalPayable()).isEqualByComparingTo("1700"); // earned, unchanged
        assertThat(result.getSalaryAdvances()).isEqualByComparingTo("500");
        assertThat(result.getNetPayable()).isEqualByComparingTo("1200");
    }

    @Test
    void anAdvanceLargerThanEarningsGivesANegativeNetPayable() {
        when(userRepository.findById(staffId))
                .thenReturn(Optional.of(staff(staffId, "Jane", "Doe", UserRole.STAFF)));
        stubWeek(staffId, 0, 0, 1); // 30 commission
        when(orderRepository.countPaidDaysWorked(eq(staffId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(salaryAdvanceRepository.sumAdvancesForUser(
                eq(staffId), any(LocalDateTime.class), any(LocalDateTime.class), eq(RequisitionStatus.PAID)))
                .thenReturn(new BigDecimal("1000"));

        StaffPayrollResponse result = payroll.getStaffPayroll(
                staffId, LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 12));

        assertThat(result.getNetPayable()).isEqualByComparingTo("-970");
    }

    @Test
    void noAdvancesMeansNothingIsSubtracted() {
        when(userRepository.findById(staffId))
                .thenReturn(Optional.of(staff(staffId, "Jane", "Doe", UserRole.STAFF)));
        stubWeek(staffId, 10, 8, 4);
        when(orderRepository.countPaidDaysWorked(eq(staffId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(5L);

        StaffPayrollResponse result = payroll.getStaffPayroll(
                staffId, LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 12));

        assertThat(result.getSalaryAdvances()).isEqualByComparingTo("0");
        assertThat(result.getNetPayable()).isEqualByComparingTo("1700");
    }

    @Test
    void allStaffPayrollSubtractsEachPersonsOwnAdvances() {
        UUID secondId = UUID.randomUUID();
        when(userRepository.findAllByDeletedAtIsNullAndRoleNot(UserRole.ADMIN))
                .thenReturn(List.of(
                        staff(staffId, "Jane", "Doe", UserRole.STAFF),
                        staff(secondId, "John", "Roe", UserRole.CASHIER)));
        when(orderRepository.countPaidOrdersByStaff(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(new Object[]{staffId, 4L}, new Object[]{secondId, 2L}));
        when(orderRepository.countPaidDaysWorkedByStaff(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(new Object[]{staffId, 5L}, new Object[]{secondId, 1L}));
        when(salaryAdvanceRepository.sumAdvancesByUser(
                any(LocalDateTime.class), any(LocalDateTime.class), eq(RequisitionStatus.PAID)))
                .thenReturn(List.<Object[]>of(new Object[]{staffId, new BigDecimal("400")}));

        List<StaffPayrollResponse> result = payroll.getAllStaffPayroll(
                LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 12));

        // Both at the low tier (no leads): Jane 4*30 + 5*300 = 1620, John 2*30 + 1*300 = 360.
        assertThat(result.get(0).getNetPayable()).isEqualByComparingTo("1220");
        assertThat(result.get(1).getSalaryAdvances()).isEqualByComparingTo("0");
        assertThat(result.get(1).getNetPayable()).isEqualByComparingTo("360");
    }

    // ---- range limits (M12) ----------------------------------------------------------------

    @Test
    void aRangeOfExactlyNinetyTwoDaysIsAccepted() {
        when(userRepository.findAllByDeletedAtIsNullAndRoleNot(UserRole.ADMIN)).thenReturn(List.of());

        LocalDate from = LocalDate.of(2025, 1, 1);

        assertThat(payroll.getAllStaffPayroll(from, from.plusDays(91))).isEmpty();
    }

    @Test
    void aRangeLongerThanNinetyTwoDaysIsRejectedBeforeAnyQuery() {
        LocalDate from = LocalDate.of(2025, 1, 1);

        assertThatThrownBy(() -> payroll.getAllStaffPayroll(from, from.plusDays(92)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("92");
        assertThatThrownBy(() -> payroll.getStaffPayroll(staffId, from, from.plusDays(400)))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(orderRepository, leadRepository, userRepository, salaryAdvanceRepository);
    }

    @Test
    void aReversedRangeIsRejected() {
        assertThatThrownBy(() -> payroll.getAllStaffPayroll(
                LocalDate.of(2025, 2, 1), LocalDate.of(2025, 1, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("before");
    }

    @Test
    void missingDatesAreRejected() {
        assertThatThrownBy(() -> payroll.getAllStaffPayroll(null, LocalDate.of(2025, 1, 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---- totals for the profit report (#24) ------------------------------------------------

    @Test
    void totalsAddEveryStaffMembersCommissionDailyPayAndAdvances() {
        UUID secondId = UUID.randomUUID();
        when(userRepository.findAllByDeletedAtIsNullAndRoleNot(UserRole.ADMIN))
                .thenReturn(List.of(
                        staff(staffId, "Jane", "Doe", UserRole.STAFF),
                        staff(secondId, "John", "Roe", UserRole.CASHIER)));
        when(orderRepository.countPaidOrdersByStaff(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(new Object[]{staffId, 4L}, new Object[]{secondId, 2L}));
        when(orderRepository.countPaidDaysWorkedByStaff(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(new Object[]{staffId, 5L}, new Object[]{secondId, 1L}));
        when(salaryAdvanceRepository.sumAdvancesByUser(
                any(LocalDateTime.class), any(LocalDateTime.class), eq(RequisitionStatus.PAID)))
                .thenReturn(List.<Object[]>of(new Object[]{staffId, new BigDecimal("400")}));

        PayrollTotalsResponse totals = payroll.getPayrollTotals(
                LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 12));

        assertThat(totals.getCommission()).isEqualByComparingTo("180");  // 120 + 60
        assertThat(totals.getDailyPay()).isEqualByComparingTo("1800");   // 1500 + 300
        assertThat(totals.getStaffPay()).isEqualByComparingTo("1980");
        assertThat(totals.getSalaryAdvances()).isEqualByComparingTo("400");
    }

    @Test
    void totalsAllowAYearButNotMore() {
        when(userRepository.findAllByDeletedAtIsNullAndRoleNot(UserRole.ADMIN)).thenReturn(List.of());

        LocalDate from = LocalDate.of(2025, 1, 1);

        assertThat(payroll.getPayrollTotals(from, from.plusDays(365)).getStaffPay()).isEqualByComparingTo("0");
        assertThatThrownBy(() -> payroll.getPayrollTotals(from, from.plusDays(366)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
