package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.*;
import com.devsam.urbantrend.Entity.*;
import com.devsam.urbantrend.Enum.OrderStatus;
import com.devsam.urbantrend.Enum.OrderType;
import com.devsam.urbantrend.Enum.PaymentStatus;
import com.devsam.urbantrend.Errors.ResourceNotFoundException;
import com.devsam.urbantrend.Repository.*;
import com.devsam.urbantrend.generator.OrderReferenceGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final CustomerRepository customerRepository;
    private final PayrollService payrollService;
    private final CourierRepository courierRepository;
    private final PaymentRepository paymentRepository;

    private static final List<OrderStatus> REVENUE_STATUSES =
            Arrays.stream(OrderStatus.values())
                    .filter(OrderStatus::isRevenueRecognized)
                    .toList();

    // Orders cannot predate the system, so a fixed start keeps "all time"
    // expressible as a bounded range the same query can use.
    private static final LocalDateTime ALL_TIME_START = LocalDateTime.of(2000, 1, 1, 0, 0);

    @Override
    @Transactional
    public Order create(OrderRequest req, User user) {

        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order must contain items");
        }
        if (req.getOrderType() == null) {
            throw new IllegalArgumentException("orderType is required (COUNTER or ONLINE)");
        }

        //  Validate discount
        BigDecimal discount = req.getDiscount() != null ? req.getDiscount() : BigDecimal.ZERO;
        if (discount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Discount cannot be negative");
        }

        BigDecimal deliveryFee = req.getDeliveryFee() != null ? req.getDeliveryFee() : BigDecimal.ZERO;
        if (deliveryFee.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Delivery fee cannot be negative");

        // Calculate raw total from line items
        BigDecimal rawTotal = BigDecimal.ZERO;

        for (OrderItemRequest itemReq : req.getItems()) {

            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

            if (itemReq.getQuantity() == null || itemReq.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Quantity must be > 0");
            }

            BigDecimal lineTotal = product.getSellingPrice().multiply(itemReq.getQuantity());
            rawTotal = rawTotal.add(lineTotal);
        }

        if (discount.compareTo(rawTotal) > 0) {
            throw new IllegalArgumentException("Discount cannot exceed order total of " + rawTotal);
        }


        BigDecimal totalAfterDiscount = rawTotal.subtract(discount);
        BigDecimal finalTotal = totalAfterDiscount.add(deliveryFee);

        // A counter sale is paid on the spot, so it must carry real payments that add up
        // to the total. Online orders get their payments later (KCB IPN / cashier).
        List<OrderPaymentRequest> payments = req.getPayments() == null ? List.of() : req.getPayments();
        if (req.getOrderType() == OrderType.COUNTER) {
            validateCounterPayments(payments, finalTotal);
        } else if (!payments.isEmpty()) {
            throw new IllegalArgumentException(
                    "Payments can only be recorded on counter sales; online orders are paid after they are created");
        }

        OrderStatus status = (req.getOrderType() == OrderType.COUNTER)
                ? OrderStatus.DISPATCHED_PAID
                : OrderStatus.AWAITING_DISPATCH;

        String reference;
        String fullAccount;
        do {
            reference = OrderReferenceGenerator.generate(5);
            fullAccount = "8025631#" + reference;
        } while (orderRepository.existsByAccountNumber(fullAccount));

        Order order = Order.builder()
                .customerId(req.getCustomerId())
                .assignedTo(req.getAssignedTo())
                .deliveryCity(req.getDeliveryCity())
                .deliveryNotes(req.getDeliveryNotes())
                .createdBy(user.getId())
                .orderType(req.getOrderType())
                .orderStatus(status)
                .paidAmount(status.isFullyPaid() ? finalTotal : BigDecimal.ZERO)
                .discount(discount)
                .deliveryFee(deliveryFee)
                .totalAmount(finalTotal)
                .balance(status.isFullyPaid() ? BigDecimal.ZERO : finalTotal)
                .accountNumber(fullAccount)
                .build();

        Order savedOrder = orderRepository.save(order);

        List<OrderItem> savedItems = new java.util.ArrayList<>();
        for (OrderItemRequest itemReq : req.getItems()) {

            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

            BigDecimal lineTotal = product.getSellingPrice().multiply(itemReq.getQuantity());

            OrderItem item = OrderItem.builder()
                    .order(savedOrder)
                    .product(product)
                    .quantity(itemReq.getQuantity())
                    .sellingPrice(product.getSellingPrice())
                    .costPrice(product.getCostPrice())
                    .lineTotal(lineTotal)
                    .build();

            savedItems.add(orderItemRepository.save(item));
        }
        savedOrder.setItems(savedItems);

        if (req.getOrderType() == OrderType.COUNTER) {
            for (OrderPaymentRequest paymentReq : payments) {
                paymentRepository.save(Payment.builder()
                        .order(savedOrder)
                        .method(paymentReq.getMethod())
                        .amount(paymentReq.getAmount())
                        .currency("KES")
                        .transactionRef(normalizeReference(paymentReq.getTransactionRef()))
                        .payerPhone(paymentReq.getPayerPhone())
                        .paymentDate(OffsetDateTime.now())
                        .status(PaymentStatus.PAID)
                        .recordedBy(user.getId())
                        .matched(true)
                        .notes("Counter sale")
                        .build());
            }
        }

        // Counter sales leave the shop immediately, so their stock has to move
        // now instead of waiting for a dispatch status transition.
        if (status.isFullyPaid()) {
            deductInventoryForOrder(savedOrder.getId());
        }

        return savedOrder;
    }


    private void validateCounterPayments(List<OrderPaymentRequest> payments, BigDecimal saleTotal) {
        if (payments.isEmpty()) {
            if (saleTotal.signum() == 0) {
                return;
            }
            throw new IllegalArgumentException("A counter sale needs at least one payment");
        }

        BigDecimal paidTotal = BigDecimal.ZERO;
        java.util.Set<String> seenReferences = new java.util.HashSet<>();

        for (OrderPaymentRequest payment : payments) {
            if (payment.getMethod() == null) {
                throw new IllegalArgumentException("Each payment needs a method");
            }
            if (payment.getAmount() == null || payment.getAmount().signum() <= 0) {
                throw new IllegalArgumentException("Each payment amount must be greater than 0");
            }
            paidTotal = paidTotal.add(payment.getAmount());

            String reference = normalizeReference(payment.getTransactionRef());
            if (reference != null
                    && (!seenReferences.add(reference) || paymentRepository.existsByTransactionRef(reference))) {
                throw new IllegalArgumentException("Duplicate transaction reference " + reference);
            }
        }

        if (paidTotal.compareTo(saleTotal) != 0) {
            throw new IllegalArgumentException(
                    "Payments total " + paidTotal + " but the sale total is " + saleTotal);
        }
    }

    private static String normalizeReference(String reference) {
        if (reference == null) {
            return null;
        }
        String cleaned = reference.trim().toUpperCase();
        return cleaned.isEmpty() ? null : cleaned;
    }

    @Override
    public Order getById(UUID id) {
        return orderRepository.findDetailedByIdAndDeletedAtIsNull(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found with id " + id)
                );
    }


    @Override
    @Transactional
    public Order updateStatus(UUID id, OrderStatus status, User user) {

        // Serialise concurrent status changes on the same order so stock is only
        // deducted or restored once.
        orderRepository.lockById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id " + id));

        Order order = getById(id);
        OrderStatus oldStatus = order.getOrderStatus();

        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        if (oldStatus != null && !oldStatus.canTransitionTo(status)) {
            throw new IllegalArgumentException(
                    "Cannot change order status from " + oldStatus + " to " + status);
        }
        if (oldStatus == status) {
            return order;
        }
        // A "paid" status must be backed by recorded payments, otherwise staff could
        // mark their own orders paid (which drives commission).
        if (status == OrderStatus.DISPATCHED_PAID || status == OrderStatus.DELIVERED_PAID) {
            BigDecimal paid = order.getPaidAmount() != null ? order.getPaidAmount() : BigDecimal.ZERO;
            if (paid.compareTo(order.getTotalAmount()) < 0) {
                throw new IllegalArgumentException(
                        "Order is not fully paid; record the payment before marking it " + status);
            }
        }

        order.setOrderStatus(status);
        Order saved = orderRepository.save(order);

        boolean isNowDispatched = isDispatched(status);
        boolean wasAlreadyDispatched = isDispatched(oldStatus);

        if (isNowDispatched && !wasAlreadyDispatched) {
            deductInventoryForOrder(saved.getId());
        }

        // Cancelling an order whose stock was already deducted puts it back.
        if (status == OrderStatus.CANCELLED && wasAlreadyDispatched) {
            addInventoryForOrder(saved.getId());
        }

        boolean isNowReturnedGood = status == OrderStatus.RETURNED_GOOD;
        boolean wasAlreadyReturnedGood = oldStatus == OrderStatus.RETURNED_GOOD;

        if (isNowReturnedGood && !wasAlreadyReturnedGood) {
            addInventoryForOrder(saved.getId());
        }

        return saved;
    }

    private boolean isDispatched(OrderStatus status) {
        return status == OrderStatus.DISPATCHED_PAID
                || status == OrderStatus.DISPATCHED_UNPAID
                || status == OrderStatus.DISPATCHED_PARTIAL;
    }

    // Inventory rows are locked one product at a time; always taking them in the same
    // (product id) order prevents two orders with overlapping products deadlocking.
    private List<OrderItem> itemsInLockOrder(UUID orderId) {
        List<OrderItem> items = new java.util.ArrayList<>(orderItemRepository.findByOrder_Id(orderId));
        items.sort(java.util.Comparator.comparing(item -> item.getProduct().getId()));
        return items;
    }

    private void addInventoryForOrder(UUID orderId) {

        List<OrderItem> items = itemsInLockOrder(orderId);

        for (OrderItem item : items) {
            InventoryRequest req = InventoryRequest.builder()
                    .productId(item.getProduct().getId())
                    .quantity(item.getQuantity())
                    .build();

            inventoryService.increaseIfTracked(req);
        }
    }

    private void deductInventoryForOrder(UUID orderId) {

        List<OrderItem> items = itemsInLockOrder(orderId);

        for (OrderItem item : items) {
            InventoryRequest req = InventoryRequest.builder()
                    .productId(item.getProduct().getId())
                    .quantity(item.getQuantity())
                    .build();

            inventoryService.reduceIfTracked(req);
        }
    }

    @Override
    public Page<OrderResponse> getAll(OrderStatus status, UUID staffId, UUID courierId, String query, Pageable pageable) {

        boolean hasQuery = query != null && !query.isBlank();
        String pattern = hasQuery ? "%" + query.trim().toLowerCase() + "%" : "%";

        Page<UUID> idPage = orderRepository.findAllIds(status, staffId, courierId, hasQuery, pattern, pageable);
        List<UUID> ids = idPage.getContent();

        if (ids.isEmpty()) {
            return Page.empty(pageable);
        }

        List<Order> orders = orderRepository.findAllWithItemsByIds(ids);

        Map<UUID, Order> orderMap = orders.stream()
                .collect(Collectors.toMap(Order::getId, o -> o));

        Map<UUID, Customer> customerMap = customerRepository
                .findAllById(orderMap.values().stream()
                        .map(Order::getCustomerId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(Customer::getId, c -> c));

        List<OrderResponse> responses = ids.stream()
                .map(orderMap::get)
                .filter(Objects::nonNull)
                .map(order -> {

                    String customerName = "-";
                    String phoneNumber = null;

                    if (order.getCustomerId() != null) {
                        Customer customer = customerMap.get(order.getCustomerId());
                        if (customer != null) {
                            customerName = customer.getName() != null ? customer.getName() : "-";
                            phoneNumber = customer.getPhone();
                        }
                    }

                    String productName = "-";
                    if (order.getItems() != null && !order.getItems().isEmpty()) {
                        String first = order.getItems().get(0).getProduct().getName();
                        int count = order.getItems().size();
                        productName = (count == 1)
                                ? first
                                : first + " + " + (count - 1) + " more";
                    }

                    List<OrderItemResponse> itemResponses = order.getItems() == null ? List.of() :
                            order.getItems().stream()
                                    .map(item -> OrderItemResponse.builder()
                                            .id(item.getId())
                                            .productId(item.getProduct().getId())
                                            .productName(item.getProduct().getName())
                                            .quantity(item.getQuantity())
                                            .sellingPrice(item.getSellingPrice())
                                            .build()
                                    ).toList();

                    return OrderResponse.builder()
                            .id(order.getId())
                            .customerId(order.getCustomerId())
                            .customerName(customerName)
                            .productName(productName)
                            .phoneNumber(phoneNumber)
                            .accountNumber(order.getAccountNumber())
                            .assignedTo(order.getAssignedTo())
                            .orderStatus(order.getOrderStatus())
                            .totalAmount(order.getTotalAmount())
                            .paidAmount(order.getPaidAmount())
                            .balance(order.getBalance())
                            .deliveryCity(order.getDeliveryCity())
                            .deliveryNotes(order.getDeliveryNotes())
                            .invoicePdfUrl(order.getInvoicePdfUrl())
                            .createdBy(order.getCreatedBy())
                            .createdAt(order.getCreatedAt())
                            .updatedAt(order.getUpdatedAt())
                            .items(itemResponses)
                            .build();
                })
                .toList();

        return new PageImpl<>(responses, pageable, idPage.getTotalElements());
    }

    @Override
    public BigDecimal getOrderRevenue(UUID orderId) {
        List<OrderItem> items = orderItemRepository.findByOrder_Id(orderId);

        return items.stream()
                .map(i -> i.getSellingPrice().multiply(i.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal getOrderCogs(UUID orderId) {
        List<OrderItem> items = orderItemRepository.findByOrder_Id(orderId);

        return items.stream()
                .map(i -> i.getCostPrice().multiply(i.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal getOrderGrossProfit(UUID orderId) {
        return getOrderRevenue(orderId).subtract(getOrderCogs(orderId));
    }


    @Override
    public List<TopSellingProductResponse> getTopSellingProducts(
            LocalDate from,
            LocalDate to,
            int limit) {

        if (from == null || to == null) {
            throw new IllegalArgumentException("from and to dates are required");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from date cannot be after to date");
        }
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("limit must be between 1 and 100");
        }

        List<TopSellingProductProjection> results =
                orderRepository.findTopSellingProducts(
                        from.atStartOfDay(),
                        to.atTime(23, 59, 59),
                        limit
                );

        return results.stream()
                .map(r -> new TopSellingProductResponse(
                        r.getProductId(),
                        r.getProductName(),
                        r.getTotalQuantity()
                ))
                .toList();
    }

    @Override
    public StaffOrderSummary getStaffSummary(UUID staffId) {

        LocalDate today = LocalDate.now();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime upper = LocalDateTime.now().plusDays(1);

        int totalOrders = Math.toIntExact(
                orderRepository.countByCreatedByAndDeletedAtIsNull(staffId));

        int ordersToday = Math.toIntExact(
                orderRepository.countByCreatedByAndCreatedAtGreaterThanEqualAndDeletedAtIsNull(staffId, dayStart));

        BigDecimal revenueToday =
                orderRepository.sumStaffRevenue(staffId, REVENUE_STATUSES, dayStart, upper);
        BigDecimal cogsToday =
                orderItemRepository.sumStaffCogs(staffId, REVENUE_STATUSES, dayStart, upper);

        BigDecimal profitToday = revenueToday.subtract(cogsToday);

        // Commission is driven by the real weekly-KPI rule (50/30 bob per
        // fully-paid order depending on that week's KPI), not a flat
        // percentage of revenue. Reuse PayrollService so there's one source
        // of truth instead of two disagreeing commission numbers.
        BigDecimal commissionToday = payrollService
                .getStaffPayroll(staffId, today, today)
                .getCommission();

        BigDecimal totalRevenue =
                orderRepository.sumStaffRevenue(staffId, REVENUE_STATUSES, ALL_TIME_START, upper);
        BigDecimal totalCogs =
                orderItemRepository.sumStaffCogs(staffId, REVENUE_STATUSES, ALL_TIME_START, upper);

        BigDecimal totalProfit = totalRevenue.subtract(totalCogs);

        LocalDateTime firstOrderAt = orderRepository.findEarliestOrderCreatedAt(staffId);
        LocalDate earliestOrderDate = firstOrderAt != null ? firstOrderAt.toLocalDate() : today;

        BigDecimal totalCommission = earliestOrderDate.equals(today)
                ? commissionToday
                : payrollService.getStaffPayroll(staffId, earliestOrderDate, today).getCommission();

        long revenueOrders =
                orderRepository.countStaffRevenueOrders(staffId, REVENUE_STATUSES);

        BigDecimal averageOrderValue = revenueOrders == 0
                ? BigDecimal.ZERO
                : totalRevenue.divide(BigDecimal.valueOf(revenueOrders), 2, RoundingMode.HALF_UP);

        return StaffOrderSummary.builder()
                .ordersToday(ordersToday)
                .revenueToday(revenueToday)
                .profitToday(profitToday)
                .commissionToday(commissionToday)
                .totalOrders(totalOrders)
                .totalRevenue(totalRevenue)
                .totalProfit(totalProfit)
                .totalCommission(totalCommission)
                .averageOrderValue(averageOrderValue)
                .build();
    }

    public Order updateNotes(UUID id, UpdateOrderNotesRequest request, User user) {
        Order order = getById(id);

        if (request.getDeliveryNotes() == null || request.getDeliveryNotes().isBlank()) {
            throw new RuntimeException("Delivery notes cannot be empty");
        }

        order.setDeliveryNotes(request.getDeliveryNotes());

        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public Order assignCourier(UUID id, AssignCourierRequest request, User user) {
        Order order = getById(id);

        UUID courierId = request.getCourierId();

        if (courierId == null) {
            order.setCourierId(null);
            order.setCourierAssignedAt(null);
            return orderRepository.save(order);
        }

        Courier courier = courierRepository.findById(courierId)
                .orElseThrow(() -> new ResourceNotFoundException("Courier not found with id " + courierId));

        if (!courier.isActive()) {
            throw new IllegalArgumentException("Courier is inactive and cannot be assigned");
        }

        order.setCourierId(courier.getId());
        order.setCourierAssignedAt(LocalDateTime.now());

        return orderRepository.save(order);
    }

}