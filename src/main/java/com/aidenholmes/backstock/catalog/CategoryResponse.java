package com.aidenholmes.backstock.catalog;

import java.time.Instant;
import java.util.Objects;

record CategoryResponse(long id, String name, String slug, Instant createdAt, Instant updatedAt) {

    static CategoryResponse from(Category category) {
        return new CategoryResponse(
                Objects.requireNonNull(category.getId()),
                category.getName(),
                category.getSlug(),
                Objects.requireNonNull(category.getCreatedAt()),
                Objects.requireNonNull(category.getUpdatedAt()));
    }
}
