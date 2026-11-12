package com.devsam.urbantrend.Repository;

import com.devsam.urbantrend.Entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    boolean existsByNameIgnoreCase(String name);

    Optional<Category> findByIdAndDeletedAtIsNull(UUID id);

    Page<Category> findAllByDeletedAtIsNull(Pageable pageable);

    @Query("""
        SELECT c FROM Category c
        WHERE c.deletedAt IS NULL
          AND (LOWER(c.name) LIKE :pattern OR LOWER(c.description) LIKE :pattern)
    """)
    Page<Category> searchByPattern(@Param("pattern") String pattern, Pageable pageable);
}
