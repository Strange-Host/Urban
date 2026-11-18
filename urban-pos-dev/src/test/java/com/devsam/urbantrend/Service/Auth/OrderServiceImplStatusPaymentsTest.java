package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Entity.Order;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Enum.OrderStatus;
import com.devsam.urbantrend.Repository.CourierRepository;
import com.devsam.urbantrend.Repository.CustomerRepository;
import com.devsam.urbantrend.Repository.OrderItemRepository;
import com.devsam.urbantrend.Repository.OrderRepository;
import com.devsam.urbantrend.Repository.PaymentRepository;
import com.devsam.urbantrend.Repository.ProductRepository;
import com.devsam.urbantrend.Repository.StockUnitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplStatusPaymentsTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private ProductRepository productRepository;
    @Mock private InventoryService inventoryService;
    @Mock private CustomerRepository customerRepository;
    @Mock private PayrollService payrollService;
    @Mock private CourierRepository courierRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private StockUnitRepository stockUnitRepository;

    private OrderServiceImpl service;
    private Order order;

    @BeforeEach
    void setUp() {
        service = new OrderServiceImpl(orderRepository, orderItemRepository, productRepository,
                inventoryService, customerRepository, payrollService, courierRepository,
                paymentRepository, stockUnitRepository);

        order = new Order();
        order.setId(UUID.randomUUID());
        order.setTotalAmount(new BigDecimal("1000"));

        lenient().when(orderRepository.lockById(order.getId())).thenReturn(Optional.of(order));
        lenient().when(orderRepository.findDetailedByIdAndDeletedAtIsNull(order.getId()))
                .thenReturn(Optional.of(order));
        lenient().when(orderRepository.save(any(Order.class))).thenAnswer(call -> call.getArgument(0));
    }

    private void orderIs(OrderStatus status, String paid) {
        order.setOrderStatus(status);
        order.setPaidAmount(new BigDecimal(paid));
    }

    @Test
    void aFullyPaidOrderCannotBeMarkedUnpaid() {
        orderIs(OrderStatus.DISPATCHED_PAID, "1000");

        assertThatThrownBy(() -> service.updateStatus(order.getId(), OrderStatus.DISPATCHED_UNPAID, new User()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("payments recorded");
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.DISPATCHED_PAID);
    }

    @Test
    void aDeliveredFullyPaidOrderCannotBeMarkedDeliveredUnpaid() {
        orderIs(OrderStatus.DELIVERED_PAID, "1000");

        assertThatThrownBy(() -> service.updateStatus(order.getId(), OrderStatus.DELIVERED_UNPAID, new User()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already fully paid");
    }

    @Test
    void partialNeedsARealPartPayment() {
        orderIs(OrderStatus.DISPATCHED_UNPAID, "0");

        assertThatThrownBy(() -> service.updateStatus(order.getId(), OrderStatus.DISPATCHED_PARTIAL, new User()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nothing has been paid");

        order.setPaidAmount(new BigDecimal("400"));
        service.updateStatus(order.getId(), OrderStatus.DISPATCHED_PARTIAL, new User());

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.DISPATCHED_PARTIAL);
    }

    @Test
    void anUnpaidOrderCannotBeCompleted() {
        orderIs(OrderStatus.DELIVERED_UNPAID, "0");

        assertThatThrownBy(() -> service.updateStatus(order.getId(), OrderStatus.COMPLETED, new User()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not fully paid");
    }

    @Test
    void aPaidOrderCanBeCompleted() {
        orderIs(OrderStatus.DELIVERED_PAID, "1000");

        service.updateStatus(order.getId(), OrderStatus.COMPLETED, new User());

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    void aPartPaidDispatchedOrderCanBeDeliveredUnpaid() {
        orderIs(OrderStatus.DISPATCHED_PARTIAL, "400");

        service.updateStatus(order.getId(), OrderStatus.DELIVERED_UNPAID, new User());

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.DELIVERED_UNPAID);
    }
}
