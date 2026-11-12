package com.devsam.urbantrend.Repository;

import com.devsam.urbantrend.Entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findByPhone(String phoneNumber);

    List<Customer> findAllByPhone(String phoneNumber);
}
