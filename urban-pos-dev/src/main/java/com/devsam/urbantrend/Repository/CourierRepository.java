package com.devsam.urbantrend.Repository;

import com.devsam.urbantrend.Entity.Courier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CourierRepository extends JpaRepository<Courier, UUID> {

    Page<Courier> findAllByActiveTrue(Pageable pageable);

    List<Courier> findAllByActiveTrueOrderByNameAsc();

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumberAndIdNot(String phoneNumber, UUID id);
}