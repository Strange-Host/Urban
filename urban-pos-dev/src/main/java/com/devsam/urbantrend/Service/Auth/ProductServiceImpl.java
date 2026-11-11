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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
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

        if (user.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("Only ADMIN can create products");
        }

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

        Page<Product> products = (query != null && !query.trim().isEmpty())
                ? productRepository.findByNameIgnoreCaseContainingAndDeletedAtIsNull(query.trim(), pageable)
                : productRepository.findAllByDeletedAtIsNull(pageable);

        if (user.getRole() == UserRole.STAFF) {
            List<Product> filtered = products.getContent().stream()
                    .filter(product -> product.getAssignedStaff() != null
                            && product.getAssignedStaff().getId().equals(user.getId()))
                    .toList();
            products = new PageImpl<>(filtered, pageable, filtered.size());
        }

        if (user.getRole() == UserRole.ADMIN) {
            return products.map(product ->
                    AdminProductResponse.builder()
                            .id(product.getId())
                            .name(product.getName())
                            .sku(product.getSku())
                            .sellingPrice(product.getSellingPrice())
                            .costPrice(product.getCostPrice())
                            .categoryId(product.getCategory().getId())
                            .assignedStaffId(product.getAssignedStaff() != null ? product.getAssignedStaff().getId() : null)
                            .assignedStaffName(product.getAssignedStaff() != null ? staffName(product.getAssignedStaff()) : null)
                            .build()
            );
        }

        return products.map(product ->
                ProductResponse.builder()
                        .id(product.getId())
                        .name(product.getName())
                        .sku(product.getSku())
                        .sellingPrice(product.getSellingPrice())
                        .categoryId(product.getCategory().getId())
                        .assignedStaffId(product.getAssignedStaff() != null ? product.getAssignedStaff().getId() : null)
                        .assignedStaffName(product.getAssignedStaff() != null ? staffName(product.getAssignedStaff()) : null)
                        .build()
        );
    }


    @Override
    public Page<?> search(String query, Pageable pageable, User user) {

        Page<Product> products =
                productRepository.findByNameIgnoreCaseContainingAndDeletedAtIsNull(query, pageable);

        if (user.getRole() == UserRole.ADMIN) {
            return products.map(product ->
                    AdminProductResponse.builder()
                            .id(product.getId())
                            .name(product.getName())
                            .sku(product.getSku())
                            .sellingPrice(product.getSellingPrice())
                            .costPrice(product.getCostPrice())
                            .categoryId(product.getCategory().getId())
                            .assignedStaffId(product.getAssignedStaff() != null ? product.getAssignedStaff().getId() : null)
                            .assignedStaffName(product.getAssignedStaff() != null ? staffName(product.getAssignedStaff()) : null)
                            .build()
            );
        }

        return products.map(product ->
                ProductResponse.builder()
                        .id(product.getId())
                        .name(product.getName())
                        .sku(product.getSku())
                        .sellingPrice(product.getSellingPrice())
                        .categoryId(product.getCategory().getId())
                        .assignedStaffId(product.getAssignedStaff() != null ? product.getAssignedStaff().getId() : null)
                        .assignedStaffName(product.getAssignedStaff() != null ? staffName(product.getAssignedStaff()) : null)
                        .build()
        );
    }

    @Override
    public Object getById(UUID id, User user) {

        Product product = productRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product not found")
                );

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
        return productRepository.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException(" product with id "+ id + "not found"));
    }

    @Override
    public Product update(UUID id, ProductRequest request, User user) {

        if (user.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("Only ADMIN can update products");
        }

        Product product = getByProductId(id,user);

        product.setName(request.getName());
        product.setSellingPrice(request.getSellingPrice());
        product.setCostPrice(request.getCostPrice());


        return productRepository.save(product);
    }

    @Override
    public void delete(UUID id, User user) {

        if (user.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("Only ADMIN can delete products");
        }

        Product product = getByProductId(id, user);
        product.setDeletedAt(LocalDateTime.now());
        product.setStatus(ProductStatus.INACTIVE);

        productRepository.save(product);
    }

    @Override
    public Product assignStaff(UUID productId, UUID staffId, User currentUser) {

        if (currentUser.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("Only ADMIN can assign staff to products");
        }

        Product product = getByProductId(productId, currentUser);

        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));

        product.setAssignedStaff(staff);

        return productRepository.save(product);
    }

    @Override
    public Product unassignStaff(UUID productId, User currentUser) {

        if (currentUser.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("Only ADMIN can unassign staff from products");
        }

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