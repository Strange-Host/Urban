package com.devsam.urbantrend.Controller;

import com.devsam.urbantrend.Dtos.AssignCourierRequest;
import com.devsam.urbantrend.Dtos.OrderRequest;
import com.devsam.urbantrend.Dtos.OrderResponse;
import com.devsam.urbantrend.Dtos.StaffOrderSummary;
import com.devsam.urbantrend.Dtos.UpdateOrderNotesRequest;
import com.devsam.urbantrend.Entity.Order;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Enum.OrderStatus;
import com.devsam.urbantrend.Repository.UserRepository;
import com.devsam.urbantrend.Roles.UserRole;
import com.devsam.urbantrend.Security.OrderAccess;
import com.devsam.urbantrend.Service.Auth.OrderCheckoutService;
import com.devsam.urbantrend.Service.Auth.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderCheckoutService orderCheckoutService;
    private final UserRepository userRepository;

    @PostMapping
    @PreAuthorize("hasAuthority('CAN_CREATE_ORDERS')")
    public ResponseEntity<Order> createOrder(
            @Valid @RequestBody OrderRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getUser(userDetails);
        Order order = orderCheckoutService.create(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CAN_VIEW_ORDERS')")
    public ResponseEntity<Page<OrderResponse>> getAllOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) UUID staffId,
            @RequestParam(required = false) UUID courierId,
            @RequestParam(required = false) String query,

            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getUser(userDetails);

        // Staff only ever see their own orders, whatever filter they send.
        UUID effectiveStaffId = OrderAccess.seesAllOrders(user) ? staffId : user.getId();

        return ResponseEntity.ok(orderService.getAll(status, effectiveStaffId, courierId, query, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CAN_VIEW_ORDERS')")
    public ResponseEntity<Order> getOrderById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Order order = orderService.getById(id);
        OrderAccess.requireAccess(getUser(userDetails), order);
        return ResponseEntity.ok(order);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('CAN_UPDATE_ORDER_STATUS')")
    public ResponseEntity<Order> updateOrderStatus(
            @PathVariable UUID id,
            @RequestParam OrderStatus status,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getUser(userDetails);
        OrderAccess.requireAccess(user, orderService.getById(id));
        return ResponseEntity.ok(orderService.updateStatus(id, status, user));
    }

    @PatchMapping("/{id}/notes")
    @PreAuthorize("hasAuthority('CAN_UPDATE_ORDER')")
    public ResponseEntity<Order> updateOrderNotes(
            @PathVariable UUID id,
            @RequestBody UpdateOrderNotesRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getUser(userDetails);
        OrderAccess.requireAccess(user, orderService.getById(id));
        return ResponseEntity.ok(orderService.updateNotes(id, request, user));
    }

    @PatchMapping("/{id}/courier")
    @PreAuthorize("hasAuthority('CAN_UPDATE_ORDER')")
    public ResponseEntity<Order> assignCourier(
            @PathVariable UUID id,
            @RequestBody AssignCourierRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getUser(userDetails);
        OrderAccess.requireAccess(user, orderService.getById(id));
        return ResponseEntity.ok(orderService.assignCourier(id, request, user));
    }

    @GetMapping("/dashboard/top-selling")
    @PreAuthorize("hasAuthority('CAN_VIEW_TOP_SELLING')")
    public ResponseEntity<?> topSelling(
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @RequestParam(defaultValue = "5") int limit
    ) {
        return ResponseEntity.ok(orderService.getTopSellingProducts(from, to, limit));
    }

    @GetMapping("/staff/{staffId}/summary")
    @PreAuthorize("hasAuthority('CAN_VIEW_ORDER_SUMMARY')")
    public StaffOrderSummary getStaffSummary(
            @PathVariable UUID staffId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User current = getUser(userDetails);

        boolean elevated = current.getRole() == UserRole.ADMIN || current.getRole() == UserRole.MANAGER;

        if (!elevated && !current.getId().equals(staffId)) {
            throw new AccessDeniedException("Not allowed to view this summary");
        }

        return orderService.getStaffSummary(staffId);
    }

    private User getUser(UserDetails userDetails) {
        return userRepository.findByEmailAndDeletedAtIsNull(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}