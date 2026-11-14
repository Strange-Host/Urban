package com.devsam.urbantrend.Controller;

import com.devsam.urbantrend.Dtos.SettingsResponse;
import com.devsam.urbantrend.Dtos.UpdatePaybillRequest;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Errors.ResourceNotFoundException;
import com.devsam.urbantrend.Repository.UserRepository;
import com.devsam.urbantrend.Service.Auth.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;
    private final UserRepository userRepository;

    /** Any signed-in user: receipts and dispatch slips print the paybill. */
    @GetMapping
    public SettingsResponse getSettings() {
        return new SettingsResponse(settingsService.getMpesaPaybill());
    }

    @PutMapping("/mpesa-paybill")
    @PreAuthorize("hasRole('ADMIN')")
    public SettingsResponse updatePaybill(
            @RequestBody UpdatePaybillRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return new SettingsResponse(settingsService.setMpesaPaybill(request.getMpesaPaybill(), user));
    }
}
