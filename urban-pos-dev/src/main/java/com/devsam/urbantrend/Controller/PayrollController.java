package com.devsam.urbantrend.Controller;

import com.devsam.urbantrend.Dtos.StaffPayrollResponse;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Repository.UserRepository;
import com.devsam.urbantrend.Roles.UserRole;
import com.devsam.urbantrend.Service.Auth.PayrollService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payroll")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;
    private final UserRepository userRepository;

    @GetMapping("/staff/{staffId}")
    @PreAuthorize("hasAuthority('CAN_VIEW_OWN_PAYROLL') or hasAuthority('CAN_VIEW_PAYROLL_SUMMARY')")
    public ResponseEntity<StaffPayrollResponse> staffPayroll(
            @PathVariable UUID staffId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User current = userRepository.findByEmailAndDeletedAtIsNull(userDetails.getUsername())
                .orElseThrow(() -> new AccessDeniedException("User not found"));

        boolean elevated = current.getRole() == UserRole.ADMIN || current.getRole() == UserRole.MANAGER;

        if (!elevated && !current.getId().equals(staffId)) {
            throw new AccessDeniedException("Not allowed to view this payroll");
        }

        return ResponseEntity.ok(payrollService.getStaffPayroll(staffId, from, to));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('CAN_VIEW_PAYROLL_SUMMARY')")
    public ResponseEntity<List<StaffPayrollResponse>> payrollSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(payrollService.getAllStaffPayroll(from, to));
    }
}
