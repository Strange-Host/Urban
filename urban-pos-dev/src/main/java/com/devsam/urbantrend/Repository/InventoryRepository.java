package com.devsam.urbantrend.Repository;

import com.devsam.urbantrend.Entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    Optional<Inventory> findByProductId(UUID productId);

    // Stock changes are read-modify-write; lock the row so concurrent sales, returns
    // and restocks cannot overwrite each other. Must run inside a transaction.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.product.id = :productId")
    Optional<Inventory> findByProductIdForUpdate(@Param("productId") UUID productId);

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
