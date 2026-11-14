package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.OrderItemRequest;
import com.devsam.urbantrend.Dtos.OrderPaymentRequest;
import com.devsam.urbantrend.Dtos.OrderRequest;
import com.devsam.urbantrend.Entity.Order;
import com.devsam.urbantrend.Entity.Payment;
import com.devsam.urbantrend.Entity.Product;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Enum.OrderStatus;
import com.devsam.urbantrend.Enum.OrderType;
import com.devsam.urbantrend.Enum.PaymentMethod;
import com.devsam.urbantrend.Repository.CourierRepository;
import com.devsam.urbantrend.Repository.CustomerRepository;
import com.devsam.urbantrend.Repository.OrderItemRepository;
import com.devsam.urbantrend.Repository.OrderRepository;
import com.devsam.urbantrend.Repository.PaymentRepository;
import com.devsam.urbantrend.Repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplCounterSaleTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private ProductRepository productRepository;
    @Mock private InventoryService inventoryService;
    @Mock private CustomerRepository customerRepository;
    @Mock private PayrollService payrollService;
    @Mock private CourierRepository courierRepository;
    @Mock private PaymentRepository paymentRepository;

    private OrderServiceImpl service;
    private User cashier;
    private UUID productId;

    @BeforeEach
    void setUp() {
        service = new OrderServiceImpl(orderRepository, orderItemRepository, productRepository,
                inventoryService, customerRepository, payrollService, courierRepository, paymentRepository);

        cashier = new User();
        cashier.setId(UUID.randomUUID());

        Product product = Product.builder()
                .sellingPrice(new BigDecimal("500.00"))
                .costPrice(new BigDecimal("300.00"))
                .build();
        productId = UUID.randomUUID();
        product.setId(productId);

        lenient().when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        lenient().when(orderRepository.save(any(Order.class))).thenAnswer(call -> {
            Order saved = call.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });
        lenient().when(orderItemRepository.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    private OrderPaymentRequest tender(PaymentMethod method, String amount, String ref) {
        return OrderPaymentRequest.builder()
                .method(method)
                .amount(new BigDecimal(amount))
                .transactionRef(ref)
                .build();
    }

    /** Two units at 500.00 = 1000.00. */
    private OrderRequest sale(OrderType type, List<OrderPaymentRequest> payments) {
        return OrderRequest.builder()
                .customerId(UUID.randomUUID())
                .orderType(type)
                .items(List.of(new OrderItemRequest(productId, new BigDecimal("2"))))
                .payments(payments)
                .build();
    }

    @Test
    void counterSaleWithoutPaymentsIsRejected() {
        assertThatThrownBy(() -> service.create(sale(OrderType.COUNTER, null), cashier))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one payment");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void paymentsThatDoNotAddUpToTheTotalAreRejected() {
        OrderRequest request = sale(OrderType.COUNTER, List.of(tender(PaymentMethod.CASH, "900.00", null)));

        assertThatThrownBy(() -> service.create(request, cashier))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Payments total 900.00 but the sale total is 1000.00");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void splitPaymentsAreRecordedAsRealPaymentRows() {
        OrderRequest request = sale(OrderType.COUNTER, List.of(
                tender(PaymentMethod.CASH, "300.00", null),
                tender(PaymentMethod.MPESA, "700.00", " ab12 ")));

        Order order = service.create(request, cashier);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.DISPATCHED_PAID);
        assertThat(order.getPaidAmount()).isEqualByComparingTo("1000");

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(2)).save(captor.capture());

        List<Payment> saved = captor.getAllValues();
        assertThat(saved).extracting(Payment::getMethod)
                .containsExactly(PaymentMethod.CASH, PaymentMethod.MPESA);
        assertThat(saved).extracting(Payment::getTransactionRef).containsExactly(null, "AB12");
        assertThat(saved).allSatisfy(payment -> {
            assertThat(payment.isMatched()).isTrue();
            assertThat(payment.getOrder()).isSameAs(order);
            assertThat(payment.getRecordedBy()).isEqualTo(cashier.getId());
        });
    }

    @Test
    void duplicateReferencesWithinOneSaleAreRejected() {
        OrderRequest request = sale(OrderType.COUNTER, List.of(
                tender(PaymentMethod.MPESA, "500.00", "QWE123"),
                tender(PaymentMethod.MPESA, "500.00", "qwe123")));

        assertThatThrownBy(() -> service.create(request, cashier))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate transaction reference");
    }

    @Test
    void referenceAlreadyUsedByAnotherPaymentIsRejected() {
        when(paymentRepository.existsByTransactionRef("QWE123")).thenReturn(true);
        OrderRequest request = sale(OrderType.COUNTER,
                List.of(tender(PaymentMethod.MPESA, "1000.00", "qwe123")));

        assertThatThrownBy(() -> service.create(request, cashier))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate transaction reference");
    }

    @Test
    void onlineOrdersCannotCarryPaymentsAtCreation() {
        OrderRequest request = sale(OrderType.ONLINE, List.of(tender(PaymentMethod.CASH, "1000.00", null)));

        assertThatThrownBy(() -> service.create(request, cashier))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("only be recorded on counter sales");
    }

    @Test
    void onlineOrderWithoutPaymentsStillWaitsForDispatch() {
        Order order = service.create(sale(OrderType.ONLINE, null), cashier);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.AWAITING_DISPATCH);
        assertThat(order.getPaidAmount()).isEqualByComparingTo("0");
        verify(paymentRepository, never()).save(any());
    }
}
