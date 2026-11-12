package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.StaffPayrollResponse;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Repository.LeadRepository;
import com.devsam.urbantrend.Repository.OrderRepository;
import com.devsam.urbantrend.Repository.UserRepository;
import com.devsam.urbantrend.Roles.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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

    private PayrollServiceImpl payroll;

    private final UUID staffId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        payroll = new PayrollServiceImpl(orderRepository, userRepository, leadRepository);
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
}
