package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.CourierRequest;
import com.devsam.urbantrend.Dtos.CourierResponse;
import com.devsam.urbantrend.Entity.Courier;
import com.devsam.urbantrend.Errors.ResourceNotFoundException;
import com.devsam.urbantrend.Repository.CourierRepository;
import com.devsam.urbantrend.Repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CourierServiceImpl implements CourierService {

    private final CourierRepository courierRepository;
    private final OrderRepository orderRepository;

    @Override
    public CourierResponse create(CourierRequest request) {

        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Courier name is required");
        }
        if (request.getPhoneNumber() == null || request.getPhoneNumber().isBlank()) {
            throw new IllegalArgumentException("Courier phone number is required");
        }
        if (courierRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new IllegalArgumentException("A courier with this phone number already exists");
        }

        Courier courier = Courier.builder()
                .name(request.getName())
                .phoneNumber(request.getPhoneNumber())
                .vehicleInfo(request.getVehicleInfo())
                .notes(request.getNotes())
                .active(request.getActive() == null || request.getActive())
                .build();

        return toResponse(courierRepository.save(courier));
    }

    @Override
    public CourierResponse getById(UUID id) {
        return toResponse(getEntity(id));
    }

    @Override
    public Page<CourierResponse> getAll(Pageable pageable, boolean activeOnly) {
        Page<Courier> page = activeOnly
                ? courierRepository.findAllByActiveTrue(pageable)
                : courierRepository.findAll(pageable);
        return page.map(this::toResponse);
    }

    @Override
    public List<CourierResponse> getAllActive() {
        return courierRepository.findAllByActiveTrueOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public CourierResponse update(UUID id, CourierRequest request) {
        Courier courier = getEntity(id);

        boolean wasActive = courier.isActive();

        if (request.getName() != null && !request.getName().isBlank()) {
            courier.setName(request.getName());
        }
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()) {
            String phoneNumber = request.getPhoneNumber().trim();
            if (!phoneNumber.equals(courier.getPhoneNumber())
                    && courierRepository.existsByPhoneNumberAndIdNot(phoneNumber, id)) {
                throw new IllegalArgumentException("A courier with this phone number already exists");
            }
            courier.setPhoneNumber(phoneNumber);
        }
        if (request.getVehicleInfo() != null) {
            courier.setVehicleInfo(request.getVehicleInfo());
        }
        if (request.getNotes() != null) {
            courier.setNotes(request.getNotes());
        }
        if (request.getActive() != null) {
            courier.setActive(request.getActive());
        }

        Courier saved = courierRepository.save(courier);

        if (wasActive && !saved.isActive()) {
            orderRepository.unassignCourier(saved.getId());
        }

        return toResponse(saved);
    }

    @Override
    public void deactivate(UUID id) {
        Courier courier = getEntity(id);
        courier.setActive(false);
        courierRepository.save(courier);
        orderRepository.unassignCourier(courier.getId());
    }

    private Courier getEntity(UUID id) {
        return courierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Courier not found with id " + id));
    }

    private CourierResponse toResponse(Courier courier) {
        return CourierResponse.builder()
                .id(courier.getId())
                .name(courier.getName())
                .phoneNumber(courier.getPhoneNumber())
                .vehicleInfo(courier.getVehicleInfo())
                .notes(courier.getNotes())
                .active(courier.isActive())
                .createdAt(courier.getCreatedAt())
                .build();
    }
}