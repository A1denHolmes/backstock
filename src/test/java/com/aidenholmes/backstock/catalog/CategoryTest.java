package com.aidenholmes.backstock.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CategoryTest {

    @Test
    void nameIsStrippedOnCreate() {
        Category category = new Category("\u2003Books ", "books");

        assertThat(category.getName()).isEqualTo("Books");
    }

    @Test
    void nameIsStrippedOnRename() {
        Category category = new Category("Books", "books");

        category.rename("\u2003Phones ");

        assertThat(category.getName()).isEqualTo("Phones");
    }
}
