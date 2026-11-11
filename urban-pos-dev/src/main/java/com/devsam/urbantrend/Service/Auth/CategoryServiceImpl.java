package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.CategoryRequest;
import com.devsam.urbantrend.Entity.Category;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Errors.ResourceNotFoundException;
import com.devsam.urbantrend.Repository.CategoryRepository;
import com.devsam.urbantrend.Roles.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    public Category addCategory(CategoryRequest request, User user) {

        if (user.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("Only ADMIN can create categories");
        }

        if (categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new IllegalArgumentException("Category already exists");
        }

        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

        return categoryRepository.save(category);
    }

    @Override
    public Category getCategoryById(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Category not found")
                );
    }
    @Override
    public Page<Category> getAllCategories(Pageable pageable) {
        return categoryRepository.findAll(pageable);
    }

    @Override
    public Category updateCategory(UUID id, CategoryRequest request, User user) {

        if (user.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("Only ADMIN can update categories");
        }

        Category category = getCategoryById(id);

        category.setName(request.getName());
        category.setDescription(request.getDescription());

        return categoryRepository.save(category);
    }

    @Override
    public void deleteCategory(UUID id, User user) {

        if (user.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("Only ADMIN can delete categories");
        }

        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category not found");
        }

        categoryRepository.deleteById(id);
    }

}

