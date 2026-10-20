package com.aidenholmes.backstock.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

record UpdateCategoryRequest(
        @NotBlank
        @Size(max = Category.NAME_MAX_LENGTH)
        @Pattern(regexp = Category.NAME_PATTERN, message = Category.NAME_PATTERN_MESSAGE)
        String name) {}
