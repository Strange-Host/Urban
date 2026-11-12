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

        if (request == null || request.getRequestPayload() == null
                || request.getRequestPayload().getAdditionalData() == null
                || request.getRequestPayload().getAdditionalData().getNotificationData() == null) {
            throw new IllegalArgumentException("Malformed KCB IPN payload");
        }

        var notification = request.getRequestPayload()
                .getAdditionalData()
                .getNotificationData();

        String accountNumber = notification.getBusinessKey();
        String transactionRef = notification.getTransactionID();

        if (transactionRef == null || transactionRef.isBlank()) {
            throw new IllegalArgumentException("KCB IPN is missing transactionID");
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(notification.getTransactionAmt());
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("KCB IPN has an invalid transactionAmt");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("KCB IPN amount must be greater than zero");
        }

        String currency = notification.getCurrency() == null || notification.getCurrency().isBlank()
                ? "KES"
                : notification.getCurrency().trim().toUpperCase();

        boolean supportedCurrency = "KES".equals(currency);

        log.info("BusinessKey: {}", accountNumber);
        log.info("TransactionRef: {}", transactionRef);
        log.info("Amount: {} {}", amount, currency);
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
        Order order = null;

        if (supportedCurrency && accountNumber != null && !accountNumber.isBlank()) {
            order = orderRepository.findByAccountNumber(accountNumber)
                    .orElse(null);
        }

        if (!supportedCurrency) {
            log.warn("⚠ Unsupported KCB currency {}, payment stored as unmatched", currency);
        } else if (order == null) {
            log.warn("⚠ No order found for account number={}", accountNumber);
        } else {
            log.info("✅ Order found: id={}", order.getId());
        }

        // Save payment whether matched or unmatched
        Payment payment = Payment.builder()
                .order(order) // can be null
                .method(PaymentMethod.BANK)
                .amount(amount)
                .currency(currency)
                .transactionRef(transactionRef)
                .payerPhone(notification.getDebitMSISDN())
                .payerName(payerName)
                .paymentDate(OffsetDateTime.now())
                .status(PaymentStatus.SUCCESS)
                .matched(order != null)
                .suspicious(!supportedCurrency)
                .notes(
                        !supportedCurrency
                                ? "UNMATCHED KCB IPN: unsupported currency " + currency
                                : order == null
                                    ? "UNMATCHED KCB IPN: " + notification.getNarration()
                                    : "KCB IPN: " + notification.getNarration()
                )
                .build();

        paymentRepository.save(payment);

        log.info("💰 Payment saved successfully transactionRef={}", transactionRef);

        // Stop here if no order matched
        if (!supportedCurrency || order == null) {
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