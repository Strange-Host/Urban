package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.CourierRequest;
import com.devsam.urbantrend.Dtos.CourierResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface CourierService {

    CourierResponse create(CourierRequest request);

    CourierResponse getById(UUID id);

    Page<CourierResponse> getAll(Pageable pageable, boolean activeOnly, String query);

    List<CourierResponse> getAllActive();

    CourierResponse update(UUID id, CourierRequest request);

    void deactivate(UUID id);
}