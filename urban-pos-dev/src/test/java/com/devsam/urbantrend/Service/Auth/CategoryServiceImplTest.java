package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.CategoryRequest;
import com.devsam.urbantrend.Entity.Category;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Repository.CategoryRepository;
import com.devsam.urbantrend.Repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private ProductRepository productRepository;

    private CategoryServiceImpl service;
    private final User admin = new User();

    @BeforeEach
    void setUp() {
        service = new CategoryServiceImpl(categoryRepository, productRepository);
        lenient().when(categoryRepository.save(any(Category.class))).thenAnswer(call -> call.getArgument(0));
        lenient().when(categoryRepository.saveAndFlush(any(Category.class))).thenAnswer(call -> call.getArgument(0));
    }

    private static Category category(String name) {
        Category category = Category.builder().name(name).build();
        category.setId(UUID.randomUUID());
        return category;
    }

    private static CategoryRequest request(String name) {
        CategoryRequest request = new CategoryRequest();
        request.setName(name);
        return request;
    }

    @Test
    void aNameCanBeReusedAfterTheCategoryWasDeletedAndTheLegacyRowIsRenamedOutOfTheWay() {
        Category legacyDeleted = category("Shirts");
        legacyDeleted.setDeletedAt(LocalDateTime.now().minusDays(10));
        when(categoryRepository.findAllByNameIgnoreCase("Shirts")).thenReturn(List.of(legacyDeleted));

        Category created = service.addCategory(request("  Shirts "), admin);

        assertThat(created.getName()).isEqualTo("Shirts");
        assertThat(legacyDeleted.getName()).startsWith("Shirts (deleted ");
        verify(categoryRepository).saveAndFlush(legacyDeleted);
    }

    @Test
    void aLiveCategoryWithTheSameNameStillBlocks() {
        when(categoryRepository.findAllByNameIgnoreCase("shirts")).thenReturn(List.of(category("Shirts")));

        assertThatThrownBy(() -> service.addCategory(request("shirts"), admin))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Category already exists");
    }

    @Test
    void renamingToItsOwnNameIsNotAConflict() {
        Category existing = category("Shirts");
        when(categoryRepository.findByIdAndDeletedAtIsNull(existing.getId())).thenReturn(Optional.of(existing));
        when(categoryRepository.findAllByNameIgnoreCase("Shirts")).thenReturn(List.of(existing));

        Category updated = service.updateCategory(existing.getId(), request("Shirts"), admin);

        assertThat(updated.getName()).isEqualTo("Shirts");
    }

    @Test
    void renamingOntoAnotherLiveCategoryIsRejected() {
        Category existing = category("Shirts");
        when(categoryRepository.findByIdAndDeletedAtIsNull(existing.getId())).thenReturn(Optional.of(existing));
        when(categoryRepository.findAllByNameIgnoreCase("Shoes")).thenReturn(List.of(category("Shoes")));

        assertThatThrownBy(() -> service.updateCategory(existing.getId(), request("Shoes"), admin))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aCategoryWithProductsCannotBeDeleted() {
        Category existing = category("Shirts");
        when(categoryRepository.findByIdAndDeletedAtIsNull(existing.getId())).thenReturn(Optional.of(existing));
        when(productRepository.existsByCategory_IdAndDeletedAtIsNull(existing.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.deleteCategory(existing.getId(), admin))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("still has products");

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void deletingAnEmptyCategoryFreesItsName() {
        Category existing = category("Shirts");
        when(categoryRepository.findByIdAndDeletedAtIsNull(existing.getId())).thenReturn(Optional.of(existing));

        service.deleteCategory(existing.getId(), admin);

        assertThat(existing.getDeletedAt()).isNotNull();
        assertThat(existing.getName()).isNotEqualTo("Shirts").startsWith("Shirts (deleted ");
    }
}
