package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.StaffPayrollResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PayrollService {

    StaffPayrollResponse getStaffPayroll(UUID staffId, LocalDate from, LocalDate to);

    List<StaffPayrollResponse> getAllStaffPayroll(LocalDate from, LocalDate to);
}

