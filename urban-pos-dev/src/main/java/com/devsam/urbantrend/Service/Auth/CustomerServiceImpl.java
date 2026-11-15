package com.devsam.urbantrend.Service.Auth;


import com.devsam.urbantrend.Dtos.CustomerRequest;
import com.devsam.urbantrend.Entity.Customer;
import com.devsam.urbantrend.Repository.CustomerRepository;
import com.devsam.urbantrend.Util.PhoneNumbers;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import com.devsam.urbantrend.Errors.ResourceNotFoundException;

@Service
@Transactional
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService{

    private final CustomerRepository customerRepository;

    @Override
    public Customer addCustomer(CustomerRequest request) {
        return findOrCreate(request.getName(), request.getPhone(), request.getEmail(), request.getCity_town());
    }

    @Override
    public Customer findOrCreate(String name, String phone, String email, String city) {
        String canonicalPhone = PhoneNumbers.normalize(phone);

        if (canonicalPhone != null) {
            List<Customer> matches = customerRepository.findAllByPhoneIn(PhoneNumbers.variants(phone));

            // Prefer a live customer; fall back to a deleted one, which is brought back.
            Customer existing = matches.stream()
                    .filter(c -> c.getDeletedAt() == null)
                    .findFirst()
                    .orElse(matches.stream().findFirst().orElse(null));

            if (existing != null) {
                if (existing.getDeletedAt() != null) {
                    existing.setDeletedAt(null);
                    existing.setName(name);
                }
                // A second person sharing a phone, or a typo, must not rename the customer.
                if (isBlank(existing.getName())) {
                    existing.setName(name);
                }
                if (isBlank(existing.getEmail()) && !isBlank(email)) {
                    existing.setEmail(email);
                }
                if (isBlank(existing.getCity_town()) && !isBlank(city)) {
                    existing.setCity_town(city);
                }
                // Move old spellings (254..., spaces) to the canonical form.
                if (!canonicalPhone.equals(existing.getPhone())
                        && customerRepository.findAllByPhone(canonicalPhone).isEmpty()) {
                    existing.setPhone(canonicalPhone);
                }
                return customerRepository.save(existing);
            }
        }

        Customer customer = Customer.builder()
                .name(name)
                .phone(canonicalPhone)
                .email(email)
                .city_town(city)
                .build();

        return customerRepository.save(customer);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @Override
    public Page<Customer> getAllCustomers(Pageable pageable) {
        return customerRepository.findAllByDeletedAtIsNull(pageable);
    }

    @Override
    public Customer getCustomerById(UUID id) {
        return customerRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
    }

    // Soft delete: orders keep pointing at the customer, and a hard delete failed with a
    // foreign-key error as soon as the customer had any order.
    @Override
    @Transactional
    public void deleteCustomerById(UUID id) {
        Customer customer = customerRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
        customer.setDeletedAt(LocalDateTime.now());
        customerRepository.save(customer);
    }

}
