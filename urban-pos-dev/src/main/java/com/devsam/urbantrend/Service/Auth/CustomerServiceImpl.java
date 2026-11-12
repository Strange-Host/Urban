package com.devsam.urbantrend.Service.Auth;


import com.devsam.urbantrend.Dtos.CustomerRequest;
import com.devsam.urbantrend.Entity.Customer;
import com.devsam.urbantrend.Repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService{

    private final CustomerRepository customerRepository;

    @Override
    public Customer addCustomer(CustomerRequest request) {
        String phone = request.getPhone() == null ? null : request.getPhone().trim();
        if (phone != null && phone.isEmpty()) {
            phone = null;
        }

        if (phone != null) {
            Customer existing = customerRepository.findAllByPhone(phone)
                    .stream()
                    .findFirst()
                    .orElse(null);

            if (existing != null) {
                if (request.getName() != null && !request.getName().isBlank()) {
                    existing.setName(request.getName());
                }
                if (request.getEmail() != null && !request.getEmail().isBlank()) {
                    existing.setEmail(request.getEmail());
                }
                if (request.getCity_town() != null && !request.getCity_town().isBlank()) {
                    existing.setCity_town(request.getCity_town());
                }
                return customerRepository.save(existing);
            }
        }

        Customer customer = Customer.builder()
                .name(request.getName())
                .phone(phone)
                .email(request.getEmail())
                .city_town(request.getCity_town())
                .build();

        return customerRepository.save(customer);
    }


    @Override
    public Page<Customer> getAllCustomers(Pageable pageable) {
        return customerRepository.findAll(pageable);
    }

    @Override
    public Customer getCustomerById(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
    }

    @Override
    @Transactional
    public void deleteCustomerById(UUID id) {
        if (!customerRepository.existsById(id)) {
            throw new RuntimeException("Customer not found with id: " + id);
        }
        customerRepository.deleteById(id);
    }

}
