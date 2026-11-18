package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.OrderRequest;
import com.devsam.urbantrend.Entity.Customer;
import com.devsam.urbantrend.Entity.Order;
import com.devsam.urbantrend.Entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates an order together with its customer in one transaction. When the order is rejected
 * (stock, payments, validation) the customer that was about to be created is rolled back too,
 * so a failed sale no longer leaves a stray or changed customer record behind.
 */
@Service
@RequiredArgsConstructor
public class OrderCheckoutService {

    private final CustomerService customerService;
    private final OrderService orderService;

    @Transactional
    public Order create(OrderRequest request, User user) {

        if (request.getCustomerId() == null) {
            String name = request.getCustomerName();
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Customer is required");
            }

            Customer customer = customerService.findOrCreate(
                    name.trim(),
                    request.getCustomerPhone(),
                    request.getCustomerEmail(),
                    request.getCustomerCity());
            request.setCustomerId(customer.getId());
        }

        return orderService.create(request, user);
    }
}
