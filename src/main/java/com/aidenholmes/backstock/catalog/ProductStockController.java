package com.aidenholmes.backstock.catalog;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products/{id}/stock")
@RequiredArgsConstructor
@Tag(name = "Products")
class ProductStockController {

    private final ProductService service;

    @GetMapping
    @Operation(operationId = "getProductStock", summary = "Get product stock")
    @ApiResponse(responseCode = "200", description = "Current stock of the product")
    @ApiResponse(
            responseCode = "400",
            description = "Invalid product ID",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "404",
            description = "Product not found",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    StockResponse findStock(@PathVariable long id) {
        return service.findStock(id);
    }
}
