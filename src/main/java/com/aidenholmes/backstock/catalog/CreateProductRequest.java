package com.aidenholmes.backstock.catalog;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import org.hibernate.validator.constraints.URL;
import org.jspecify.annotations.Nullable;

record CreateProductRequest(
        @NotBlank
        @Size(min = 1, max = Product.NAME_MAX_LENGTH)
        @Pattern(regexp = Product.NAME_PATTERN, message = Product.NAME_PATTERN_MESSAGE)
        @Schema(example = "Book")
        String name,

        @NotBlank
        @Size(min = 1, max = Product.SKU_MAX_LENGTH)
        @Pattern(regexp = Product.SKU_PATTERN, message = Product.SKU_PATTERN_MESSAGE)
        @Schema(example = "BOOK", description = "Unique identifier of the product. Cannot be changed after creation")
        String sku,

        @Size(min = 1, max = Product.DESCRIPTION_MAX_LENGTH) @Schema(example = "Product's description.") @Nullable
        String description,

        @NotNull @Positive @Digits(integer = 15, fraction = 2) @Schema(example = "100.50")
        BigDecimal price,

        @NotNull
        @PositiveOrZero
        @Max(Product.STOCK_QUANTITY_MAX)
        @Schema(description = "Initial stock. Changed afterwards through the product stock endpoint")
        Integer stockQuantity,

        @NotNull Long categoryId,

        @Size(min = 1, max = Product.IMAGE_URL_MAX_LENGTH)
        @URL(protocol = "https")
        @Schema(example = "https://example.com/picture.jpg")
        @Nullable
        String imageUrl) {}
