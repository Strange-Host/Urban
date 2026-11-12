package com.devsam.urbantrend.Repository;

import com.devsam.urbantrend.Entity.Courier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CourierRepository extends JpaRepository<Courier, UUID> {

    Page<Courier> findAllByActiveTrue(Pageable pageable);

    List<Courier> findAllByActiveTrueOrderByNameAsc();

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumberAndIdNot(String phoneNumber, UUID id);

    @Query("""
        SELECT c FROM Courier c
        WHERE (:activeOnly = false OR c.active = true)
          AND (LOWER(c.name) LIKE :pattern
               OR LOWER(c.phoneNumber) LIKE :pattern
               OR LOWER(c.vehicleInfo) LIKE :pattern)
    """)
    Page<Courier> searchByPattern(
            @Param("activeOnly") boolean activeOnly,
            @Param("pattern") String pattern,
            Pageable pageable
    );
}
