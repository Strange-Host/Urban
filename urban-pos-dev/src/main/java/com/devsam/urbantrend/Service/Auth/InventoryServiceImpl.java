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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
@Service
@RequiredArgsConstructor
@Transactional
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    @Override
    public Inventory getInventoryByProduct(UUID productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));
        return withProduct(inventory);
    }

    private Inventory withProduct(Inventory inventory) {
        return inventoryRepository.findWithProductById(inventory.getId()).orElse(inventory);
    }

    private static void requireProductAndQuantity(InventoryRequest request, String action) {
        if (request == null || request.getProductId() == null) {
            throw new IllegalArgumentException("productId is required");
        }
        if (request.getQuantity() == null || request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(action + " quantity must be greater than zero");
        }
    }

    private static IllegalStateException insufficient(Inventory inventory, BigDecimal needed) {
        String name = inventory.getProduct() != null ? inventory.getProduct().getName() : "this product";
        return new IllegalStateException("Insufficient stock for " + name + ": "
                + inventory.getQuantity().stripTrailingZeros().toPlainString() + " available, "
                + needed.stripTrailingZeros().toPlainString() + " needed");
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

        requireProductAndQuantity(request, "Restock");

        // A deleted product cannot be restocked (it used to get stock silently).
        Product liveProduct = productRepository.findByIdAndDeletedAtIsNull(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        Inventory inventory = inventoryRepository.findByProductIdForUpdate(request.getProductId())
                .orElseGet(() -> {
                    Product product = liveProduct;

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

        return withProduct(inventoryRepository.save(inventory));
    }

    @Override
    public Inventory reduce(InventoryRequest request) {

        requireProductAndQuantity(request, "Reduce");

        Inventory inventory = inventoryRepository.findByProductIdForUpdate(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));

        if (inventory.getQuantity().compareTo(request.getQuantity()) < 0) {
            throw insufficient(inventory, request.getQuantity());
        }

        inventory.setQuantity(
                inventory.getQuantity().subtract(request.getQuantity())
        );

        return withProduct(inventoryRepository.save(inventory));
    }

    @Override
    public void reduceIfTracked(InventoryRequest request) {

        if (request.getQuantity() == null || request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Reduce quantity must be greater than zero");
        }

        // Products without an inventory row are not stock-tracked, so there is
        // nothing to deduct. Tracked products still enforce available stock.
        inventoryRepository.findByProductIdForUpdate(request.getProductId())
                .ifPresent(inventory -> {
                    if (inventory.getQuantity().compareTo(request.getQuantity()) < 0) {
                        throw insufficient(inventory, request.getQuantity());
                    }

                    inventory.setQuantity(
                            inventory.getQuantity().subtract(request.getQuantity())
                    );
                    inventoryRepository.save(inventory);
                });
    }

    @Override
    public void ensureAvailableIfTracked(InventoryRequest request) {

        if (request.getQuantity() == null || request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        inventoryRepository.findByProductId(request.getProductId())
                .ifPresent(inventory -> {
                    if (inventory.getQuantity().compareTo(request.getQuantity()) < 0) {
                        throw insufficient(inventory, request.getQuantity());
                    }
                });
    }

    @Override
    public void increaseIfTracked(InventoryRequest request) {

        inventoryRepository.findByProductIdForUpdate(request.getProductId())
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
