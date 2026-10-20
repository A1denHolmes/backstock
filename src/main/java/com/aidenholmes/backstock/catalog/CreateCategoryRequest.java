package com.aidenholmes.backstock.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

record CreateCategoryRequest(
        @NotBlank
        @Size(max = Category.NAME_MAX_LENGTH)
        @Pattern(regexp = Category.NAME_PATTERN, message = Category.NAME_PATTERN_MESSAGE)
        String name,

        @NotBlank
        @Size(max = Category.SLUG_MAX_LENGTH)
        @Pattern(regexp = Category.SLUG_PATTERN, message = Category.SLUG_PATTERN_MESSAGE)
        String slug) {}
