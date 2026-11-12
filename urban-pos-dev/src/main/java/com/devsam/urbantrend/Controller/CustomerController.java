package com.devsam.urbantrend.Controller;

import com.devsam.urbantrend.Dtos.CustomerRequest;
import com.devsam.urbantrend.Entity.Customer;
import com.devsam.urbantrend.Service.Auth.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    @PreAuthorize("hasAuthority('CAN_CREATE_CUSTOMERS')")
    public Customer addCustomer(@Valid @RequestBody CustomerRequest request) {
        return customerService.addCustomer(request);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CAN_VIEW_CUSTOMERS')")
    public Page<Customer> getAllCustomers(Pageable pageable) {
        return customerService.getAllCustomers(pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CAN_VIEW_CUSTOMERS')")
    public Customer getCustomerById(@PathVariable UUID id) {
        return customerService.getCustomerById(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CAN_DELETE_CUSTOMERS')")
    public ResponseEntity<Void> deleteCustomer(@PathVariable UUID id) {
        customerService.deleteCustomerById(id);
        return ResponseEntity.noContent().build();
    }

}