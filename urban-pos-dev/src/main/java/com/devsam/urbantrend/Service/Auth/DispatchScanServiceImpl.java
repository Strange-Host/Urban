package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.ScanRequest;
import com.devsam.urbantrend.Dtos.ScanResponse;
import com.devsam.urbantrend.Entity.Order;
import com.devsam.urbantrend.Entity.Product;
import com.devsam.urbantrend.Entity.StockUnit;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Enum.OrderStatus;
import com.devsam.urbantrend.Enum.StockUnitStatus;
import com.devsam.urbantrend.Errors.ResourceNotFoundException;
import com.devsam.urbantrend.Repository.OrderRepository;
import com.devsam.urbantrend.Repository.StockUnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DispatchScanServiceImpl implements DispatchScanService {

    private final StockUnitRepository stockUnitRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;

    @Override
    @Transactional
    public ScanResponse scan(ScanRequest request, User user) {
        StockUnit unit = findUnit(request.getBarcode());

        if (unit.getStatus() != StockUnitStatus.IN_STOCK) {
            String orderRef = unit.getOrderId() == null ? "" : orderRepository.findById(unit.getOrderId())
                    .map(o -> " for order " + o.getAccountNumber())
                    .orElse("");
            throw new IllegalArgumentException("Sticker " + unit.getSerial() + " was already scanned" + orderRef);
        }

        Product product = unit.getProduct();
        Order order = request.getOrderId() != null
                ? requestedOrder(request.getOrderId(), product)
                : oldestOrderNeeding(product);

        unit.setOrderId(order.getId());
        unit.setStatus(StockUnitStatus.PACKED);
        unit.setScannedAt(LocalDateTime.now());
        unit.setScannedBy(user.getId());
        stockUnitRepository.saveAndFlush(unit);

        long scanned = stockUnitRepository.countByOrderId(order.getId());
        long required = order.getItems().stream()
                .mapToLong(item -> item.getQuantity().longValue())
                .sum();

        boolean complete = order.getItems().stream()
                .allMatch(item -> remainingForProduct(order, item.getProduct()) <= 0);
        OrderStatus status = order.getOrderStatus();

        if (complete) {
            status = dispatchStatus(order);
            // updateStatus also deducts inventory for the order
            orderService.updateStatus(order.getId(), status, user);
            markDispatched(order.getId());
        }

        return ScanResponse.builder()
                .serial(unit.getSerial())
                .productName(product.getName())
                .orderId(order.getId())
                .accountNumber(order.getAccountNumber())
                .orderStatus(status)
                .unitsScanned(scanned)
                .unitsRequired(required)
                .orderDispatched(complete)
                .message(complete
                        ? "Order " + order.getAccountNumber() + " fully packed and marked " + status
                        : "Packed " + scanned + " of " + required + " units for order " + order.getAccountNumber())
                .build();
    }

    @Override
    @Transactional
    public void unscan(String barcode) {
        StockUnit unit = findUnit(barcode);
        if (unit.getStatus() != StockUnitStatus.PACKED) {
            throw new IllegalArgumentException("Only stickers scanned for an order that has not been dispatched can be undone");
        }
        unit.setStatus(StockUnitStatus.IN_STOCK);
        unit.setOrderId(null);
        unit.setScannedAt(null);
        unit.setScannedBy(null);
        stockUnitRepository.save(unit);
    }

    private StockUnit findUnit(String barcode) {
        if (barcode == null || barcode.isBlank()) {
            throw new IllegalArgumentException("barcode is required");
        }
        return stockUnitRepository.findBySerial(barcode.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("No sticker found for barcode " + barcode.trim()));
    }

    private Order requestedOrder(UUID orderId, Product product) {
        Order order = orderService.getById(orderId);
        if (order.getOrderStatus() != OrderStatus.AWAITING_DISPATCH) {
            throw new IllegalArgumentException("Order " + order.getAccountNumber() + " is not awaiting dispatch");
        }
        if (remainingForProduct(order, product) <= 0) {
            throw new IllegalArgumentException("Order " + order.getAccountNumber()
                    + " does not need any more " + product.getName());
        }
        return order;
    }

    private Order oldestOrderNeeding(Product product) {
        List<Order> candidates = orderRepository.findAwaitingDispatchWithProduct(product.getId());
        return candidates.stream()
                .filter(order -> remainingForProduct(order, product) > 0)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No order awaiting dispatch needs " + product.getName()));
    }

    private long remainingForProduct(Order order, Product product) {
        long required = order.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(product.getId()))
                .mapToLong(item -> item.getQuantity().longValue())
                .sum();
        return required - stockUnitRepository.countByOrderIdAndProduct_Id(order.getId(), product.getId());
    }

    private OrderStatus dispatchStatus(Order order) {
        BigDecimal paid = order.getPaidAmount() != null ? order.getPaidAmount() : BigDecimal.ZERO;
        if (paid.compareTo(order.getTotalAmount()) >= 0) {
            return OrderStatus.DISPATCHED_PAID;
        }
        return paid.compareTo(BigDecimal.ZERO) > 0
                ? OrderStatus.DISPATCHED_PARTIAL
                : OrderStatus.DISPATCHED_UNPAID;
    }

    private void markDispatched(UUID orderId) {
        List<StockUnit> packed = stockUnitRepository.findByOrderIdAndStatus(orderId, StockUnitStatus.PACKED);
        packed.forEach(unit -> unit.setStatus(StockUnitStatus.DISPATCHED));
        stockUnitRepository.saveAll(packed);
    }
}
