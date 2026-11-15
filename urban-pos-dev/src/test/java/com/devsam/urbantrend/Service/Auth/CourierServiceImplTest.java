package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.CourierRequest;
import com.devsam.urbantrend.Entity.Courier;
import com.devsam.urbantrend.Repository.CourierRepository;
import com.devsam.urbantrend.Repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CourierServiceImplTest {

    @Mock private CourierRepository courierRepository;
    @Mock private OrderRepository orderRepository;

    private CourierServiceImpl service;
    private Courier courier;

    @BeforeEach
    void setUp() {
        service = new CourierServiceImpl(courierRepository, orderRepository);
        courier = Courier.builder().name("Sam").phoneNumber("0712345678").active(true).build();
        courier.setId(UUID.randomUUID());
        lenient().when(courierRepository.findById(courier.getId())).thenReturn(Optional.of(courier));
        lenient().when(courierRepository.save(any(Courier.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void deactivatingOnlyReleasesParcelsNotYetCollected() {
        service.deactivate(courier.getId());

        verify(orderRepository).unassignCourierFromUndispatchedOrders(courier.getId());
    }

    @Test
    void switchingACourierOffThroughUpdateDoesTheSame() {
        CourierRequest request = new CourierRequest();
        request.setActive(false);

        service.update(courier.getId(), request);

        verify(orderRepository).unassignCourierFromUndispatchedOrders(courier.getId());
    }

    @Test
    void editingAnActiveCourierLeavesOrdersAlone() {
        CourierRequest request = new CourierRequest();
        request.setVehicleInfo("Motorbike");

        service.update(courier.getId(), request);

        verify(orderRepository, never()).unassignCourierFromUndispatchedOrders(any());
    }
}
