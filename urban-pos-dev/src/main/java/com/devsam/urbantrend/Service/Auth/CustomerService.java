package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.CustomerRequest;
import com.devsam.urbantrend.Entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CustomerService  {

    Customer addCustomer(CustomerRequest request);

    /**
     * The customer with this phone (in any spelling), created if there is none. Existing
     * details are never overwritten; only blanks are filled. A missing phone always creates
     * a new customer, because "no phone" is not an identity.
     */
    Customer findOrCreate(String name, String phone, String email, String city);

    Page<Customer> getAllCustomers(Pageable pageable);

    Customer getCustomerById(UUID id);

    void deleteCustomerById(UUID id);



}
