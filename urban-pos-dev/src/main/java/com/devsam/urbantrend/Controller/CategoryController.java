package com.devsam.urbantrend.Controller;

import com.devsam.urbantrend.Dtos.CategoryRequest;
import com.devsam.urbantrend.Entity.Category;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Repository.CategoryRepository;
import com.devsam.urbantrend.Repository.UserRepository;
import com.devsam.urbantrend.Service.Auth.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@RequestMapping("/api/v1/category")
@RestController
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final UserRepository userRepository;

    @PostMapping
    @PreAuthorize("hasAuthority('CAN_CREATE_CATEGORIES')")
    public Category createCategory(
            @Valid @RequestBody CategoryRequest categoryRequest,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return categoryService.addCategory(categoryRequest, user);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CAN_VIEW_CATEGORIES')")
    public Page<Category> getAllCategories(
            @RequestParam(required = false) String query,
            @PageableDefault(sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return categoryService.getAllCategories(pageable, query);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CAN_VIEW_CATEGORIES')")
    public Category getCategoryById(@PathVariable UUID id) {
        return categoryService.getCategoryById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CAN_EDIT_CATEGORIES')")
    public Category updateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryRequest categoryRequest,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return categoryService.updateCategory(id, categoryRequest, user);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CAN_DELETE_CATEGORIES')")
    public void deleteCategory(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        categoryService.deleteCategory(id, user);
    }
}
