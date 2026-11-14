package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.KcbIpnRequest;
import com.devsam.urbantrend.Entity.Order;
import com.devsam.urbantrend.Entity.Payment;
import com.devsam.urbantrend.Enum.OrderStatus;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KcbIpnServiceImplTest {

    private static final String ACCOUNT = "8025631#ABCDE";

    @Mock private OrderRepository orderRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private OrderService orderService;
    @Mock private ApplicationEventPublisher eventPublisher;

    private KcbIpnServiceImpl service;
    private Order order;

    @BeforeEach
    void setUp() {
        service = new KcbIpnServiceImpl(orderRepository, paymentRepository, orderService, eventPublisher);

        order = Order.builder()
                .customerId(UUID.randomUUID())
                .orderStatus(OrderStatus.AWAITING_DISPATCH)
                .totalAmount(new BigDecimal("1000.00"))
                .paidAmount(BigDecimal.ZERO)
                .balance(new BigDecimal("1000.00"))
                .accountNumber(ACCOUNT)
                .build();
        order.setId(UUID.randomUUID());
    }

    private KcbIpnRequest ipn(String businessKey, String amount, String txId) {
        KcbIpnRequest.NotificationData data = new KcbIpnRequest.NotificationData();
        data.setBusinessKey(businessKey);
        data.setTransactionAmt(amount);
        data.setTransactionID(txId);
        data.setCurrency("KES");
        data.setNarration("test payment");

        KcbIpnRequest.AdditionalData additional = new KcbIpnRequest.AdditionalData();
        additional.setNotificationData(data);

        KcbIpnRequest.RequestPayload payload = new KcbIpnRequest.RequestPayload();
        payload.setAdditionalData(additional);

        KcbIpnRequest request = new KcbIpnRequest();
        request.setRequestPayload(payload);
        return request;
    }

    private Payment savedPayment() {
        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void accountNumberTypedInLowerCaseWithSpacesStillMatches() {
        when(orderRepository.lockByAccountNumber(ACCOUNT)).thenReturn(Optional.of(order));

        service.handleIpn(ipn("  8025631#abcde ", "1000.00", "TX1"));

        Payment payment = savedPayment();
        assertThat(payment.isMatched()).isTrue();
        assertThat(payment.getOrder()).isSameAs(order);
        verify(eventPublisher).publishEvent(any(Object.class));
    }

    @Test
    void transactionIdsAreNormalisedBeforeTheDuplicateCheck() {
        when(paymentRepository.findByTransactionRef("TX1")).thenReturn(Optional.of(new Payment()));

        service.handleIpn(ipn(ACCOUNT, "100.00", " tx1 "));

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void paymentForAReturnedOrderIsParkedAsUnmatched() {
        order.setOrderStatus(OrderStatus.RETURN_IN_TRANSIT);
        when(orderRepository.lockByAccountNumber(ACCOUNT)).thenReturn(Optional.of(order));

        service.handleIpn(ipn(ACCOUNT, "500.00", "TX2"));

        Payment payment = savedPayment();
        assertThat(payment.isMatched()).isFalse();
        assertThat(payment.getOrder()).isNull();
        assertThat(payment.getNotes()).contains("UNMATCHED").contains("RETURN_IN_TRANSIT");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void paymentForAnAlreadyPaidOrderIsParkedInsteadOfInflatingPaidAmount() {
        order.setPaidAmount(new BigDecimal("1000.00"));
        order.setBalance(BigDecimal.ZERO);
        when(orderRepository.lockByAccountNumber(ACCOUNT)).thenReturn(Optional.of(order));

        service.handleIpn(ipn(ACCOUNT, "200.00", "TX3"));

        Payment payment = savedPayment();
        assertThat(payment.isMatched()).isFalse();
        assertThat(payment.getNotes()).contains("already fully paid");
        assertThat(order.getPaidAmount()).isEqualByComparingTo("1000");
    }

    @Test
    void overpaymentIsAppliedButFlaggedForReview() {
        when(orderRepository.lockByAccountNumber(ACCOUNT)).thenReturn(Optional.of(order));

        service.handleIpn(ipn(ACCOUNT, "1300.00", "TX4"));

        Payment payment = savedPayment();
        assertThat(payment.isMatched()).isTrue();
        assertThat(payment.isSuspicious()).isTrue();
        assertThat(payment.getNotes()).contains("OVERPAYMENT").contains("1300.00").contains("1000.00");
        assertThat(order.getBalance()).isEqualByComparingTo("0");
    }
}
