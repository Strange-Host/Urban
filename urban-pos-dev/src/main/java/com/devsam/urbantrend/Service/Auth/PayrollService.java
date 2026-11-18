package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.PayrollTotalsResponse;
import com.devsam.urbantrend.Dtos.StaffPayrollResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PayrollService {

    /** @throws IllegalArgumentException when the range is reversed or longer than 92 days */
    StaffPayrollResponse getStaffPayroll(UUID staffId, LocalDate from, LocalDate to);

    /** @throws IllegalArgumentException when the range is reversed or longer than 92 days */
    List<StaffPayrollResponse> getAllStaffPayroll(LocalDate from, LocalDate to);

    /**
     * Pay for all non-admin staff added together, for the profit report.
     *
     * @throws IllegalArgumentException when the range is reversed or longer than 366 days
     */
    PayrollTotalsResponse getPayrollTotals(LocalDate from, LocalDate to);
}
