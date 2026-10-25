package com.aidenholmes.backstock.catalog;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

record ProductResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long id,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "Book")
        String name,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "BOOK")
        String sku,

        @Schema(example = "Product's description.") @Nullable
        String description,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "100.50")
        BigDecimal price,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int stockQuantity,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long categoryId,

        @Schema(example = "https://example.com/picture.jpg") @Nullable
        String imageUrl,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant createdAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant updatedAt) {

    static ProductResponse from(Product product) {
        BigDecimal price = product.getPrice().setScale(2, RoundingMode.HALF_EVEN);

        return new ProductResponse(
                Objects.requireNonNull(product.getId()),
                product.getName(),
                product.getSku(),
                product.getDescription(),
                price,
                product.getStockQuantity(),
                Objects.requireNonNull(product.getCategory().getId()),
                product.getImageUrl(),
                Objects.requireNonNull(product.getCreatedAt()),
                Objects.requireNonNull(product.getUpdatedAt()));
    }
}
