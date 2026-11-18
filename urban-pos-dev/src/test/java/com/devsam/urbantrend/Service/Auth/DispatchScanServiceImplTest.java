package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.ScanRequest;
import com.devsam.urbantrend.Dtos.ScanResponse;
import com.devsam.urbantrend.Entity.Order;
import com.devsam.urbantrend.Entity.OrderItem;
import com.devsam.urbantrend.Entity.Product;
import com.devsam.urbantrend.Entity.StockUnit;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Enum.OrderStatus;
import com.devsam.urbantrend.Enum.StockUnitStatus;
import com.devsam.urbantrend.Repository.OrderRepository;
import com.devsam.urbantrend.Repository.StockUnitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DispatchScanServiceImplTest {

    @Mock private StockUnitRepository stockUnitRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderService orderService;

    private DispatchScanServiceImpl service;
    private User packer;
    private Product product;
    private Order order;

    @BeforeEach
    void setUp() {
        service = new DispatchScanServiceImpl(stockUnitRepository, orderRepository, orderService);

        packer = new User();
        packer.setId(UUID.randomUUID());

        product = Product.builder().name("Leather Wallet").build();
        product.setId(UUID.randomUUID());

        // Two wallets ordered.
        OrderItem item = OrderItem.builder().product(product).quantity(new BigDecimal("2")).build();
        order = new Order();
        order.setId(UUID.randomUUID());
        order.setAccountNumber("8025631#ABCDE");
        order.setOrderStatus(OrderStatus.AWAITING_DISPATCH);
        order.setTotalAmount(new BigDecimal("1000"));
        order.setPaidAmount(BigDecimal.ZERO);
        order.setItems(List.of(item));

        lenient().when(orderService.getById(order.getId())).thenReturn(order);
        lenient().when(orderRepository.lockById(order.getId())).thenReturn(Optional.of(order));
    }

    private void unitsPackedForTheOrder(long count) {
        lenient().when(stockUnitRepository.countByOrderId(order.getId())).thenReturn(count);
        lenient().when(stockUnitRepository.countByOrderIdAndProduct_Id(order.getId(), product.getId()))
                .thenReturn(count);
    }

    @Test
    void anOrderThatIsNotFullyPackedCannotBeFinished() {
        unitsPackedForTheOrder(1);

        assertThatThrownBy(() -> service.completeDispatch(order.getId(), packer))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1 of 2 units scanned");

        verify(orderService, never()).updateStatus(any(), any(), any());
    }

    @Test
    void aFullyPackedOrderIsDispatchedAndItsUnitsMarkedDispatched() {
        unitsPackedForTheOrder(2);
        order.setPaidAmount(new BigDecimal("1000"));
        StockUnit packed = StockUnit.builder().serial("UT1").status(StockUnitStatus.PACKED).build();
        when(stockUnitRepository.findByOrderIdAndStatus(order.getId(), StockUnitStatus.PACKED))
                .thenReturn(List.of(packed));

        ScanResponse response = service.completeDispatch(order.getId(), packer);

        verify(orderService).updateStatus(order.getId(), OrderStatus.DISPATCHED_PAID, packer);
        assertThat(packed.getStatus()).isEqualTo(StockUnitStatus.DISPATCHED);
        assertThat(response.isOrderDispatched()).isTrue();
        assertThat(response.getUnitsScanned()).isEqualTo(2);
        assertThat(response.getUnitsRequired()).isEqualTo(2);
    }

    @Test
    void thePaymentStateDecidesTheDispatchStatus() {
        unitsPackedForTheOrder(2);

        service.completeDispatch(order.getId(), packer);
        verify(orderService).updateStatus(order.getId(), OrderStatus.DISPATCHED_UNPAID, packer);

        order.setPaidAmount(new BigDecimal("300"));
        service.completeDispatch(order.getId(), packer);
        verify(orderService).updateStatus(order.getId(), OrderStatus.DISPATCHED_PARTIAL, packer);
    }

    @Test
    void anOrderThatIsNotAwaitingDispatchIsRejected() {
        order.setOrderStatus(OrderStatus.DISPATCHED_PAID);

        assertThatThrownBy(() -> service.completeDispatch(order.getId(), packer))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not awaiting dispatch");
    }

    @Test
    void scanningLocksTheOrderSoTheLastTwoScansCannotMissEachOther() {
        StockUnit unit = StockUnit.builder().serial("UT00000002").product(product).status(StockUnitStatus.IN_STOCK).build();
        when(stockUnitRepository.findBySerial("UT00000002")).thenReturn(Optional.of(unit));
        unitsPackedForTheOrder(2); // this scan brings the order to 2 of 2

        ScanRequest request = new ScanRequest();
        request.setBarcode("ut00000002");
        request.setOrderId(order.getId());

        ScanResponse response = service.scan(request, packer);

        verify(orderRepository).lockById(order.getId());
        verify(orderService).updateStatus(order.getId(), OrderStatus.DISPATCHED_UNPAID, packer);
        assertThat(response.isOrderDispatched()).isTrue();
    }

    @Test
    void fractionalQuantitiesRoundUpToWholeUnits() {
        OrderItem half = OrderItem.builder().quantity(new BigDecimal("1.5")).build();
        OrderItem whole = OrderItem.builder().quantity(new BigDecimal("3")).build();
        OrderItem tiny = OrderItem.builder().quantity(new BigDecimal("0.2")).build();

        org.assertj.core.api.Assertions.assertThat(DispatchScanServiceImpl.unitsFor(half)).isEqualTo(2);
        org.assertj.core.api.Assertions.assertThat(DispatchScanServiceImpl.unitsFor(whole)).isEqualTo(3);
        org.assertj.core.api.Assertions.assertThat(DispatchScanServiceImpl.unitsFor(tiny)).isEqualTo(1);
    }
}
