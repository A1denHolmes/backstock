package com.aidenholmes.backstock.catalog;

import com.aidenholmes.backstock.common.EntityTags;
import com.aidenholmes.backstock.common.Versioned;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products")
class ProductController {

    private final ProductService service;

    @GetMapping("/{id}")
    @Operation(operationId = "getProduct", summary = "Get a product")
    @ApiResponse(
            responseCode = "200",
            description = "Product found",
            headers =
                    @Header(
                            name = HttpHeaders.ETAG,
                            description = "Entity tag of the product, sent back in If-Match to change it"))
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
    ResponseEntity<ProductResponse> findById(@PathVariable long id) {
        Versioned<ProductResponse> found = service.findById(id);
        return ResponseEntity.ok().eTag(EntityTags.of(found.version())).body(found.body());
    }

    @PostMapping
    @Operation(operationId = "createProduct", summary = "Create a product")
    @ApiResponse(
            responseCode = "201",
            description = "Product created",
            useReturnTypeSchema = true,
            headers = {
                @Header(name = HttpHeaders.LOCATION, description = "URL of the created product"),
                @Header(
                        name = HttpHeaders.ETAG,
                        description = "Entity tag of the product, sent back in If-Match to change it")
            })
    @ApiResponse(
            responseCode = "400",
            description = "Invalid request body",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "404",
            description = "Category not found",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "409",
            description = "SKU is already taken",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        Versioned<ProductResponse> created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}")
                .build(created.body().id());
        return ResponseEntity.created(location)
                .eTag(EntityTags.of(created.version()))
                .body(created.body());
    }

    @PutMapping("/{id}")
    @Operation(operationId = "updateProduct", summary = "Update a product")
    @ApiResponse(
            responseCode = "200",
            description = "Product updated",
            headers = @Header(name = HttpHeaders.ETAG, description = "Entity tag of the updated product"))
    @ApiResponse(
            responseCode = "400",
            description = "Invalid product ID or request body",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "404",
            description = "Product or category not found",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "412",
            description = "Product was modified by another request",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "428",
            description = "If-Match header is missing or contains *",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<ProductResponse> update(
            @PathVariable long id,
            @Valid @RequestBody UpdateProductRequest request,
            @RequestHeader(name = HttpHeaders.IF_MATCH, required = false)
                    @Parameter(required = true, description = "ETag from the latest read of the product")
                    @Nullable
                    String ifMatch) {
        Versioned<ProductResponse> updated = service.update(id, request, EntityTags.parseIfMatch(ifMatch));
        return ResponseEntity.ok().eTag(EntityTags.of(updated.version())).body(updated.body());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "deactivateProduct", summary = "Deactivate a product")
    @ApiResponse(responseCode = "204", description = "Product deactivated")
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
    @ApiResponse(
            responseCode = "412",
            description = "Product was modified by another request",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "428",
            description = "If-Match header is missing or contains *",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    void deactivate(
            @PathVariable long id,
            @RequestHeader(name = HttpHeaders.IF_MATCH, required = false)
                    @Parameter(required = true, description = "ETag from the latest read of the product")
                    @Nullable
                    String ifMatch) {
        service.deactivate(id, EntityTags.parseIfMatch(ifMatch));
    }
}
