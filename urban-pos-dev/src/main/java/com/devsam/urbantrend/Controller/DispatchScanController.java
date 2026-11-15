package com.devsam.urbantrend.Controller;

import com.devsam.urbantrend.Dtos.ScanRequest;
import com.devsam.urbantrend.Dtos.ScanResponse;
import com.devsam.urbantrend.Service.Auth.OrderService;
import com.devsam.urbantrend.Security.OrderAccess;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Repository.UserRepository;
import com.devsam.urbantrend.Service.Auth.DispatchScanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@RestController
@RequestMapping("/api/v1/orders/scan")
@RequiredArgsConstructor
public class DispatchScanController {

    private final DispatchScanService dispatchScanService;
    private final UserRepository userRepository;
    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasAuthority('CAN_UPDATE_ORDER_STATUS')")
    public ResponseEntity<ScanResponse> scan(
            @RequestBody ScanRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        // Staff may only pack and dispatch their own orders.
        if (request.getOrderId() != null) {
            OrderAccess.requireAccess(user, orderService.getById(request.getOrderId()));
        } else if (!OrderAccess.seesAllOrders(user)) {
            // Without an order the service would pick the oldest awaiting order, which may
            // belong to someone else.
            throw new IllegalArgumentException("Choose one of your orders before scanning");
        }
        return ResponseEntity.ok(dispatchScanService.scan(request, user));
    }

    @DeleteMapping("/{barcode}")
    @PreAuthorize("hasAuthority('CAN_UPDATE_ORDER_STATUS')")
    public ResponseEntity<Void> unscan(@PathVariable String barcode) {
        dispatchScanService.unscan(barcode);
        return ResponseEntity.noContent().build();
    }
}
