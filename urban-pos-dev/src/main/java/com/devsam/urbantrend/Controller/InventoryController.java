package com.devsam.urbantrend.Controller;

import com.devsam.urbantrend.Dtos.InventoryRequest;
import com.devsam.urbantrend.Entity.Inventory;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Repository.UserRepository;
import com.devsam.urbantrend.Service.Auth.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;
    private final UserRepository userRepository;

    @GetMapping("/{productId}")
    @PreAuthorize("hasAuthority('CAN_VIEW_INVENTORY')")
    public Inventory getInventoryByProduct(@PathVariable UUID productId) {
        return inventoryService.getInventoryByProduct(productId);
    }

    @PostMapping("/restock")
    @PreAuthorize("hasAuthority('CAN_RESTOCK_INVENTORY')")
    public Inventory restock(
            @RequestBody InventoryRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return inventoryService.restock(request, user);
    }

    @PostMapping("/reduce")
    @PreAuthorize("hasAuthority('CAN_REDUCE_INVENTORY')")
    public Inventory reduce(@RequestBody InventoryRequest request) {
        return inventoryService.reduce(request);
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAuthority('CAN_VIEW_LOW_STOCK')")
    public List<Inventory> getLowStock() {
        return inventoryService.getLowStock();
    }
}