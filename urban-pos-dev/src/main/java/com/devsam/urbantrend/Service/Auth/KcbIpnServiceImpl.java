package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.KcbIpnRequest;
import com.devsam.urbantrend.Entity.Order;
import com.devsam.urbantrend.Entity.Payment;
import com.devsam.urbantrend.Enum.OrderStatus;
import com.devsam.urbantrend.Enum.PaymentMethod;
import com.devsam.urbantrend.Enum.PaymentStatus;
import com.devsam.urbantrend.Events.OrderPaidEvent;
import com.devsam.urbantrend.Repository.OrderRepository;
import com.devsam.urbantrend.Repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class KcbIpnServiceImpl implements KcbIpnService {

    private static final Logger log = LoggerFactory.getLogger(KcbIpnServiceImpl.class);

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional
    public void handleIpn(KcbIpnRequest request) {

        log.info("🔔 KCB IPN received");

        var notification = request.getRequestPayload()
                .getAdditionalData()
                .getNotificationData();

        String accountNumber = notification.getBusinessKey();
        String transactionRef = notification.getTransactionID();
        BigDecimal amount = new BigDecimal(notification.getTransactionAmt());

        log.info("BusinessKey: {}", accountNumber);
        log.info("TransactionRef: {}", transactionRef);
        log.info("Amount: {}", amount);
        log.info("Phone: {}", notification.getDebitMSISDN());

        if (paymentRepository.findByTransactionRef(transactionRef).isPresent()) {
            log.warn("⚠ Duplicate IPN ignored for transactionRef={}", transactionRef);
            return;
        }

        String payerName = buildPayerName(
                notification.getFirstName(),
                notification.getMiddleName(),
                notification.getLastName()
        );

        // Try finding the order
        Order order = orderRepository.findByAccountNumber(accountNumber)
                .orElse(null);

        if (order == null) {
            log.warn("⚠ No order found for account number={}", accountNumber);
        } else {
            log.info("✅ Order found: id={}", order.getId());
        }

        // Save payment whether matched or unmatched
        Payment payment = Payment.builder()
                .order(order) // can be null
                .method(PaymentMethod.BANK)
                .amount(amount)
                .currency(notification.getCurrency() == null ? "KES" : notification.getCurrency())
                .transactionRef(transactionRef)
                .payerPhone(notification.getDebitMSISDN())
                .payerName(payerName)
                .paymentDate(OffsetDateTime.now())
                .status(PaymentStatus.SUCCESS)
                .matched(order != null)
                .notes(
                        order == null
                                ? "UNMATCHED KCB IPN: " + notification.getNarration()
                                : "KCB IPN: " + notification.getNarration()
                )
                .build();

        paymentRepository.save(payment);

        log.info("💰 Payment saved successfully transactionRef={}", transactionRef);

        // Stop here if no order matched
        if (order == null) {
            log.warn("🏁 Payment stored as unmatched transactionRef={}", transactionRef);
            return;
        }

        BigDecimal oldPaid = order.getPaidAmount() == null
                ? BigDecimal.ZERO
                : order.getPaidAmount();

        BigDecimal newPaid = oldPaid.add(amount);

        order.setPaidAmount(newPaid);

        BigDecimal total = order.getTotalAmount();

        if (newPaid.compareTo(total) >= 0) {

            order.setOrderStatus(OrderStatus.DISPATCHED_PAID);

            log.info("🟢 Order fully paid. Status updated to DISPATCHED_PAID");

            applicationEventPublisher.publishEvent(
                    new OrderPaidEvent(order.getId())
            );

        } else {

            order.setOrderStatus(OrderStatus.DISPATCHED_PARTIAL);

            log.info("🟡 Partial payment received. Status updated to DISPATCHED_PARTIAL");
        }

        orderRepository.save(order);

        log.info("🏁 KCB IPN processing completed for transactionRef={}", transactionRef);
    }

    private String buildPayerName(String first, String middle, String last) {
        String f = first == null ? "" : first.trim();
        String m = middle == null ? "" : middle.trim();
        String l = last == null ? "" : last.trim();
        return (f + " " + m + " " + l).replaceAll("\\s+", " ").trim();
    }
}