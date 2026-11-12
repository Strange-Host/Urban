package com.devsam.urbantrend.Repository;

import com.devsam.urbantrend.Entity.Payment;
import com.devsam.urbantrend.Enum.PaymentMethod;
import com.devsam.urbantrend.Enum.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findAllByOrder_Id(UUID orderId);

    Page<Payment> findAllByMatchedFalse(Pageable pageable);

    Optional<Payment> findByTransactionRef(String transactionRef);

    boolean existsByTransactionRef(String transactionRef);

    Page<Payment> findAllByStatus(PaymentStatus status, Pageable pageable);

    @Query(
            value = """
        SELECT p FROM Payment p
        WHERE (:method IS NULL OR p.method = :method)
          AND (:hasQuery = false
              OR LOWER(p.transactionRef) LIKE :pattern
              OR LOWER(p.payerName) LIKE :pattern
              OR LOWER(p.payerPhone) LIKE :pattern)
    """,
            countQuery = """
        SELECT COUNT(p) FROM Payment p
        WHERE (:method IS NULL OR p.method = :method)
          AND (:hasQuery = false
              OR LOWER(p.transactionRef) LIKE :pattern
              OR LOWER(p.payerName) LIKE :pattern
              OR LOWER(p.payerPhone) LIKE :pattern)
    """
    )
    Page<Payment> search(
            @Param("method") PaymentMethod method,
            @Param("hasQuery") boolean hasQuery,
            @Param("pattern") String pattern,
            Pageable pageable
    );
}
