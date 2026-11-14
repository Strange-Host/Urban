package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.PaymentRequest;
import com.devsam.urbantrend.Dtos.PaymentResponse;
import com.devsam.urbantrend.Entity.Order;
import com.devsam.urbantrend.Entity.Payment;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Enum.OrderStatus;
import com.devsam.urbantrend.Enum.PaymentMethod;
import com.devsam.urbantrend.Events.OrderPaidEvent;
import com.devsam.urbantrend.Repository.OrderRepository;
import com.devsam.urbantrend.Repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderService orderService;
    @Mock private ApplicationEventPublisher eventPublisher;

    private PaymentServiceImpl service;
    private User cashier;
    private Order order;

    @BeforeEach
    void setUp() {
        service = new PaymentServiceImpl(paymentRepository, orderRepository, orderService, eventPublisher);

        cashier = new User();
        cashier.setId(UUID.randomUUID());

        order = Order.builder()
                .customerId(UUID.randomUUID())
                .orderStatus(OrderStatus.AWAITING_DISPATCH)
                .totalAmount(new BigDecimal("1000.00"))
                .paidAmount(BigDecimal.ZERO)
                .balance(new BigDecimal("1000.00"))
                .build();
        order.setId(UUID.randomUUID());
    }

    private PaymentRequest request(String amount, String ref) {
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(order.getId());
        request.setMethod(PaymentMethod.MPESA);
        request.setAmount(new BigDecimal(amount));
        request.setTransactionRef(ref);
        return request;
    }

    @Test
    void paymentLargerThanTheBalanceIsRejected() {
        when(orderRepository.lockById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.recordPayment(request("1500.00", null), cashier))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds the order balance");

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void paymentOnAFullyPaidOrderIsRejected() {
        order.setPaidAmount(new BigDecimal("1000.00"));
        order.setBalance(BigDecimal.ZERO);
        when(orderRepository.lockById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.recordPayment(request("100.00", null), cashier))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already fully paid");
    }

    @Test
    void blankReferenceIsStoredAsNoReferenceAndNeverTreatedAsDuplicate() {
        when(orderRepository.lockById(order.getId())).thenReturn(Optional.of(order));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(call -> call.getArgument(0));

        PaymentResponse response = service.recordPayment(request("400.00", "   "), cashier);

        verify(paymentRepository, never()).existsByTransactionRef(any());
        assertThat(response.getTransactionRef()).isNull();
    }

    @Test
    void referencesAreComparedCaseInsensitively() {
        when(paymentRepository.existsByTransactionRef("ABC123")).thenReturn(true);

        assertThatThrownBy(() -> service.recordPayment(request("400.00", " abc123 "), cashier))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate transaction reference");
    }

    @Test
    void paymentThatClearsTheBalancePublishesThePaidEventAfterwards() {
        when(orderRepository.lockById(order.getId())).thenReturn(Optional.of(order));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(call -> call.getArgument(0));

        service.recordPayment(request("1000.00", "ref1"), cashier);

        assertThat(order.getPaidAmount()).isEqualByComparingTo("1000");
        ArgumentCaptor<OrderPaidEvent> event = ArgumentCaptor.forClass(OrderPaidEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().orderId()).isEqualTo(order.getId());
    }

    @Test
    void partialPaymentDoesNotPublishThePaidEvent() {
        when(orderRepository.lockById(order.getId())).thenReturn(Optional.of(order));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(call -> call.getArgument(0));

        service.recordPayment(request("400.00", "ref2"), cashier);

        assertThat(order.getBalance()).isEqualByComparingTo("600");
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }
}
