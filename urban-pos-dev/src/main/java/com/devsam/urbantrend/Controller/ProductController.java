package com.devsam.urbantrend.Controller;

import com.devsam.urbantrend.Dtos.ProductLookupResponse;
import com.devsam.urbantrend.Dtos.ProductRequest;
import com.devsam.urbantrend.Dtos.ProductStaffAssignmentRequest;
import com.devsam.urbantrend.Dtos.ProductStaffAssignmentResponse;
import com.devsam.urbantrend.Entity.Product;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Repository.ProductRepository;
import com.devsam.urbantrend.Repository.UserRepository;
import com.devsam.urbantrend.Service.Auth.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @PostMapping
    @PreAuthorize("@userPermissionService.hasPermissionByEmail(authentication.name, 'CAN_CREATE_PRODUCTS')")
    public Product createProduct(
            @RequestBody ProductRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getUser(userDetails);
        return productService.create(request, user);
    }

    @GetMapping("/by-name")
    @PreAuthorize("hasAuthority('CAN_VIEW_PRODUCTS')")
    public ResponseEntity<ProductLookupResponse> getByName(@RequestParam String name) {
        Product product = productRepository.findFirstByNameIgnoreCaseAndDeletedAtIsNull(name.trim())
                .orElseThrow(() -> new RuntimeException("Product not found: " + name));
        return ResponseEntity.ok(ProductLookupResponse.from(product));
    }

    @GetMapping
    @PreAuthorize("@userPermissionService.hasPermissionByEmail(authentication.name, 'CAN_VIEW_PRODUCTS')")
    public Page<?> getAllProducts(
            @RequestParam(required = false) String query,
            Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getUser(userDetails);
        return productService.getAll(query, pageable, user);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('CAN_VIEW_PRODUCTS')")
    public Page<?> searchProducts(
            @RequestParam String query,
            Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getUser(userDetails);
        return productService.search(query, pageable, user);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@userPermissionService.hasPermissionByEmail(authentication.name, 'CAN_VIEW_PRODUCTS')")
    public Object getProductById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getUser(userDetails);
        return productService.getById(id, user);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@userPermissionService.hasPermissionByEmail(authentication.name, 'CAN_EDIT_PRODUCTS')")
    public Product updateProduct(
            @PathVariable UUID id,
            @RequestBody ProductRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getUser(userDetails);
        return productService.update(id, request, user);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@userPermissionService.hasPermissionByEmail(authentication.name, 'CAN_DELETE_PRODUCTS')")
    public void deleteProduct(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getUser(userDetails);
        productService.delete(id, user);
    }

    private User getUser(UserDetails userDetails) {
        return userRepository.findByEmailAndDeletedAtIsNull(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PostMapping("/{id}/assign-staff")
    @PreAuthorize("@userPermissionService.hasPermissionByEmail(authentication.name, 'CAN_MANAGE_PRODUCT_ASSIGNMENTS')")
    public Product assignStaff(
            @PathVariable UUID id,
            @RequestBody ProductStaffAssignmentRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getUser(userDetails);
        return productService.assignStaff(id, request.getStaffId(), user);
    }

    @DeleteMapping("/{id}/assign-staff")
    @PreAuthorize("@userPermissionService.hasPermissionByEmail(authentication.name, 'CAN_MANAGE_PRODUCT_ASSIGNMENTS')")
    public Product unassignStaff(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getUser(userDetails);
        return productService.unassignStaff(id, user);
    }

    @GetMapping("/staff-assignments")
    @PreAuthorize("hasAuthority('CAN_MANAGE_PRODUCT_ASSIGNMENTS')")
    public List<ProductStaffAssignmentResponse> getStaffAssignments(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getUser(userDetails);
        return productService.getStaffAssignments(user);
    }
}