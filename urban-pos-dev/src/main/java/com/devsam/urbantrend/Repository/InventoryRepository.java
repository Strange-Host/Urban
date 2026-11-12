package com.devsam.urbantrend.Repository;

import com.devsam.urbantrend.Entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    Optional<Inventory> findByProductId(UUID productId);

    @Query("""
        SELECT i FROM Inventory i
        JOIN FETCH i.product p
        LEFT JOIN FETCH p.category
        LEFT JOIN FETCH p.assignedStaff
    """)
    List<Inventory> findAllWithProduct();

    @Query("""
        SELECT i FROM Inventory i
        WHERE i.quantity <= i.reorderLevel
    """)
    List<Inventory> findLowStock();
}
