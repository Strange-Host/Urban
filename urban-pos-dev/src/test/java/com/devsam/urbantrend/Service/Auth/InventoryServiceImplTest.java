package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Dtos.InventoryRequest;
import com.devsam.urbantrend.Entity.Inventory;
import com.devsam.urbantrend.Entity.Product;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Errors.ResourceNotFoundException;
import com.devsam.urbantrend.Repository.InventoryRepository;
import com.devsam.urbantrend.Repository.ProductRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

    @Mock private InventoryRepository inventoryRepository;
    @Mock private ProductRepository productRepository;

    private InventoryServiceImpl service;
    private Product product;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        service = new InventoryServiceImpl(inventoryRepository, productRepository);

        product = Product.builder().name("Leather Wallet").build();
        product.setId(UUID.randomUUID());

        inventory = Inventory.builder()
                .product(product)
                .quantity(new BigDecimal("3"))
                .reorderLevel(new BigDecimal("5"))
                .build();
        inventory.setId(UUID.randomUUID());

        lenient().when(productRepository.findByIdAndDeletedAtIsNull(product.getId())).thenReturn(Optional.of(product));
        lenient().when(inventoryRepository.findByProductIdForUpdate(product.getId())).thenReturn(Optional.of(inventory));
        lenient().when(inventoryRepository.save(any(Inventory.class))).thenAnswer(call -> call.getArgument(0));
        lenient().when(inventoryRepository.findWithProductById(inventory.getId())).thenReturn(Optional.of(inventory));
    }

    private InventoryRequest request(UUID productId, String quantity) {
        return InventoryRequest.builder()
                .productId(productId)
                .quantity(quantity == null ? null : new BigDecimal(quantity))
                .build();
    }

    @Test
    void insufficientStockNamesTheProductAndTheNumbers() {
        assertThatThrownBy(() -> service.reduceIfTracked(request(product.getId(), "5")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Insufficient stock for Leather Wallet: 3 available, 5 needed");

        assertThatThrownBy(() -> service.reduce(request(product.getId(), "5")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Leather Wallet");
    }

    @Test
    void missingQuantityOrProductIsABadRequestNotANullPointer() {
        assertThatThrownBy(() -> service.restock(request(product.getId(), null), new User()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.restock(request(null, "5"), new User()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("productId");
        assertThatThrownBy(() -> service.reduce(request(product.getId(), null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aDeletedProductCannotBeRestocked() {
        UUID deletedProduct = UUID.randomUUID();
        when(productRepository.findByIdAndDeletedAtIsNull(deletedProduct)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.restock(request(deletedProduct, "5"), new User()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void restockAddsToTheExistingQuantityAndReturnsTheInventoryWithItsProductLoaded() {
        Inventory result = service.restock(request(product.getId(), "4"), new User());

        assertThat(result.getQuantity()).isEqualByComparingTo("7");
        verify(inventoryRepository).findWithProductById(inventory.getId());
    }

    @Test
    void sellingExactlyWhatIsInStockIsAllowed() {
        service.reduceIfTracked(request(product.getId(), "3"));

        assertThat(inventory.getQuantity()).isEqualByComparingTo("0");
    }
    @Test
    void availabilityCheckNamesTheProductWhenStockIsShort() {
        when(inventoryRepository.findByProductId(product.getId())).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> service.ensureAvailableIfTracked(request(product.getId(), "4")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Insufficient stock for Leather Wallet: 3 available, 4 needed");
        assertThat(inventory.getQuantity()).isEqualByComparingTo("3");
    }

    @Test
    void availabilityCheckPassesForEnoughStockAndDoesNotChangeIt() {
        when(inventoryRepository.findByProductId(product.getId())).thenReturn(Optional.of(inventory));

        service.ensureAvailableIfTracked(request(product.getId(), "3"));

        assertThat(inventory.getQuantity()).isEqualByComparingTo("3");
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    void availabilityCheckIgnoresProductsWithoutAnInventoryRecord() {
        UUID untracked = UUID.randomUUID();
        when(inventoryRepository.findByProductId(untracked)).thenReturn(Optional.empty());

        service.ensureAvailableIfTracked(request(untracked, "100"));
    }
}
