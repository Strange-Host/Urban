package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.OrderRequest;
import com.devsam.urbantrend.Entity.Customer;
import com.devsam.urbantrend.Entity.Order;
import com.devsam.urbantrend.Entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderCheckoutServiceTest {

    @Mock private CustomerService customerService;
    @Mock private OrderService orderService;

    private OrderCheckoutService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new OrderCheckoutService(customerService, orderService);
        user = new User();
        user.setId(UUID.randomUUID());
    }

    @Test
    void anExistingCustomerIdIsUsedAsIs() {
        UUID customerId = UUID.randomUUID();
        OrderRequest request = new OrderRequest();
        request.setCustomerId(customerId);
        when(orderService.create(request, user)).thenReturn(new Order());

        service.create(request, user);

        verify(customerService, never()).findOrCreate(any(), any(), any(), any());
        assertThat(request.getCustomerId()).isEqualTo(customerId);
    }

    @Test
    void customerDetailsAreResolvedBeforeTheOrderIsCreated() {
        Customer customer = Customer.builder().name("Amina").build();
        customer.setId(UUID.randomUUID());
        when(customerService.findOrCreate("Amina", "0712345678", null, "Nairobi")).thenReturn(customer);

        OrderRequest request = new OrderRequest();
        request.setCustomerName(" Amina ");
        request.setCustomerPhone("0712345678");
        request.setCustomerCity("Nairobi");
        when(orderService.create(request, user)).thenReturn(new Order());

        service.create(request, user);

        assertThat(request.getCustomerId()).isEqualTo(customer.getId());
    }

    @Test
    void anOrderWithNoCustomerAtAllIsRejectedBeforeAnythingIsSaved() {
        OrderRequest request = new OrderRequest();

        assertThatThrownBy(() -> service.create(request, user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Customer");

        verify(customerService, never()).findOrCreate(any(), any(), any(), any());
        verify(orderService, never()).create(any(), any());
    }
}
