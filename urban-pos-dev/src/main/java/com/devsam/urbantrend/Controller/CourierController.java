package com.devsam.urbantrend.Controller;

import com.devsam.urbantrend.Dtos.CourierRequest;
import com.devsam.urbantrend.Dtos.CourierResponse;
import com.devsam.urbantrend.Dtos.OrderResponse;
import com.devsam.urbantrend.Service.Auth.CourierService;
import com.devsam.urbantrend.Service.Auth.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/couriers")
@RequiredArgsConstructor
public class CourierController {

    private final CourierService courierService;
    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasAuthority('CAN_MANAGE_COURIERS')")
    public ResponseEntity<CourierResponse> createCourier(@RequestBody CourierRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(courierService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CAN_VIEW_COURIERS')")
    public ResponseEntity<Page<CourierResponse>> getAllCouriers(
            @RequestParam(defaultValue = "false") boolean activeOnly,
            Pageable pageable
    ) {
        return ResponseEntity.ok(courierService.getAll(pageable, activeOnly));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('CAN_VIEW_COURIERS')")
    public ResponseEntity<List<CourierResponse>> getActiveCouriers() {
        return ResponseEntity.ok(courierService.getAllActive());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CAN_VIEW_COURIERS')")
    public ResponseEntity<CourierResponse> getCourierById(@PathVariable UUID id) {
        return ResponseEntity.ok(courierService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CAN_MANAGE_COURIERS')")
    public ResponseEntity<CourierResponse> updateCourier(
            @PathVariable UUID id,
            @RequestBody CourierRequest request
    ) {
        return ResponseEntity.ok(courierService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CAN_MANAGE_COURIERS')")
    public ResponseEntity<Void> deactivateCourier(@PathVariable UUID id) {
        courierService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}