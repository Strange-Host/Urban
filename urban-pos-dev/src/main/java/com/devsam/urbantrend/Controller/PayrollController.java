package com.devsam.urbantrend.Controller;

import com.devsam.urbantrend.Dtos.StaffPayrollResponse;
import com.devsam.urbantrend.Service.Auth.PayrollService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payroll")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;

    @GetMapping("/staff/{staffId}")
    @PreAuthorize("hasAuthority('CAN_VIEW_OWN_PAYROLL')")
    public ResponseEntity<StaffPayrollResponse> staffPayroll(
            @PathVariable UUID staffId,
            @RequestParam String from,
            @RequestParam String to
    ) {
        LocalDate fromDate = LocalDate.parse(from);
        LocalDate toDate = LocalDate.parse(to);
        return ResponseEntity.ok(payrollService.getStaffPayroll(staffId, fromDate, toDate));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('CAN_VIEW_PAYROLL_SUMMARY')")
    public ResponseEntity<List<StaffPayrollResponse>> payrollSummary(
            @RequestParam String from,
            @RequestParam String to
    ) {
        LocalDate fromDate = LocalDate.parse(from);
        LocalDate toDate = LocalDate.parse(to);
        return ResponseEntity.ok(payrollService.getAllStaffPayroll(fromDate, toDate));
    }
}