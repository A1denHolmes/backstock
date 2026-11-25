package com.aidenholmes.backstock.catalog;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

@ValidPriceRange
record ProductFilterRequest(
        @Nullable @Positive @Schema(description = "Filter by positive category ID.", example = "1")
        Long categoryId,

        @Nullable
        @PositiveOrZero
        @Digits(integer = 15, fraction = 2)
        @Schema(
                description = "Inclusive minimum price. Must be nonnegative with at most two decimal places.",
                example = "100.00")
        BigDecimal minPrice,

        @Nullable
        @PositiveOrZero
        @Digits(integer = 15, fraction = 2)
        @Schema(
                description = "Inclusive maximum price. Must be nonnegative with at most two decimal places.",
                example = "200.00")
        BigDecimal maxPrice) {}
