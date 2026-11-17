package com.devsam.urbantrend.Repository;

import com.devsam.urbantrend.Entity.StockUnit;
import com.devsam.urbantrend.Enum.StockUnitStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockUnitRepository extends JpaRepository<StockUnit, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<StockUnit> findBySerial(String serial);

    List<StockUnit> findByBatchIdOrderBySerialAsc(UUID batchId);

    // Newest printed batches first, optionally for one product.
    @Query("""
        SELECT u.batchId, p.id, p.name, COUNT(u), MIN(u.createdAt)
        FROM StockUnit u JOIN u.product p
        WHERE (:productId IS NULL OR p.id = :productId)
        GROUP BY u.batchId, p.id, p.name
        ORDER BY MIN(u.createdAt) DESC
    """)
    List<Object[]> findRecentBatches(@Param("productId") UUID productId, Pageable pageable);

    List<StockUnit> findByOrderId(UUID orderId);

    List<StockUnit> findByOrderIdAndStatus(UUID orderId, StockUnitStatus status);

    long countByOrderIdAndProduct_Id(UUID orderId, UUID productId);

    long countByOrderId(UUID orderId);

    @Query(value = "SELECT nextval('stock_unit_serial_seq') FROM generate_series(1, :count)", nativeQuery = true)
    List<Number> nextSerialNumbers(@Param("count") int count);
}
