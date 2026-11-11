package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.*;
import com.devsam.urbantrend.Entity.*;
import com.devsam.urbantrend.Enum.OrderStatus;
import com.devsam.urbantrend.Enum.OrderType;
import com.devsam.urbantrend.Errors.ResourceNotFoundException;
import com.devsam.urbantrend.Repository.*;
import com.devsam.urbantrend.generator.OrderReferenceGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
                .paidAmount(BigDecimal.ZERO)
                .discount(discount)
                .deliveryFee(deliveryFee)
                .totalAmount(finalTotal)
                .balance(totalAfterDiscount)
                .accountNumber(fullAccount)
                .build();

        Order savedOrder = orderRepository.save(order);

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

            orderItemRepository.save(item);
        }

        return savedOrder;
    }


    @Override
    public Order getById(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found with id " + id)
                );
    }


    @Override
    @Transactional
    @CachePut(value = "ORDERS_CACHE", key = "#result.id")
    public Order updateStatus(UUID id, OrderStatus status, User user) {

        Order order = getById(id);
        OrderStatus oldStatus = order.getOrderStatus();

        order.setOrderStatus(status);
        Order saved = orderRepository.save(order);

        boolean isNowDispatched = isDispatched(status);
        boolean wasAlreadyDispatched = isDispatched(oldStatus);

        if (isNowDispatched && !wasAlreadyDispatched) {
            deductInventoryForOrder(saved.getId());
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

    private void addInventoryForOrder(UUID orderId) {

        List<OrderItem> items = orderItemRepository.findByOrder_Id(orderId);

        for (OrderItem item : items) {
            InventoryRequest req = InventoryRequest.builder()
                    .productId(item.getProduct().getId())
                    .quantity(item.getQuantity())
                    .build();

            inventoryService.increase(req);
        }
    }

    private void deductInventoryForOrder(UUID orderId) {

        List<OrderItem> items = orderItemRepository.findByOrder_Id(orderId);

        for (OrderItem item : items) {
            InventoryRequest req = InventoryRequest.builder()
                    .productId(item.getProduct().getId())
                    .quantity(item.getQuantity())
                    .build();

            inventoryService.reduce(req);
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

        List<OrderResponse> responses = ids.stream()
                .map(orderMap::get)
                .filter(Objects::nonNull)
                .map(order -> {

                    String customerName = "-";
                    Long phoneNumber = null;

                    if (order.getCustomerId() != null) {
                        var customer = customerRepository.findById(order.getCustomerId()).orElse(null);
                        if (customer != null) {
                            customerName = customer.getName() != null ? customer.getName() : "-";
                            phoneNumber = customer.getPhone() != null
                                    ? Long.valueOf(customer.getPhone())
                                    : null;
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
                                            .costPrice(item.getCostPrice())
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

        List<Order> orders = orderRepository.findByCreatedBy(staffId);

        LocalDate today = LocalDate.now();

        List<Order> todaysOrders = orders.stream()
                .filter(o -> o.getCreatedAt() != null &&
                        o.getCreatedAt().toLocalDate().equals(today))
                .toList();

        BigDecimal revenueToday = todaysOrders.stream()
                .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal cogsToday = todaysOrders.stream()
                .map(o -> o.getItems() == null ? BigDecimal.ZERO :
                        o.getItems().stream()
                                .map(i -> i.getCostPrice().multiply(i.getQuantity()))
                                .reduce(BigDecimal.ZERO, BigDecimal::add))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal profitToday = revenueToday.subtract(cogsToday);

        // Commission is driven by the real weekly-KPI rule (50/30 bob per
        // DELIVERED_PAID order depending on that week's KPI), not a flat
        // percentage of revenue. Reuse PayrollService so there's one source
        // of truth instead of two disagreeing commission numbers.
        BigDecimal commissionToday = payrollService
                .getStaffPayroll(staffId, today, today)
                .getCommission();

        int totalOrders = orders.size();

        BigDecimal totalRevenue = orders.stream()
                .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCogs = orders.stream()
                .map(o -> o.getItems() == null ? BigDecimal.ZERO :
                        o.getItems().stream()
                                .map(i -> i.getCostPrice().multiply(i.getQuantity()))
                                .reduce(BigDecimal.ZERO, BigDecimal::add))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalProfit = totalRevenue.subtract(totalCogs);

        LocalDate earliestOrderDate = orders.stream()
                .map(o -> o.getCreatedAt() != null ? o.getCreatedAt().toLocalDate() : today)
                .min(LocalDate::compareTo)
                .orElse(today);

        BigDecimal totalCommission = payrollService
                .getStaffPayroll(staffId, earliestOrderDate, today)
                .getCommission();

        BigDecimal averageOrderValue = totalOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return StaffOrderSummary.builder()
                .ordersToday(todaysOrders.size())
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
    @CachePut(value = "ORDERS_CACHE", key = "#result.id")
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