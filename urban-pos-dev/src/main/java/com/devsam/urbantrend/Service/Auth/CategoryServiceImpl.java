package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.CategoryRequest;
import com.devsam.urbantrend.Entity.Category;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Errors.ResourceNotFoundException;
import com.devsam.urbantrend.Repository.CategoryRepository;
import com.devsam.urbantrend.Repository.ProductRepository;
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
    private final ProductRepository productRepository;

    @Override
    public Category addCategory(CategoryRequest request, User user) {


        String name = request.getName().trim();
        ensureNameIsFree(name, null);

        Category category = Category.builder()
                .name(name)
                .description(request.getDescription())
                .build();

        return categoryRepository.save(category);
    }

    @Override
    public Category getCategoryById(UUID id) {
        return categoryRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Category not found")
                );
    }
    @Override
    public Page<Category> getAllCategories(Pageable pageable, String query) {

        boolean hasQuery = query != null && !query.trim().isEmpty();
        if (!hasQuery) {
            return categoryRepository.findAllByDeletedAtIsNull(pageable);
        }

        String pattern = "%" + query.trim().toLowerCase() + "%";
        return categoryRepository.searchByPattern(pattern, pageable);
    }

    @Override
    public Category updateCategory(UUID id, CategoryRequest request, User user) {


        Category category = getCategoryById(id);

        String name = request.getName().trim();
        ensureNameIsFree(name, id);

        category.setName(name);
        category.setDescription(request.getDescription());

        return categoryRepository.save(category);
    }

    @Override
    public void deleteCategory(UUID id, User user) {


        Category category = getCategoryById(id);

        // Products keep pointing at a deleted category and then cannot be edited, so it
        // has to be empty first.
        if (productRepository.existsByCategory_IdAndDeletedAtIsNull(id)) {
            throw new IllegalStateException(
                    "This category still has products. Move or delete them before deleting the category.");
        }

        category.setDeletedAt(java.time.LocalDateTime.now());
        // The name column is unique, so a deleted category would block its name forever.
        freeName(category);

        categoryRepository.save(category);
    }

    /**
     * A live category with the same name (ignoring case) blocks it. Deleted ones from before
     * names were freed on delete are renamed out of the way instead.
     */
    private void ensureNameIsFree(String name, UUID updatingId) {
        for (Category other : categoryRepository.findAllByNameIgnoreCase(name)) {
            if (other.getId().equals(updatingId)) {
                continue;
            }
            if (other.getDeletedAt() == null) {
                throw new IllegalArgumentException("Category already exists");
            }
            freeName(other);
            categoryRepository.saveAndFlush(other);
        }
    }

    private static void freeName(Category category) {
        String suffix = " (deleted " + category.getId().toString().substring(0, 8) + ")";
        if (!category.getName().endsWith(suffix)) {
            category.setName(category.getName() + suffix);
        }
    }

}

