package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.CategoryRequest;
import com.devsam.urbantrend.Entity.Category;
import com.devsam.urbantrend.Entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public interface CategoryService {
    Category addCategory(CategoryRequest categoryRequest, User user);

    Category getCategoryById(UUID id);

    Page<Category> getAllCategories(Pageable pageable, String query);

    Category updateCategory(UUID id, CategoryRequest request, User user);

    void deleteCategory(UUID id, User user);
}
