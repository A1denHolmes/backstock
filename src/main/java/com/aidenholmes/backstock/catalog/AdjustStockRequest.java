package com.aidenholmes.backstock.catalog;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

record AdjustStockRequest(
        @NotNull
        @Min(-Product.STOCK_QUANTITY_MAX)
        @Max(Product.STOCK_QUANTITY_MAX)
        @Schema(example = "-2", description = "Change to apply to the current stock. Negative values decrease it")
        Integer delta) {}
