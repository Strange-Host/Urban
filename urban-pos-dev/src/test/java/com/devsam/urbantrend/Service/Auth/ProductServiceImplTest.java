package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.AdminProductResponse;
import com.devsam.urbantrend.Dtos.ProductRequest;
import com.devsam.urbantrend.Dtos.ProductResponse;
import com.devsam.urbantrend.Entity.Category;
import com.devsam.urbantrend.Entity.Product;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Repository.CategoryRepository;
import com.devsam.urbantrend.Repository.OrderRepository;
import com.devsam.urbantrend.Repository.ProductRepository;
import com.devsam.urbantrend.Repository.UserRepository;
import com.devsam.urbantrend.Roles.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private UserRepository userRepository;

    private ProductServiceImpl service;
    private Category category;
    private Product product;

    @BeforeEach
    void setUp() {
        service = new ProductServiceImpl(productRepository, categoryRepository, orderRepository, userRepository);

        category = new Category();
        category.setId(UUID.randomUUID());

        product = Product.builder()
                .name("Shirt")
                .sku("SH-1")
                .sellingPrice(new BigDecimal("500"))
                .costPrice(new BigDecimal("300"))
                .category(category)
                .build();
        product.setId(UUID.randomUUID());

        lenient().when(productRepository.findByIdAndDeletedAtIsNull(product.getId()))
                .thenReturn(Optional.of(product));
        lenient().when(categoryRepository.findByIdAndDeletedAtIsNull(category.getId()))
                .thenReturn(Optional.of(category));
        lenient().when(productRepository.save(any(Product.class))).thenAnswer(call -> call.getArgument(0));
    }

    private static User user(UserRole role) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setRole(role);
        return user;
    }

    private ProductRequest request(String sku, String sellingPrice, String costPrice) {
        ProductRequest request = new ProductRequest();
        request.setName("Shirt");
        request.setSku(sku);
        request.setSellingPrice(new BigDecimal(sellingPrice));
        request.setCostPrice(costPrice == null ? null : new BigDecimal(costPrice));
        request.setCategoryId(category.getId());
        return request;
    }

    @Test
    void managerEditingAProductCannotOverwriteTheCostTheyCannotSee() {
        service.update(product.getId(), request("SH-1", "550", "1"), user(UserRole.MANAGER));

        assertThat(product.getSellingPrice()).isEqualByComparingTo("550");
        assertThat(product.getCostPrice()).isEqualByComparingTo("300");
    }

    @Test
    void managerEditWithoutAnyCostStillWorks() {
        service.update(product.getId(), request("SH-1", "550", null), user(UserRole.MANAGER));

        assertThat(product.getCostPrice()).isEqualByComparingTo("300");
    }

    @Test
    void adminCanChangeTheCost() {
        service.update(product.getId(), request("SH-1", "550", "320"), user(UserRole.ADMIN));

        assertThat(product.getCostPrice()).isEqualByComparingTo("320");
    }

    @Test
    void creatingAProductNeedsACost() {
        assertThatThrownBy(() -> service.create(request("NEW-1", "500", null), user(UserRole.MANAGER)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cost price is required");

        verify(productRepository, never()).save(any());
    }

    @Test
    void blankSkuIsStoredAsNoSkuSoSeveralProductsCanLackOne() {
        Product created = service.create(request("   ", "500", "300"), user(UserRole.MANAGER));

        assertThat(created.getSku()).isNull();
        verify(productRepository, never()).existsBySku(any());
    }

    @Test
    void omittingTheSkuOnUpdateLeavesItAloneAndBlankClearsIt() {
        service.update(product.getId(), request(null, "500", null), user(UserRole.MANAGER));
        assertThat(product.getSku()).isEqualTo("SH-1");

        service.update(product.getId(), request("  ", "500", null), user(UserRole.MANAGER));
        assertThat(product.getSku()).isNull();
    }

    @Test
    void onlyAdminsReceiveTheCostInProductResponses() {
        Object forAdmin = service.getById(product.getId(), user(UserRole.ADMIN));
        Object forManager = service.getById(product.getId(), user(UserRole.MANAGER));

        assertThat(forAdmin).isInstanceOf(AdminProductResponse.class);
        assertThat(((AdminProductResponse) forAdmin).getCostPrice()).isEqualByComparingTo("300");
        assertThat(forManager).isInstanceOf(ProductResponse.class);
        assertThat(forManager).isNotInstanceOf(AdminProductResponse.class);
    }
}
