package com.devsam.urbantrend.Service.Auth;
import com.devsam.urbantrend.Dtos.PaymentRequest;
import com.devsam.urbantrend.Dtos.PaymentResponse;
import com.devsam.urbantrend.Entity.Customer;
import com.devsam.urbantrend.Entity.Order;
import com.devsam.urbantrend.Entity.Payment;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Enum.OrderStatus;
import com.devsam.urbantrend.Enum.PaymentStatus;
import com.devsam.urbantrend.Errors.ResourceNotFoundException;
import com.devsam.urbantrend.Repository.CustomerRepository;
import com.devsam.urbantrend.Repository.OrderRepository;
import com.devsam.urbantrend.Repository.PaymentRepository;
import com.devsam.urbantrend.Roles.UserRole;
import com.devsam.urbantrend.Service.Auth.Messaging.SmsProvider;
import lombok.RequiredArgsConstructor;
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
    private final CustomerRepository customerRepository;
    private final OrderService orderService;
    private final SmsProvider smsProvider;

    @Override
    public PaymentResponse recordPayment(PaymentRequest request, User user) {

        if (user.getRole() != UserRole.ADMIN &&
                user.getRole() != UserRole.MANAGER &&
                user.getRole() != UserRole.CASHIER) {
            throw new AccessDeniedException("Not allowed to record payments");
        }

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }

        if (request.getTransactionRef() != null &&
                paymentRepository.existsByTransactionRef(request.getTransactionRef())) {
            throw new IllegalArgumentException("Duplicate transaction reference");
        }

        Order order = null;

        if (request.getOrderId() != null) {
            order = orderRepository.findById(request.getOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

            ensureOrderAcceptsPayment(order);
        }

        Payment payment = Payment.builder()
                .order(order)
                .method(request.getMethod())
                .amount(request.getAmount())
                .currency("KES")
                .transactionRef(request.getTransactionRef())
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
    public Page<PaymentResponse> getAllPayments(Pageable pageable) {

        Page<Payment> payments = paymentRepository.findAll(pageable);

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

        if (user.getRole() != UserRole.ADMIN && user.getRole() != UserRole.MANAGER) {
            throw new AccessDeniedException("Only ADMIN or MANAGER can reconcile payments");
        }

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        Order order = orderRepository.findById(orderId)
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

        if (status == OrderStatus.CANCELLED
                || (status != null && status.name().startsWith("RETURN"))) {
            throw new IllegalArgumentException("Cannot apply a payment to a cancelled or returned order");
        }
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
        OrderStatus target;

        if (current != null && current.isDeliveredOrClosed()) {
            target = current;
        } else {
            target = newBalance.compareTo(BigDecimal.ZERO) <= 0
                    ? OrderStatus.DISPATCHED_PAID
                    : OrderStatus.DISPATCHED_PARTIAL;
        }

        Order updated = orderService.updateStatus(order.getId(), target, user);

        if (!wasFullyPaid && updated.getBalance() != null
                && updated.getBalance().compareTo(BigDecimal.ZERO) <= 0) {

            Customer customer = customerRepository.findById(order.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

            smsProvider.sendSms(
                    customer.getPhone(),
                    "URBAN TRENDS: Hi " + customer.getName() +
                            ", payment of KES " + payment.getAmount() +
                            " received. Thank you for shopping with us."
            );
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
