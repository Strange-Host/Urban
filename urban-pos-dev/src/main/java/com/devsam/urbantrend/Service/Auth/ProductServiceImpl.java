package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.*;
import com.devsam.urbantrend.Entity.Category;
import com.devsam.urbantrend.Entity.Product;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Enum.ProductStatus;
import com.devsam.urbantrend.Errors.ResourceNotFoundException;
import com.devsam.urbantrend.Repository.CategoryRepository;
import com.devsam.urbantrend.Repository.OrderRepository;
import com.devsam.urbantrend.Repository.ProductRepository;
import com.devsam.urbantrend.Repository.UserRepository;
import com.devsam.urbantrend.Roles.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Override
    public Product create(ProductRequest request, User user) {

        if (request.getSku() != null &&
                productRepository.existsBySku(request.getSku())) {
            throw new IllegalArgumentException("SKU already exists");
        }

        Category category = categoryRepository
                .findByIdAndDeletedAtIsNull(request.getCategoryId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Category not found")
                );

        User assignedStaff = null;
        if (request.getAssignedStaffId() != null) {
            assignedStaff = userRepository.findById(request.getAssignedStaffId())
                    .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));
        }

        Product product = Product.builder()
                .name(request.getName())
                .sku(request.getSku())
                .sellingPrice(request.getSellingPrice())
                .costPrice(request.getCostPrice())
                .category(category)
                .status(ProductStatus.ACTIVE)
                .assignedStaff(assignedStaff)
                .build();

        return productRepository.save(product);
    }

    @Override
    public Page<?> getAll(Pageable pageable, User user) {
        return getAll(null, pageable, user);
    }

    @Override
    public Page<?> getAll(String query, Pageable pageable, User user) {

        boolean hasQuery = query != null && !query.trim().isEmpty();
        String trimmedQuery = hasQuery ? query.trim() : null;

        Page<Product> products;

        if (user.getRole() == UserRole.STAFF) {
            products = hasQuery
                    ? productRepository.findByNameIgnoreCaseContainingAndDeletedAtIsNullAndAssignedStaff(
                            trimmedQuery, user, pageable)
                    : productRepository.findAllByDeletedAtIsNullAndAssignedStaff(user, pageable);
        } else {
            products = hasQuery
                    ? productRepository.findByNameIgnoreCaseContainingAndDeletedAtIsNull(trimmedQuery, pageable)
                    : productRepository.findAllByDeletedAtIsNull(pageable);
        }

        return products.map(product -> toResponse(product, user));
    }


    @Override
    public Page<?> search(String query, Pageable pageable, User user) {

        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Search query is required");
        }

        String trimmedQuery = query.trim();

        Page<Product> products = (user.getRole() == UserRole.STAFF)
                ? productRepository.findByNameIgnoreCaseContainingAndDeletedAtIsNullAndAssignedStaff(
                        trimmedQuery, user, pageable)
                : productRepository.findByNameIgnoreCaseContainingAndDeletedAtIsNull(trimmedQuery, pageable);

        return products.map(product -> toResponse(product, user));
    }

    @Override
    public Object getById(UUID id, User user) {

        Product product = productRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product not found")
                );

        return toResponse(product, user);
    }

    private Object toResponse(Product product, User user) {

        if (user.getRole() == UserRole.ADMIN) {
            return AdminProductResponse.builder()
                    .id(product.getId())
                    .name(product.getName())
                    .sku(product.getSku())
                    .sellingPrice(product.getSellingPrice())
                    .costPrice(product.getCostPrice())
                    .categoryId(product.getCategory().getId())
                    .assignedStaffId(product.getAssignedStaff() != null ? product.getAssignedStaff().getId() : null)
                    .assignedStaffName(product.getAssignedStaff() != null ? staffName(product.getAssignedStaff()) : null)
                    .build();
        }

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .sku(product.getSku())
                .sellingPrice(product.getSellingPrice())
                .categoryId(product.getCategory().getId())
                .assignedStaffId(product.getAssignedStaff() != null ? product.getAssignedStaff().getId() : null)
                .assignedStaffName(product.getAssignedStaff() != null ? staffName(product.getAssignedStaff()) : null)
                .build();
    }

    private String staffName(User staff) {
        String first = staff.getFirstName() == null ? "" : staff.getFirstName();
        String last = staff.getLastName() == null ? "" : staff.getLastName();
        return (first + " " + last).trim();
    }


    public Product getByProductId(UUID id, User user) {
        return productRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(()-> new ResourceNotFoundException(" product with id "+ id + "not found"));
    }

    @Override
    public Product update(UUID id, ProductRequest request, User user) {

        Product product = getByProductId(id,user);

        if (request.getSku() != null
                && !request.getSku().isBlank()
                && !request.getSku().equals(product.getSku())
                && productRepository.existsBySku(request.getSku())) {
            throw new IllegalArgumentException("SKU already exists");
        }

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository
                    .findByIdAndDeletedAtIsNull(request.getCategoryId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Category not found")
                    );
        }

        product.setName(request.getName());
        product.setSku(request.getSku());
        product.setSellingPrice(request.getSellingPrice());
        product.setCostPrice(request.getCostPrice());
        if (category != null) {
            product.setCategory(category);
        }

        return productRepository.save(product);
    }

    @Override
    public void delete(UUID id, User user) {

        Product product = getByProductId(id, user);
        product.setDeletedAt(LocalDateTime.now());
        product.setStatus(ProductStatus.INACTIVE);

        productRepository.save(product);
    }

    @Override
    public Product assignStaff(UUID productId, UUID staffId, User currentUser) {

        Product product = getByProductId(productId, currentUser);

        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));

        product.setAssignedStaff(staff);

        return productRepository.save(product);
    }

    @Override
    public Product unassignStaff(UUID productId, User currentUser) {

        Product product = getByProductId(productId, currentUser);
        product.setAssignedStaff(null);

        return productRepository.save(product);
    }

    @Override
    public List<ProductStaffAssignmentResponse> getStaffAssignments(User currentUser) {

        return productRepository.findAllByDeletedAtIsNull(Pageable.unpaged())
                .stream()
                .filter(product -> product.getAssignedStaff() != null)
                .map(product -> ProductStaffAssignmentResponse.builder()
                        .productId(product.getId())
                        .productName(product.getName())
                        .staffId(product.getAssignedStaff().getId())
                        .staffName(staffName(product.getAssignedStaff()))
                        .build())
                .toList();
    }
}