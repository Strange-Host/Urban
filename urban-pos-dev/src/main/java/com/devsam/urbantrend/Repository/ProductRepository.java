package com.devsam.urbantrend.Repository;

import com.devsam.urbantrend.Entity.Product;
import com.devsam.urbantrend.Entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    Page<Product> findAllByDeletedAtIsNull(Pageable pageable);

    Optional<Product> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsBySku(String sku);

    Optional<Product> findFirstByNameIgnoreCaseAndDeletedAtIsNull(String name);

    List<Product> findAllByDeletedAtIsNullAndAssignedStaff(User assignedStaff);

    Page<Product> findAllByDeletedAtIsNullAndAssignedStaff(User assignedStaff, Pageable pageable);

    Page<Product> findByNameIgnoreCaseContainingAndDeletedAtIsNull(String name, Pageable pageable);

    Page<Product> findByNameIgnoreCaseContainingAndDeletedAtIsNullAndAssignedStaff(
            String name,
            User assignedStaff,
            Pageable pageable
    );

    @Query("""
        SELECT p.id, p.name, s.id, s.firstName, s.lastName
        FROM Product p
        JOIN p.assignedStaff s
        WHERE p.deletedAt IS NULL
    """)
    List<Object[]> findStaffAssignments();
}

