package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.CustomerRequest;
import com.devsam.urbantrend.Entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CustomerService  {

    Customer addCustomer(CustomerRequest request);

    Page<Customer> getAllCustomers(Pageable pageable);

    Customer getCustomerById(UUID id);

    void deleteCustomerById(UUID id);



}
