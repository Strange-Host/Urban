package com.devsam.urbantrend.Repository;

import com.devsam.urbantrend.Entity.Payment;
import com.devsam.urbantrend.Enum.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findAllByOrder_Id(UUID orderId);

    Page<Payment> findAllByMatchedFalse(Pageable pageable);

    Optional<Payment> findByTransactionRef(String transactionRef);

    boolean existsByTransactionRef(String transactionRef);

    Page<Payment> findAllByStatus(PaymentStatus status, Pageable pageable);

}
