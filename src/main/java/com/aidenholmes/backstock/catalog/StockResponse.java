package com.aidenholmes.backstock.catalog;

import io.swagger.v3.oas.annotations.media.Schema;

record StockResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int quantity) {}
