package com.aidenholmes.backstock.catalog;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

record CreateCategoryRequest(
        @NotBlank
        @Size(min = 1, max = Category.NAME_MAX_LENGTH)
        @Pattern(regexp = Category.NAME_PATTERN, message = Category.NAME_PATTERN_MESSAGE)
        @Schema(example = "Books")
        String name,

        @NotBlank
        @Size(min = 1, max = Category.SLUG_MAX_LENGTH)
        @Pattern(regexp = Category.SLUG_PATTERN, message = Category.SLUG_PATTERN_MESSAGE)
        @Schema(example = "books", description = "URL identifier of the category. Cannot be changed after creation")
        String slug) {}
