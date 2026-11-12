package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.StaffPayrollResponse;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Repository.LeadRepository;
import com.devsam.urbantrend.Repository.OrderRepository;
import com.devsam.urbantrend.Repository.UserRepository;
import com.devsam.urbantrend.Roles.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PayrollServiceImpl implements PayrollService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final LeadRepository leadRepository;

    private static final BigDecimal DAILY_PAY = BigDecimal.valueOf(300);

    // Weekly KPI = (leads assigned that week that converted AND whose order
    // was fully paid) / (leads assigned that week)
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

            long paidOrdersThisWeek = orderRepository.countPaidOrders(staffId, start, end);

            totalCommission = totalCommission.add(
                    weeklyCommission(
                            leadRepository.countAssignedLeads(staffId, start, end),
                            leadRepository.countConvertedAndPaidLeads(staffId, start, end),
                            paidOrdersThisWeek
                    )
            );
            totalPaidOrders += paidOrdersThisWeek;

            weekStart = weekStart.plusDays(7);
        }

        LocalDateTime rangeStart = from.atStartOfDay();
        LocalDateTime rangeEnd = to.plusDays(1).atStartOfDay().minusNanos(1);

        long daysWorked = orderRepository.countPaidDaysWorked(staffId, rangeStart, rangeEnd);

        return response(staff, totalPaidOrders, totalCommission, daysWorked);
    }

    @Override
    public List<StaffPayrollResponse> getAllStaffPayroll(LocalDate from, LocalDate to) {

        List<User> staff = userRepository.findAllByDeletedAtIsNullAndRoleNot(UserRole.ADMIN);

        if (staff.isEmpty()) return List.of();

        Map<UUID, Long> paidOrders = new HashMap<>();
        Map<UUID, BigDecimal> commission = new HashMap<>();

        // Same weekly-KPI walk as the single-staff path, but one grouped query
        // per metric per week instead of one query per staff per week.
        LocalDate weekStart = from;
        while (!weekStart.isAfter(to)) {

            LocalDate weekEnd = weekStart.plusDays(6).isAfter(to) ? to : weekStart.plusDays(6);

            LocalDateTime start = weekStart.atStartOfDay();
            LocalDateTime end = weekEnd.plusDays(1).atStartOfDay().minusNanos(1);

            Map<UUID, Long> assignedLeads = toCountMap(leadRepository.countAssignedLeadsByStaff(start, end));
            Map<UUID, Long> convertedLeads = toCountMap(leadRepository.countConvertedAndPaidLeadsByStaff(start, end));
            Map<UUID, Long> paidThisWeek = toCountMap(orderRepository.countPaidOrdersByStaff(start, end));

            for (User member : staff) {
                UUID id = member.getId();
                long paid = paidThisWeek.getOrDefault(id, 0L);

                commission.merge(id, weeklyCommission(
                        assignedLeads.getOrDefault(id, 0L),
                        convertedLeads.getOrDefault(id, 0L),
                        paid
                ), BigDecimal::add);
                paidOrders.merge(id, paid, Long::sum);
            }

            weekStart = weekStart.plusDays(7);
        }

        LocalDateTime rangeStart = from.atStartOfDay();
        LocalDateTime rangeEnd = to.plusDays(1).atStartOfDay().minusNanos(1);
        Map<UUID, Long> daysWorked = toCountMap(
                orderRepository.countPaidDaysWorkedByStaff(rangeStart, rangeEnd));

        return staff.stream()
                .map(member -> response(
                        member,
                        paidOrders.getOrDefault(member.getId(), 0L),
                        commission.getOrDefault(member.getId(), BigDecimal.ZERO),
                        daysWorked.getOrDefault(member.getId(), 0L)
                ))
                .toList();
    }

    private BigDecimal weeklyCommission(long assignedLeads, long convertedPaidLeads, long paidOrders) {

        double kpiPercent = assignedLeads == 0
                ? 0.0
                : (convertedPaidLeads * 100.0) / assignedLeads;

        BigDecimal rateThisWeek = kpiPercent >= KPI_THRESHOLD_PERCENT
                ? COMMISSION_HIGH_KPI
                : COMMISSION_LOW_KPI;

        return rateThisWeek.multiply(BigDecimal.valueOf(paidOrders));
    }

    private StaffPayrollResponse response(User staff, long paidOrders, BigDecimal commission, long daysWorked) {

        BigDecimal dailySalary = DAILY_PAY.multiply(BigDecimal.valueOf(daysWorked));
        BigDecimal totalPayable = commission.add(dailySalary);

        String staffName = (staff.getFirstName() == null ? "" : staff.getFirstName())
                + " "
                + (staff.getLastName() == null ? "" : staff.getLastName());

        return StaffPayrollResponse.builder()
                .staffId(staff.getId())
                .staffName(staffName.trim())
                .paidOrders(paidOrders)
                .commission(commission)
                .daysWorked(daysWorked)
                .dailySalary(dailySalary)
                .totalPayable(totalPayable)
                .build();
    }

    private static Map<UUID, Long> toCountMap(List<Object[]> rows) {

        Map<UUID, Long> counts = new HashMap<>();

        for (Object[] row : rows) {
            if (row[0] == null || row[1] == null) continue;
            counts.put((UUID) row[0], ((Number) row[1]).longValue());
        }
        return counts;
    }
}
