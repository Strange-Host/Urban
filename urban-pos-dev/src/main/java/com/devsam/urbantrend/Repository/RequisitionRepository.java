package com.devsam.urbantrend.Repository;

import com.devsam.urbantrend.Entity.Requisition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RequisitionRepository extends JpaRepository<Requisition, UUID> {
    Page<Requisition> findAllByDeletedAtIsNull(Pageable pageable);
    Page<Requisition> findAllByRequestedBy_IdAndDeletedAtIsNull(UUID userId, Pageable pageable);
    Optional<Requisition> findByIdAndDeletedAtIsNull(UUID id);
}

