package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Entity.Order;
import com.devsam.urbantrend.Entity.StockUnit;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Enum.OrderStatus;
import com.devsam.urbantrend.Enum.StockUnitStatus;
import com.devsam.urbantrend.Repository.CourierRepository;
import com.devsam.urbantrend.Repository.CustomerRepository;
import com.devsam.urbantrend.Repository.OrderItemRepository;
import com.devsam.urbantrend.Repository.OrderRepository;
import com.devsam.urbantrend.Repository.PaymentRepository;
import com.devsam.urbantrend.Repository.ProductRepository;
import com.devsam.urbantrend.Repository.StockUnitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplStockUnitsTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private ProductRepository productRepository;
    @Mock private InventoryService inventoryService;
    @Mock private CustomerRepository customerRepository;
    @Mock private PayrollService payrollService;
    @Mock private CourierRepository courierRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private StockUnitRepository stockUnitRepository;

    private OrderServiceImpl service;
    private Order order;
    private StockUnit unit;

    @BeforeEach
    void setUp() {
        service = new OrderServiceImpl(orderRepository, orderItemRepository, productRepository,
                inventoryService, customerRepository, payrollService, courierRepository,
                paymentRepository, stockUnitRepository);

        order = new Order();
        order.setId(UUID.randomUUID());
        order.setTotalAmount(new BigDecimal("1000"));
        order.setPaidAmount(BigDecimal.ZERO);

        unit = StockUnit.builder()
                .serial("UT00000001")
                .status(StockUnitStatus.PACKED)
                .orderId(order.getId())
                .scannedAt(LocalDateTime.now())
                .scannedBy(UUID.randomUUID())
                .build();

        lenient().when(orderRepository.lockById(order.getId())).thenReturn(Optional.of(order));
        lenient().when(orderRepository.findDetailedByIdAndDeletedAtIsNull(order.getId()))
                .thenReturn(Optional.of(order));
        lenient().when(orderRepository.save(any(Order.class))).thenAnswer(call -> call.getArgument(0));
        lenient().when(stockUnitRepository.findByOrderId(order.getId())).thenReturn(List.of(unit));
    }

    @Test
    void cancellingAnOrderReleasesItsPackedUnits() {
        order.setOrderStatus(OrderStatus.AWAITING_DISPATCH);

        service.updateStatus(order.getId(), OrderStatus.CANCELLED, new User());

        assertThat(unit.getStatus()).isEqualTo(StockUnitStatus.IN_STOCK);
        assertThat(unit.getOrderId()).isNull();
        assertThat(unit.getScannedAt()).isNull();
        assertThat(unit.getScannedBy()).isNull();
        verify(stockUnitRepository).saveAll(List.of(unit));
    }

    @Test
    void aGoodReturnPutsDispatchedUnitsBackSoTheyCanBeScannedAgain() {
        unit.setStatus(StockUnitStatus.DISPATCHED);
        order.setOrderStatus(OrderStatus.RETURNED_AWAITING_INSPECTION);

        service.updateStatus(order.getId(), OrderStatus.RETURNED_GOOD, new User());

        assertThat(unit.getStatus()).isEqualTo(StockUnitStatus.IN_STOCK);
        assertThat(unit.getOrderId()).isNull();
    }

    @Test
    void aDamagedReturnLeavesTheUnitsAlone() {
        unit.setStatus(StockUnitStatus.DISPATCHED);
        order.setOrderStatus(OrderStatus.RETURNED_AWAITING_INSPECTION);

        service.updateStatus(order.getId(), OrderStatus.RETURNED_DAMAGED, new User());

        assertThat(unit.getStatus()).isEqualTo(StockUnitStatus.DISPATCHED);
        verify(stockUnitRepository, never()).saveAll(any());
    }
}
