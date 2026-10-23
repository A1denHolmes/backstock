package com.aidenholmes.backstock.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ProductTest {

    @Test
    void nameIsStrippedOnCreate() {
        Product product =
                new Product("\u2003Book ", "BOOK", null, BigDecimal.ONE, 1, new Category("Books", "books"), null);

        assertThat(product.getName()).isEqualTo("Book");
    }

    @Test
    void nameIsStrippedOnUpdate() {
        Product product = new Product("Book", "BOOK", null, BigDecimal.ONE, 1, new Category("Books", "books"), null);

        product.update(
                "\u2003Renamed ",
                product.getDescription(),
                product.getPrice(),
                product.getCategory(),
                product.getImageUrl());

        assertThat(product.getName()).isEqualTo("Renamed");
    }

    @Test
    void productIsUpdated() {
        Product product = new Product("Book", "BOOK", null, BigDecimal.ONE, 1, new Category("Books", "books"), null);
        Category anotherCategory = new Category("Phones", "phones");

        product.update("Renamed", "Description", BigDecimal.TWO, anotherCategory, "http://example.url");

        assertThat(product.getName()).isEqualTo("Renamed");
        assertThat(product.getDescription()).isEqualTo("Description");
        assertThat(product.getPrice()).isEqualByComparingTo(BigDecimal.TWO);
        assertThat(product.getCategory()).isSameAs(anotherCategory);
        assertThat(product.getImageUrl()).isEqualTo("http://example.url");
    }
}
