package com.devsam.urbantrend.Repository;

import com.devsam.urbantrend.Entity.StockUnit;
import com.devsam.urbantrend.Enum.StockUnitStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockUnitRepository extends JpaRepository<StockUnit, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<StockUnit> findBySerial(String serial);

    List<StockUnit> findByBatchIdOrderBySerialAsc(UUID batchId);

    List<StockUnit> findByOrderIdAndStatus(UUID orderId, StockUnitStatus status);

    long countByOrderIdAndProduct_Id(UUID orderId, UUID productId);

    long countByOrderId(UUID orderId);

    @Query(value = "SELECT nextval('stock_unit_serial_seq') FROM generate_series(1, :count)", nativeQuery = true)
    List<Number> nextSerialNumbers(@Param("count") int count);
}
