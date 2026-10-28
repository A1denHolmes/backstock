package com.aidenholmes.backstock.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.aidenholmes.backstock.common.ConflictException;
import com.aidenholmes.backstock.common.NotFoundException;
import com.aidenholmes.backstock.common.VersionMismatchException;
import com.aidenholmes.backstock.common.Versioned;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-19T10:00:00Z");
    private static final CreateProductRequest CREATE_BOOK =
            new CreateProductRequest("Book", "BOOK", null, new BigDecimal("10"), 1, 1L, null);
    private static final UpdateProductRequest UPDATE_TO_PHONE = new UpdateProductRequest(
            "Phone", "Description", new BigDecimal("20.50"), 2L, "https://example.com/picture.jpg");

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    private ProductService service;

    @BeforeEach
    void setUp() {
        service = new ProductService(productRepository, categoryRepository);
    }

    @Test
    void productIsCreatedWhenSkuIsFree() {
        when(productRepository.existsBySku("BOOK")).thenReturn(false);
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> persistedProduct(invocation.getArgument(0), 1L, 1L));
        when(categoryRepository.findForShareByIdAndActiveTrue(1L))
                .thenReturn(Optional.of(persistedCategory(new Category("Books", "books"), 1L)));

        Versioned<ProductResponse> created = service.create(CREATE_BOOK);

        assertThat(created)
                .isEqualTo(new Versioned<>(
                        new ProductResponse(1L, "Book", "BOOK", null, new BigDecimal("10.00"), 1, 1L, null, NOW, NOW),
                        1L));
    }

    @Test
    void productWithTakenSkuIsRejected() {
        when(productRepository.existsBySku("BOOK")).thenReturn(true);

        assertThatThrownBy(() -> service.create(CREATE_BOOK)).isInstanceOf(ConflictException.class);
        verify(productRepository, never()).save(any());
    }

    @Test
    void productWithUnknownCategoryIsRejected() {
        when(productRepository.existsBySku("BOOK")).thenReturn(false);
        when(categoryRepository.findForShareByIdAndActiveTrue(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(CREATE_BOOK)).isInstanceOf(NotFoundException.class);
        verify(productRepository, never()).save(any());
    }

    @Test
    void productIsFoundById() {
        when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(persistedBook()));

        Versioned<ProductResponse> found = service.findById(1L);

        assertThat(found)
                .isEqualTo(new Versioned<>(
                        new ProductResponse(1L, "Book", "BOOK", null, new BigDecimal("10.00"), 1, 1L, null, NOW, NOW),
                        1L));
    }

    @Test
    void unknownProductIsNotFound() {
        when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(1L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void stockIsFoundByProductId() {
        when(productRepository.findStockQuantityByIdAndActiveTrue(1L)).thenReturn(Optional.of(10));

        assertThat(service.findStock(1L)).isEqualTo(new StockResponse(10));
    }

    @Test
    void stockOfUnknownProductIsNotFound() {
        when(productRepository.findStockQuantityByIdAndActiveTrue(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findStock(1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Product 1 not found");
    }

    @Test
    void existingProductIsUpdated() {
        when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(persistedBook()));
        when(categoryRepository.findForShareByIdAndActiveTrue(2L))
                .thenReturn(Optional.of(persistedCategory(new Category("Phones", "phones"), 2L)));

        Versioned<ProductResponse> updated = service.update(1L, UPDATE_TO_PHONE, 1L);

        assertThat(updated)
                .isEqualTo(new Versioned<>(
                        new ProductResponse(
                                1L,
                                "Phone",
                                "BOOK",
                                "Description",
                                new BigDecimal("20.50"),
                                1,
                                2L,
                                "https://example.com/picture.jpg",
                                NOW,
                                NOW),
                        1L));
    }

    @Test
    void unknownProductCannotBeUpdated() {
        when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(1L, UPDATE_TO_PHONE, 1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Product 1 not found");
    }

    @Test
    void productCannotBeMovedToUnknownCategory() {
        when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(persistedBook()));
        when(categoryRepository.findForShareByIdAndActiveTrue(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(1L, UPDATE_TO_PHONE, 1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Category 2 not found");
    }

    @Test
    void updateWithOutdatedVersionIsRejected() {
        Product product = persistedProduct(book(), 1L, 2L);
        when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.update(1L, UPDATE_TO_PHONE, 1L))
                .isInstanceOf(VersionMismatchException.class)
                .hasMessage("Product 1 was modified by another request. Reload it and try again");
        assertThat(product.getName()).isEqualTo("Book");
        verify(productRepository, never()).flush();
        verifyNoInteractions(categoryRepository);
    }

    @Test
    void concurrentModificationOnUpdateFlushIsReportedAsVersionMismatch() {
        Category phones = persistedCategory(new Category("Phones", "phones"), 2L);
        when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(persistedBook()));
        when(categoryRepository.findForShareByIdAndActiveTrue(2L)).thenReturn(Optional.of(phones));
        doThrow(new OptimisticLockingFailureException("Optimistic locking failure"))
                .when(productRepository)
                .flush();

        assertThatThrownBy(() -> service.update(1L, UPDATE_TO_PHONE, 1L))
                .isInstanceOf(VersionMismatchException.class)
                .hasMessage("Product 1 was modified by another request. Reload it and try again")
                .hasCauseInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    void activeProductIsDeactivated() {
        Product product = persistedBook();
        when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(product));

        service.deactivate(1L, 1L);

        assertThat(product.isActive()).isFalse();
    }

    @Test
    void unknownProductCannotBeDeactivated() {
        when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivate(1L, 1L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void deactivationWithOutdatedVersionIsRejected() {
        Product product = persistedProduct(book(), 1L, 2L);
        when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.deactivate(1L, 1L))
                .isInstanceOf(VersionMismatchException.class)
                .hasMessage("Product 1 was modified by another request. Reload it and try again");
        assertThat(product.isActive()).isTrue();
        verify(productRepository, never()).flush();
    }

    private static Category persistedCategory(Category category, long id) {
        ReflectionTestUtils.setField(category, "id", id);
        ReflectionTestUtils.setField(category, "createdAt", NOW);
        ReflectionTestUtils.setField(category, "updatedAt", NOW);
        return category;
    }

    private static Product persistedProduct(Product product, long id, long version) {
        ReflectionTestUtils.setField(product, "id", id);
        ReflectionTestUtils.setField(product, "version", version);
        ReflectionTestUtils.setField(product, "createdAt", NOW);
        ReflectionTestUtils.setField(product, "updatedAt", NOW);
        return product;
    }

    private static Product persistedBook() {
        return persistedProduct(book(), 1L, 1L);
    }

    private static Product book() {
        return new Product(
                "Book",
                "BOOK",
                null,
                new BigDecimal("10"),
                1,
                persistedCategory(new Category("Books", "books"), 1L),
                null);
    }
}
