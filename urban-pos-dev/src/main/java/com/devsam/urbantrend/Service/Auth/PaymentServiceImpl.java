package com.devsam.urbantrend.Service.Auth;
import com.devsam.urbantrend.Dtos.PaymentRequest;
import com.devsam.urbantrend.Dtos.PaymentResponse;
import com.devsam.urbantrend.Entity.Order;
import com.devsam.urbantrend.Entity.Payment;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Enum.OrderStatus;
import com.devsam.urbantrend.Enum.PaymentMethod;
import com.devsam.urbantrend.Enum.PaymentStatus;
import com.devsam.urbantrend.Events.OrderPaidEvent;
import com.devsam.urbantrend.Errors.ResourceNotFoundException;
import com.devsam.urbantrend.Repository.OrderRepository;
import com.devsam.urbantrend.Repository.PaymentRepository;
import com.devsam.urbantrend.Roles.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public PaymentResponse recordPayment(PaymentRequest request, User user) {


        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }

        // Blank references ("") must not count as a reference, and M-Pesa/bank codes
        // are case-insensitive: "abc123 " and "ABC123" are the same transaction.
        String transactionRef = normalizeReference(request.getTransactionRef());

        if (transactionRef != null && paymentRepository.existsByTransactionRef(transactionRef)) {
            throw new IllegalArgumentException("Duplicate transaction reference");
        }

        Order order = null;

        if (request.getOrderId() != null) {
            order = orderRepository.lockById(request.getOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

            ensureOrderAcceptsPayment(order);
            ensureWithinBalance(order, request.getAmount());
        }

        Payment payment = Payment.builder()
                .order(order)
                .method(request.getMethod())
                .amount(request.getAmount())
                .currency("KES")
                .transactionRef(transactionRef)
                .payerPhone(request.getPayerPhone())
                .payerName(request.getPayerName())
                .notes(request.getNotes())
                .paymentDate(request.getPaymentDate() != null ? request.getPaymentDate() : OffsetDateTime.now())
                .status(PaymentStatus.PAID)
                .recordedBy(user.getId())
                .matched(order != null)
                .build();

        Payment saved = paymentRepository.save(payment);

        if (order != null) {
            applyPaymentToOrder(order, saved, user);
        }

        return toDto(saved);
    }

    @Override
    public Page<PaymentResponse> getAllPayments(Pageable pageable, String query, PaymentMethod method) {

        boolean hasQuery = query != null && !query.trim().isEmpty();
        String pattern = hasQuery ? "%" + query.trim().toLowerCase() + "%" : "%";

        Page<Payment> payments = (method == null && !hasQuery)
                ? paymentRepository.findAll(pageable)
                : paymentRepository.search(method, hasQuery, pattern, pageable);

        return payments.map(payment -> PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrder() != null ? payment.getOrder().getId() : null)
                .method(payment.getMethod())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .transactionRef(payment.getTransactionRef())
                .payerPhone(payment.getPayerPhone())
                .payerName(payment.getPayerName())
                .notes(payment.getNotes())
                .paymentDate(payment.getPaymentDate())
                .status(payment.getStatus())
                .matched(payment.isMatched())
                .suspicious(payment.isSuspicious())
                .createdAt(payment.getCreatedAt())
                .build()
        );
    }


    @Override
    public List<PaymentResponse> getPaymentsForOrder(UUID orderId) {
        return paymentRepository.findAllByOrder_Id(orderId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public Page<PaymentResponse> getUnmatchedPayments(Pageable pageable) {
        return paymentRepository.findAllByMatchedFalse(pageable)
                .map(this::toDto);
    }

    @Override
    public PaymentResponse matchPaymentToOrder(UUID paymentId, UUID orderId, User user) {


        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        Order order = orderRepository.lockById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (payment.isMatched() || payment.getOrder() != null) {
            throw new IllegalArgumentException("Payment is already matched to an order");
        }

        if (payment.isSuspicious()) {
            throw new IllegalArgumentException("Suspicious payments cannot be matched");
        }

        if (payment.getStatus() != PaymentStatus.SUCCESS
                && payment.getStatus() != PaymentStatus.PAID) {
            throw new IllegalArgumentException("Only successful payments can be matched");
        }

        ensureOrderAcceptsPayment(order);
        ensureWithinBalance(order, payment.getAmount());

        payment.setOrder(order);
        payment.setMatched(true);
        payment.setReconciledBy(user.getId());
        payment.setReconciledAt(OffsetDateTime.now());

        Payment saved = paymentRepository.save(payment);

        applyPaymentToOrder(order, saved, user);

        return toDto(saved);
    }

    private void ensureOrderAcceptsPayment(Order order) {
        OrderStatus status = order.getOrderStatus();

        if (status != null && !status.acceptsPayment()) {
            throw new IllegalArgumentException("Cannot apply a payment to a cancelled or returned order");
        }
    }

    // A recorded payment may not exceed what the order still owes. Previously the excess
    // was silently absorbed (balance clamped to 0) and paidAmount drifted above the total.
    private void ensureWithinBalance(Order order, BigDecimal amount) {
        BigDecimal paid = order.getPaidAmount() != null ? order.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal balance = order.getTotalAmount().subtract(paid).max(BigDecimal.ZERO);

        if (balance.signum() == 0) {
            throw new IllegalArgumentException("Order is already fully paid");
        }
        if (amount.compareTo(balance) > 0) {
            throw new IllegalArgumentException(
                    "Payment of " + amount + " exceeds the order balance of " + balance);
        }
    }

    private static String normalizeReference(String reference) {
        if (reference == null) {
            return null;
        }
        String cleaned = reference.trim().toUpperCase();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private void applyPaymentToOrder(Order order, Payment payment, User user) {

        BigDecimal total = order.getTotalAmount();
        BigDecimal paid = order.getPaidAmount() != null ? order.getPaidAmount() : BigDecimal.ZERO;

        boolean wasFullyPaid = paid.compareTo(total) >= 0;

        BigDecimal newPaid = paid.add(payment.getAmount());
        BigDecimal newBalance = total.subtract(newPaid).max(BigDecimal.ZERO);

        order.setPaidAmount(newPaid);
        order.setBalance(newBalance);
        orderRepository.save(order);

        OrderStatus current = order.getOrderStatus();
        OrderStatus target = current == null
                ? OrderStatus.AWAITING_DISPATCH
                : current.afterPayment(newBalance.compareTo(BigDecimal.ZERO) <= 0);

        // A payment never dispatches an order; only a real status change goes through OrderService.
        Order updated = target == current
                ? order
                : orderService.updateStatus(order.getId(), target, user);

        if (!wasFullyPaid && updated.getBalance() != null
                && updated.getBalance().compareTo(BigDecimal.ZERO) <= 0) {

            // Sent after the transaction commits (and never inside it), so a slow or
            // failing SMS gateway cannot hold locks or roll the payment back.
            eventPublisher.publishEvent(new OrderPaidEvent(order.getId()));
        }
    }

    private PaymentResponse toDto(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrder() != null ? payment.getOrder().getId() : null)
                .method(payment.getMethod())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .transactionRef(payment.getTransactionRef())
                .payerPhone(payment.getPayerPhone())
                .payerName(payment.getPayerName())
                .notes(payment.getNotes())
                .paymentDate(payment.getPaymentDate())
                .status(payment.getStatus())
                .matched(payment.isMatched())
                .suspicious(payment.isSuspicious())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
