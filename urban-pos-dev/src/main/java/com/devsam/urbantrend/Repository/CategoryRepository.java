package com.devsam.urbantrend.Repository;

import com.devsam.urbantrend.Entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    boolean existsByNameIgnoreCase(String name);

    Optional<Category> findByIdAndDeletedAtIsNull(UUID id);
}
