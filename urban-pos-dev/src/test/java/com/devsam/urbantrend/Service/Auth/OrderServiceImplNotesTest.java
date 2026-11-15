package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.UpdateOrderNotesRequest;
import com.devsam.urbantrend.Entity.Order;
import com.devsam.urbantrend.Entity.User;
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

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplNotesTest {

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

    @BeforeEach
    void setUp() {
        service = new OrderServiceImpl(orderRepository, orderItemRepository, productRepository,
                inventoryService, customerRepository, payrollService, courierRepository, paymentRepository, stockUnitRepository);

        order = new Order();
        order.setId(UUID.randomUUID());
        order.setDeliveryNotes("Call before arriving");
        when(orderRepository.findDetailedByIdAndDeletedAtIsNull(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(call -> call.getArgument(0));
    }

    private UpdateOrderNotesRequest notes(String text) {
        UpdateOrderNotesRequest request = new UpdateOrderNotesRequest();
        request.setDeliveryNotes(text);
        return request;
    }

    @Test
    void anEmptyNoteClearsTheNoteInsteadOfFailing() {
        assertThat(service.updateNotes(order.getId(), notes(""), new User()).getDeliveryNotes()).isNull();
    }

    @Test
    void aBlankOrMissingNoteAlsoClearsIt() {
        assertThat(service.updateNotes(order.getId(), notes("   "), new User()).getDeliveryNotes()).isNull();
        assertThat(service.updateNotes(order.getId(), notes(null), new User()).getDeliveryNotes()).isNull();
    }

    @Test
    void aRealNoteIsStored() {
        assertThat(service.updateNotes(order.getId(), notes("Leave with guard"), new User()).getDeliveryNotes())
                .isEqualTo("Leave with guard");
    }
}
