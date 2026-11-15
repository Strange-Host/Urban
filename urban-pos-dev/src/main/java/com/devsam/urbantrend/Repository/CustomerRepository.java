package com.devsam.urbantrend.Repository;

import com.devsam.urbantrend.Entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findByPhone(String phoneNumber);

    List<Customer> findAllByPhone(String phoneNumber);

    // Includes soft-deleted rows on purpose: the unique phone index still covers them.
    List<Customer> findAllByPhoneIn(Collection<String> phones);

    Page<Customer> findAllByDeletedAtIsNull(Pageable pageable);

    java.util.Optional<Customer> findByIdAndDeletedAtIsNull(UUID id);
}
