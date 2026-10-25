package com.aidenholmes.backstock.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aidenholmes.backstock.common.ConflictException;
import com.aidenholmes.backstock.common.NotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
                .thenAnswer(invocation -> persistedProduct(invocation.getArgument(0), 1L));
        when(categoryRepository.findForShareByIdAndActiveTrue(1L))
                .thenReturn(Optional.of(persistedCategory(new Category("Books", "books"), 1L)));

        ProductResponse created = service.create(CREATE_BOOK);

        assertThat(created)
                .isEqualTo(
                        new ProductResponse(1L, "Book", "BOOK", null, new BigDecimal("10.00"), 1, 1L, null, NOW, NOW));
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

        ProductResponse found = service.findById(1L);

        assertThat(found)
                .isEqualTo(
                        new ProductResponse(1L, "Book", "BOOK", null, new BigDecimal("10.00"), 1, 1L, null, NOW, NOW));
    }

    @Test
    void unknownProductIsNotFound() {
        when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(1L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void existingProductIsUpdated() {
        when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(persistedBook()));
        when(categoryRepository.findForShareByIdAndActiveTrue(2L))
                .thenReturn(Optional.of(persistedCategory(new Category("Phones", "phones"), 2L)));

        ProductResponse updated = service.update(1L, UPDATE_TO_PHONE);

        assertThat(updated)
                .isEqualTo(new ProductResponse(
                        1L,
                        "Phone",
                        "BOOK",
                        "Description",
                        new BigDecimal("20.50"),
                        1,
                        2L,
                        "https://example.com/picture.jpg",
                        NOW,
                        NOW));
    }

    @Test
    void unknownProductCannotBeUpdated() {
        when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(1L, UPDATE_TO_PHONE))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Product 1 not found");
    }

    @Test
    void productCannotBeMovedToUnknownCategory() {
        when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(persistedBook()));
        when(categoryRepository.findForShareByIdAndActiveTrue(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(1L, UPDATE_TO_PHONE))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Category 2 not found");
    }

    @Test
    void activeProductIsDeactivated() {
        Product product = persistedBook();
        when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(product));

        service.deactivate(1L);

        assertThat(product.isActive()).isFalse();
    }

    @Test
    void unknownProductCannotBeDeactivated() {
        when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivate(1L)).isInstanceOf(NotFoundException.class);
    }

    private static Category persistedCategory(Category category, long id) {
        ReflectionTestUtils.setField(category, "id", id);
        ReflectionTestUtils.setField(category, "createdAt", NOW);
        ReflectionTestUtils.setField(category, "updatedAt", NOW);
        return category;
    }

    private static Product persistedProduct(Product product, long id) {
        ReflectionTestUtils.setField(product, "id", id);
        ReflectionTestUtils.setField(product, "createdAt", NOW);
        ReflectionTestUtils.setField(product, "updatedAt", NOW);
        return product;
    }

    private static Product persistedBook() {
        return persistedProduct(
                new Product(
                        "Book",
                        "BOOK",
                        null,
                        new BigDecimal("10"),
                        1,
                        persistedCategory(new Category("Books", "books"), 1L),
                        null),
                1L);
    }
}
