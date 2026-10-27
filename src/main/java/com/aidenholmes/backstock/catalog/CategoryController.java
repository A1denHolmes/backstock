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
import java.util.List;
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
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories")
class CategoryController {

    private final CategoryService service;

    @GetMapping
    @Operation(operationId = "listCategories", summary = "List categories")
    @ApiResponse(responseCode = "200", description = "Active categories")
    List<CategoryResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @Operation(operationId = "getCategory", summary = "Get a category")
    @ApiResponse(
            responseCode = "200",
            description = "Category found",
            headers =
                    @Header(
                            name = HttpHeaders.ETAG,
                            description = "Entity tag of the category, sent back in If-Match to change it"))
    @ApiResponse(
            responseCode = "400",
            description = "Invalid category ID",
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
    ResponseEntity<CategoryResponse> findById(@PathVariable long id) {
        Versioned<CategoryResponse> found = service.findById(id);
        return ResponseEntity.ok().eTag(EntityTags.of(found.version())).body(found.body());
    }

    @PostMapping
    @Operation(operationId = "createCategory", summary = "Create a category")
    @ApiResponse(
            responseCode = "201",
            description = "Category created",
            useReturnTypeSchema = true,
            headers = {
                @Header(name = HttpHeaders.LOCATION, description = "URL of the created category"),
                @Header(
                        name = HttpHeaders.ETAG,
                        description = "Entity tag of the category, sent back in If-Match to change it")
            })
    @ApiResponse(
            responseCode = "400",
            description = "Invalid request body",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "409",
            description = "Slug is already taken",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<CategoryResponse> create(@Valid @RequestBody CreateCategoryRequest request) {
        Versioned<CategoryResponse> created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}")
                .build(created.body().id());
        return ResponseEntity.created(location)
                .eTag(EntityTags.of(created.version()))
                .body(created.body());
    }

    @PutMapping("/{id}")
    @Operation(operationId = "updateCategory", summary = "Rename a category")
    @ApiResponse(
            responseCode = "200",
            description = "Category renamed",
            headers = @Header(name = HttpHeaders.ETAG, description = "Entity tag of the updated category"))
    @ApiResponse(
            responseCode = "400",
            description = "Invalid category ID or request body",
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
            responseCode = "412",
            description = "Category was modified by another request",
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
    ResponseEntity<CategoryResponse> update(
            @PathVariable long id,
            @Valid @RequestBody UpdateCategoryRequest request,
            @RequestHeader(name = HttpHeaders.IF_MATCH, required = false)
                    @Parameter(required = true, description = "ETag from the latest read of the category")
                    @Nullable
                    String ifMatch) {
        Versioned<CategoryResponse> updated = service.update(id, request, EntityTags.parseIfMatch(ifMatch));
        return ResponseEntity.ok().eTag(EntityTags.of(updated.version())).body(updated.body());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "deactivateCategory", summary = "Deactivate a category")
    @ApiResponse(responseCode = "204", description = "Category deactivated")
    @ApiResponse(
            responseCode = "400",
            description = "Invalid category ID",
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
            description = "Category has active products",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "412",
            description = "Category was modified by another request",
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
                    @Parameter(required = true, description = "ETag from the latest read of the category")
                    @Nullable
                    String ifMatch) {
        service.deactivate(id, EntityTags.parseIfMatch(ifMatch));
    }
}
