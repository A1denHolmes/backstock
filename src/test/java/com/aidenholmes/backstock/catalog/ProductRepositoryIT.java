package com.aidenholmes.backstock.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aidenholmes.backstock.common.PostgresContainerSupport;
import com.aidenholmes.backstock.config.JpaAuditingConfig;
import java.math.BigDecimal;
import java.util.Objects;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class ProductRepositoryIT extends PostgresContainerSupport {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProductRepository repository;

    @Test
    void productIsSavedWithCategory() {
        Category category = persistCategory();
        Product product =
                new Product("Book", "BOOK", "Description", new BigDecimal("100.00"), 1, category, "http://example.url");
        Long id = Objects.requireNonNull(repository.saveAndFlush(product).getId());
        entityManager.clear();

        Product found = repository.findById(id).orElseThrow();

        assertThat(found.getName()).isEqualTo("Book");
        assertThat(found.getSku()).isEqualTo("BOOK");
        assertThat(found.getDescription()).isEqualTo("Description");
        assertThat(found.getPrice()).isEqualByComparingTo("100.00");
        assertThat(found.getStockQuantity()).isEqualTo(1);
        assertThat(found.getCategory()).isEqualTo(category);
        assertThat(found.getImageUrl()).isEqualTo("http://example.url");
        assertThat(found.isActive()).isTrue();
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    void categoryIsLoadedLazily() {
        Category category = persistCategory();
        Product product =
                new Product("Book", "BOOK", "Description", new BigDecimal("100.00"), 1, category, "http://example.url");
        Long id = Objects.requireNonNull(repository.saveAndFlush(product).getId());
        entityManager.clear();

        Product found = repository.findById(id).orElseThrow();

        assertThat(found.getCategory().getId()).isEqualTo(category.getId());
        assertThat(Hibernate.isInitialized(found.getCategory())).isFalse();
    }

    @Test
    void productWithoutDescriptionAndImageIsSaved() {
        Product product = new Product("Book", "BOOK", null, new BigDecimal("100.00"), 1, persistCategory(), null);
        Long id = Objects.requireNonNull(repository.saveAndFlush(product).getId());
        entityManager.clear();

        Product found = repository.findById(id).orElseThrow();

        assertThat(found.getDescription()).isNull();
        assertThat(found.getImageUrl()).isNull();
    }

    @Test
    void negativeStockCannotBeStored() {
        Product product = new Product(
                "Book", "BOOK", "Description", new BigDecimal("100.00"), -1, persistCategory(), "http://example.url");

        assertThatThrownBy(() -> repository.saveAndFlush(product))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("products_stock_quantity_check");
    }

    @Test
    void nonPositivePriceCannotBeStored() {
        Product product =
                new Product("Book", "BOOK", "Description", BigDecimal.ZERO, 1, persistCategory(), "http://example.url");

        assertThatThrownBy(() -> repository.saveAndFlush(product))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("products_price_check");
    }

    @Test
    void duplicateSkuCannotBeStored() {
        Category category = persistCategory();
        repository.saveAndFlush(new Product("Book", "BOOK", null, new BigDecimal("200.00"), 1, category, null));
        Product product = new Product("AnotherBook", "BOOK", null, new BigDecimal("100.00"), 1, category, null);

        assertThatThrownBy(() -> repository.saveAndFlush(product))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("products_sku_key");
    }

    @Test
    void stockQuantityIsNotUpdatedThroughEntity() {
        Product saved = repository.saveAndFlush(new Product(
                "Book", "BOOK", "Description", new BigDecimal("100.00"), 1, persistCategory(), "http://example.url"));
        ReflectionTestUtils.setField(saved, "stockQuantity", 5);
        entityManager.flush();
        entityManager.clear();

        int stock = repository
                .findById(Objects.requireNonNull(saved.getId()))
                .orElseThrow()
                .getStockQuantity();

        assertThat(stock).isEqualTo(1);
    }

    private Category persistCategory() {
        return entityManager.persist(new Category("Books", "books"));
    }
}
