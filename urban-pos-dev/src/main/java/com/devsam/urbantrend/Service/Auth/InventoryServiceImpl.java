package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.InventoryRequest;
import com.devsam.urbantrend.Entity.Inventory;
import com.devsam.urbantrend.Entity.Product;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Errors.ResourceNotFoundException;
import com.devsam.urbantrend.Repository.InventoryRepository;
import com.devsam.urbantrend.Repository.ProductRepository;
import com.devsam.urbantrend.Roles.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    @Override
    public Inventory getInventoryByProduct(UUID productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));
    }

    @Override
    public List<Inventory> getAllInventory() {
        return inventoryRepository.findAllWithProduct();
    }

    @Override
    public Inventory restock(InventoryRequest request, User user) {

        // if (user.getRole() != UserRole.ADMIN) {
        //     throw new AccessDeniedException("Only ADMIN can restock inventory");
        // }

        if (request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Restock quantity must be greater than zero");
        }

        Inventory inventory = inventoryRepository.findByProductId(request.getProductId())
                .orElseGet(() -> {
                    Product product = productRepository.findById(request.getProductId())
                            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

                    return inventoryRepository.save(
                            Inventory.builder()
                                    .product(product)
                                    .quantity(BigDecimal.ZERO)
                                    .reorderLevel(
                                            request.getReorderLevel() != null
                                                    ? request.getReorderLevel()
                                                    : BigDecimal.valueOf(5)
                                    )
                                    .build()
                    );
                });

        inventory.setQuantity(
                inventory.getQuantity().add(request.getQuantity())
        );

        // Update reorder level if provided
        if (request.getReorderLevel() != null) {
            inventory.setReorderLevel(request.getReorderLevel());
        }

        return inventoryRepository.save(inventory);
    }

    @Override
    public Inventory reduce(InventoryRequest request) {

        if (request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Reduce quantity must be greater than zero");
        }

        Inventory inventory = inventoryRepository.findByProductId(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));

        if (inventory.getQuantity().compareTo(request.getQuantity()) < 0) {
            throw new IllegalStateException("Insufficient stock");
        }

        inventory.setQuantity(
                inventory.getQuantity().subtract(request.getQuantity())
        );

        return inventoryRepository.save(inventory);
    }

    @Override
    public void reduceIfTracked(InventoryRequest request) {

        if (request.getQuantity() == null || request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Reduce quantity must be greater than zero");
        }

        // Products without an inventory row are not stock-tracked, so there is
        // nothing to deduct. Tracked products still enforce available stock.
        inventoryRepository.findByProductId(request.getProductId())
                .ifPresent(inventory -> {
                    if (inventory.getQuantity().compareTo(request.getQuantity()) < 0) {
                        throw new IllegalStateException("Insufficient stock");
                    }

                    inventory.setQuantity(
                            inventory.getQuantity().subtract(request.getQuantity())
                    );
                    inventoryRepository.save(inventory);
                });
    }

    @Override
    public void increaseIfTracked(InventoryRequest request) {

        inventoryRepository.findByProductId(request.getProductId())
                .ifPresent(inventory -> {
                    inventory.setQuantity(inventory.getQuantity().add(request.getQuantity()));
                    inventoryRepository.save(inventory);
                });
    }


    @Override
    public List<Inventory> getLowStock() {
        return inventoryRepository.findLowStock();
    }
}
