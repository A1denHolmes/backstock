package com.aidenholmes.backstock.catalog;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Range;

record AdjustStockRequest(
        @NotNull
        @Range(min = -Product.STOCK_QUANTITY_MAX, max = Product.STOCK_QUANTITY_MAX)
        @Schema(example = "-2", description = "Change to apply to the current stock. Negative values decrease it")
        Integer delta) {}
