package com.aidenholmes.backstock.catalog;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Objects;

record CategoryResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long id,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "Books")
        String name,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "books")
        String slug,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant createdAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant updatedAt) {

    static CategoryResponse from(Category category) {
        return new CategoryResponse(
                Objects.requireNonNull(category.getId()),
                category.getName(),
                category.getSlug(),
                Objects.requireNonNull(category.getCreatedAt()),
                Objects.requireNonNull(category.getUpdatedAt()));
    }
}
