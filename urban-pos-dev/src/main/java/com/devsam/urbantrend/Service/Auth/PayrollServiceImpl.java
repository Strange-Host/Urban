package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.PayrollTotalsResponse;
import com.devsam.urbantrend.Dtos.StaffPayrollResponse;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Enum.RequisitionStatus;
import com.devsam.urbantrend.Errors.ResourceNotFoundException;
import com.devsam.urbantrend.Repository.LeadRepository;
import com.devsam.urbantrend.Repository.OrderRepository;
import com.devsam.urbantrend.Repository.SalaryAdvanceRepository;
import com.devsam.urbantrend.Repository.UserRepository;
import com.devsam.urbantrend.Roles.UserRole;
import com.devsam.urbantrend.Util.DateRanges;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
    private final SalaryAdvanceRepository salaryAdvanceRepository;

    private static final BigDecimal DAILY_PAY = BigDecimal.valueOf(300);

    // Weekly KPI = (leads assigned that week that converted AND whose order
    // was fully paid) / (leads assigned that week)
    private static final double KPI_THRESHOLD_PERCENT = 80.0;
    private static final BigDecimal COMMISSION_HIGH_KPI = BigDecimal.valueOf(50);
    private static final BigDecimal COMMISSION_LOW_KPI = BigDecimal.valueOf(30);

    /** Largest range the payroll endpoints accept; each week costs several queries. */
    static final long MAX_RANGE_DAYS = 92;

    /** Largest range the profit report accepts (it only needs the grouped totals). */
    static final long MAX_TOTALS_RANGE_DAYS = 366;

    @Override
    public StaffPayrollResponse getStaffPayroll(UUID staffId, LocalDate from, LocalDate to) {

        validateRange(from, to, MAX_RANGE_DAYS);

        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));

        long totalPaidOrders = 0;
        BigDecimal totalCommission = BigDecimal.ZERO;

        // Weeks are fixed Monday-to-Sunday weeks, independent of the dates asked for.
        // The weekly KPI (and so the commission rate) is measured over the whole week even
        // when the range only covers part of it; only the paid orders are limited to the
        // range. Otherwise the same order would earn a different commission depending on
        // which dates the report happened to start on.
        for (LocalDate monday = DateRanges.startOfWeek(from); !monday.isAfter(to); monday = monday.plusDays(7)) {

            LocalDate sunday = monday.plusDays(6);
            LocalDateTime weekStart = DateRanges.startOfDay(monday);
            LocalDateTime weekEnd = DateRanges.endOfDay(sunday);

            LocalDateTime paidStart = DateRanges.startOfDay(later(monday, from));
            LocalDateTime paidEnd = DateRanges.endOfDay(earlier(sunday, to));

            long paidOrdersInRange = orderRepository.countPaidOrders(staffId, paidStart, paidEnd);

            totalCommission = totalCommission.add(
                    weeklyCommission(
                            leadRepository.countAssignedLeads(staffId, weekStart, weekEnd),
                            leadRepository.countConvertedAndPaidLeads(staffId, weekStart, weekEnd),
                            paidOrdersInRange
                    )
            );
            totalPaidOrders += paidOrdersInRange;
        }

        LocalDateTime rangeStart = DateRanges.startOfDay(from);
        LocalDateTime rangeEnd = DateRanges.endOfDay(to);

        long daysWorked = orderRepository.countPaidDaysWorked(staffId, rangeStart, rangeEnd);

        BigDecimal advances = salaryAdvanceRepository
                .sumAdvancesForUser(staffId, rangeStart, rangeEnd, RequisitionStatus.PAID);

        return response(staff, totalPaidOrders, totalCommission, daysWorked, advances);
    }

    @Override
    public List<StaffPayrollResponse> getAllStaffPayroll(LocalDate from, LocalDate to) {

        validateRange(from, to, MAX_RANGE_DAYS);

        return computeAllStaffPayroll(from, to);
    }

    @Override
    public PayrollTotalsResponse getPayrollTotals(LocalDate from, LocalDate to) {

        validateRange(from, to, MAX_TOTALS_RANGE_DAYS);

        BigDecimal commission = BigDecimal.ZERO;
        BigDecimal dailyPay = BigDecimal.ZERO;
        BigDecimal advances = BigDecimal.ZERO;

        for (StaffPayrollResponse row : computeAllStaffPayroll(from, to)) {
            commission = commission.add(row.getCommission());
            dailyPay = dailyPay.add(row.getDailySalary());
            advances = advances.add(row.getSalaryAdvances());
        }

        return PayrollTotalsResponse.builder()
                .commission(commission)
                .dailyPay(dailyPay)
                .staffPay(commission.add(dailyPay))
                .salaryAdvances(advances)
                .build();
    }

    private List<StaffPayrollResponse> computeAllStaffPayroll(LocalDate from, LocalDate to) {

        List<User> staff = userRepository.findAllByDeletedAtIsNullAndRoleNot(UserRole.ADMIN);

        if (staff.isEmpty()) return List.of();

        Map<UUID, Long> paidOrders = new HashMap<>();
        Map<UUID, BigDecimal> commission = new HashMap<>();

        // Same fixed-week walk as the single-staff path, but one grouped query
        // per metric per week instead of one query per staff per week.
        for (LocalDate monday = DateRanges.startOfWeek(from); !monday.isAfter(to); monday = monday.plusDays(7)) {

            LocalDate sunday = monday.plusDays(6);
            LocalDateTime weekStart = DateRanges.startOfDay(monday);
            LocalDateTime weekEnd = DateRanges.endOfDay(sunday);

            LocalDateTime paidStart = DateRanges.startOfDay(later(monday, from));
            LocalDateTime paidEnd = DateRanges.endOfDay(earlier(sunday, to));

            Map<UUID, Long> assignedLeads = toCountMap(leadRepository.countAssignedLeadsByStaff(weekStart, weekEnd));
            Map<UUID, Long> convertedLeads = toCountMap(leadRepository.countConvertedAndPaidLeadsByStaff(weekStart, weekEnd));
            Map<UUID, Long> paidInRange = toCountMap(orderRepository.countPaidOrdersByStaff(paidStart, paidEnd));

            for (User member : staff) {
                UUID id = member.getId();
                long paid = paidInRange.getOrDefault(id, 0L);

                commission.merge(id, weeklyCommission(
                        assignedLeads.getOrDefault(id, 0L),
                        convertedLeads.getOrDefault(id, 0L),
                        paid
                ), BigDecimal::add);
                paidOrders.merge(id, paid, Long::sum);
            }
        }

        LocalDateTime rangeStart = DateRanges.startOfDay(from);
        LocalDateTime rangeEnd = DateRanges.endOfDay(to);
        Map<UUID, Long> daysWorked = toCountMap(
                orderRepository.countPaidDaysWorkedByStaff(rangeStart, rangeEnd));
        Map<UUID, BigDecimal> advances = toAmountMap(
                salaryAdvanceRepository.sumAdvancesByUser(rangeStart, rangeEnd, RequisitionStatus.PAID));

        return staff.stream()
                .map(member -> response(
                        member,
                        paidOrders.getOrDefault(member.getId(), 0L),
                        commission.getOrDefault(member.getId(), BigDecimal.ZERO),
                        daysWorked.getOrDefault(member.getId(), 0L),
                        advances.get(member.getId())
                ))
                .toList();
    }

    private static void validateRange(LocalDate from, LocalDate to, long maxDays) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Both a from and a to date are required");
        }
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("The end date cannot be before the start date");
        }
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        if (days > maxDays) {
            throw new IllegalArgumentException(
                    "Choose a range of at most " + maxDays + " days (you asked for " + days + ")");
        }
    }

    private static LocalDate later(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    private static LocalDate earlier(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
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

    private StaffPayrollResponse response(User staff, long paidOrders, BigDecimal commission,
                                          long daysWorked, BigDecimal advances) {

        BigDecimal dailySalary = DAILY_PAY.multiply(BigDecimal.valueOf(daysWorked));
        BigDecimal totalPayable = commission.add(dailySalary);
        BigDecimal salaryAdvances = advances == null ? BigDecimal.ZERO : advances;

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
                .salaryAdvances(salaryAdvances)
                .netPayable(totalPayable.subtract(salaryAdvances))
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

    private static Map<UUID, BigDecimal> toAmountMap(List<Object[]> rows) {

        Map<UUID, BigDecimal> amounts = new HashMap<>();

        for (Object[] row : rows) {
            if (row[0] == null || row[1] == null) continue;
            Object value = row[1];
            amounts.put((UUID) row[0],
                    value instanceof BigDecimal bd ? bd : new BigDecimal(value.toString()));
        }
        return amounts;
    }
}
