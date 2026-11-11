package com.devsam.urbantrend.Controller;

import com.devsam.urbantrend.Dtos.PaymentRequest;
import com.devsam.urbantrend.Dtos.PaymentResponse;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Repository.UserRepository;
import com.devsam.urbantrend.Service.Auth.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final UserRepository userRepository;

    @PostMapping
    @PreAuthorize("hasAuthority('CAN_CREATE_PAYMENTS')")
    public PaymentResponse recordPayment(
            @RequestBody PaymentRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getUser(userDetails);
        return paymentService.recordPayment(request, user);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CAN_VIEW_PAYMENTS')")
    public Page<PaymentResponse> getAllPayments(Pageable pageable) {
        return paymentService.getAllPayments(pageable);
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAuthority('CAN_VIEW_ORDER_PAYMENTS')")
    public List<PaymentResponse> getPaymentsForOrder(@PathVariable UUID orderId) {
        return paymentService.getPaymentsForOrder(orderId);
    }

    @GetMapping("/unmatched")
    @PreAuthorize("hasAuthority('CAN_VIEW_PAYMENTS')")
    public Page<PaymentResponse> getUnmatched(Pageable pageable) {
        return paymentService.getUnmatchedPayments(pageable);
    }

    @PostMapping("/{paymentId}/match/{orderId}")
    @PreAuthorize("hasAuthority('CAN_MATCH_PAYMENTS')")
    public PaymentResponse matchPayment(
            @PathVariable UUID paymentId,
            @PathVariable UUID orderId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getUser(userDetails);
        return paymentService.matchPaymentToOrder(paymentId, orderId, user);
    }

    private User getUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}