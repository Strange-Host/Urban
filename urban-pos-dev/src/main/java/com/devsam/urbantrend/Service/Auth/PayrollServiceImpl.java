package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.StaffPayrollResponse;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Repository.LeadRepository;
import com.devsam.urbantrend.Repository.OrderRepository;
import com.devsam.urbantrend.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PayrollServiceImpl implements PayrollService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final LeadRepository leadRepository;

    private static final BigDecimal DAILY_PAY = BigDecimal.valueOf(300);

    // Weekly KPI = (leads assigned that week that converted AND whose order
    // reached DELIVERED_PAID) / (leads assigned that week)
    private static final double KPI_THRESHOLD_PERCENT = 80.0;
    private static final BigDecimal COMMISSION_HIGH_KPI = BigDecimal.valueOf(50);
    private static final BigDecimal COMMISSION_LOW_KPI = BigDecimal.valueOf(30);

    @Override
    public StaffPayrollResponse getStaffPayroll(UUID staffId, LocalDate from, LocalDate to) {

        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        long totalPaidOrders = 0;
        BigDecimal totalCommission = BigDecimal.ZERO;

        // Walk the requested range one Mon-Sun-ish 7-day block at a time,
        // since KPI (and therefore the commission rate) is calculated weekly.
        LocalDate weekStart = from;
        while (!weekStart.isAfter(to)) {

            LocalDate weekEnd = weekStart.plusDays(6).isAfter(to) ? to : weekStart.plusDays(6);

            LocalDateTime start = weekStart.atStartOfDay();
            LocalDateTime end = weekEnd.plusDays(1).atStartOfDay().minusNanos(1);

            long assignedLeads = leadRepository.countAssignedLeads(staffId, start, end);
            long convertedPaidLeads = leadRepository.countConvertedAndPaidLeads(staffId, start, end);
            long paidOrdersThisWeek = orderRepository.countPaidOrders(staffId, start, end);

            double kpiPercent = assignedLeads == 0
                    ? 0.0
                    : (convertedPaidLeads * 100.0) / assignedLeads;

            BigDecimal rateThisWeek = kpiPercent >= KPI_THRESHOLD_PERCENT
                    ? COMMISSION_HIGH_KPI
                    : COMMISSION_LOW_KPI;

            totalCommission = totalCommission.add(
                    rateThisWeek.multiply(BigDecimal.valueOf(paidOrdersThisWeek))
            );
            totalPaidOrders += paidOrdersThisWeek;

            weekStart = weekStart.plusDays(7);
        }

        LocalDateTime rangeStart = from.atStartOfDay();
        LocalDateTime rangeEnd = to.plusDays(1).atStartOfDay().minusNanos(1);

        long daysWorked = orderRepository.countPaidDaysWorked(staffId, rangeStart, rangeEnd);
        BigDecimal dailySalary = DAILY_PAY.multiply(BigDecimal.valueOf(daysWorked));

        BigDecimal totalPayable = totalCommission.add(dailySalary);

        String staffName = (staff.getFirstName() == null ? "" : staff.getFirstName())
                + " "
                + (staff.getLastName() == null ? "" : staff.getLastName());

        return StaffPayrollResponse.builder()
                .staffId(staff.getId())
                .staffName(staffName.trim())
                .paidOrders(totalPaidOrders)
                .commission(totalCommission)
                .daysWorked(daysWorked)
                .dailySalary(dailySalary)
                .totalPayable(totalPayable)
                .build();
    }

    @Override
    public List<StaffPayrollResponse> getAllStaffPayroll(LocalDate from, LocalDate to) {

        List<User> users = userRepository.findAll();

        return users.stream()
                .map(u -> getStaffPayroll(u.getId(), from, to))
                .toList();
    }
}