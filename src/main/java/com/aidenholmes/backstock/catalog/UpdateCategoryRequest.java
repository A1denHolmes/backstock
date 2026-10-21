package com.aidenholmes.backstock.catalog;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

record UpdateCategoryRequest(
        @NotBlank
        @Size(min = 1, max = Category.NAME_MAX_LENGTH)
        @Pattern(regexp = Category.NAME_PATTERN, message = Category.NAME_PATTERN_MESSAGE)
        @Schema(example = "Phones")
        String name) {}
