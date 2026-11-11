package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.ProductRequest;
import com.devsam.urbantrend.Dtos.ProductResponse;
import com.devsam.urbantrend.Dtos.ProductStaffAssignmentResponse;
import com.devsam.urbantrend.Entity.Product;
import com.devsam.urbantrend.Entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ProductService {

    Product create(ProductRequest request, User user);

    Page<?> getAll(Pageable pageable,User user);

    Page<?> getAll(String query, Pageable pageable, User user);

    Page<?> search(String query, Pageable pageable, User user);

    Object getById(UUID id, User user);

    Product update(UUID id, ProductRequest request, User user);

    void delete(UUID id, User user);

    // add to the existing ProductService interface
    Product assignStaff(UUID productId, UUID staffId, User currentUser);

    Product unassignStaff(UUID productId, User currentUser);

    List<ProductStaffAssignmentResponse> getStaffAssignments(User currentUser);
}
