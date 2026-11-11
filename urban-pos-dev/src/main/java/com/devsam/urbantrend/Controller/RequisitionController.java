package com.devsam.urbantrend.Controller;

import com.devsam.urbantrend.Dtos.RejectRequisitionRequest;
import com.devsam.urbantrend.Dtos.RequisitionRequest;
import com.devsam.urbantrend.Entity.Requisition;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Repository.UserRepository;
import com.devsam.urbantrend.Service.Auth.RequisitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/requisitions")
@RequiredArgsConstructor
public class RequisitionController {

    private final RequisitionService requisitionService;
    private final UserRepository userRepository;

    private User me(UserDetails ud) {
        return userRepository.findByEmail(ud.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CAN_CREATE_REQUISITIONS')")
    public Requisition create(
            @RequestBody RequisitionRequest req,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return requisitionService.create(req, me(userDetails));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAuthority('CAN_VIEW_OWN_REQUISITIONS')")
    public Page<Requisition> mine(
            Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return requisitionService.getMine(me(userDetails), pageable);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CAN_VIEW_REQUISITIONS')")
    public Page<Requisition> all(Pageable pageable) {
        return requisitionService.getAll(pageable);
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('CAN_APPROVE_REQUISITIONS')")
    public Requisition approve(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return requisitionService.approve(id, me(userDetails));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('CAN_REJECT_REQUISITIONS')")
    public Requisition reject(
            @PathVariable UUID id,
            @RequestBody RejectRequisitionRequest req,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return requisitionService.reject(id, req.getReason(), me(userDetails));
    }

    @PatchMapping("/{id}/mark-paid")
    @PreAuthorize("hasAuthority('CAN_MARK_REQUISITION_PAID')")
    public Requisition markPaid(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return requisitionService.markPaid(id, me(userDetails));
    }
}