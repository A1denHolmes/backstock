package com.aidenholmes.backstock.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.aidenholmes.backstock.common.ConflictException;
import com.aidenholmes.backstock.common.EntityTags;
import com.aidenholmes.backstock.common.NotFoundException;
import com.aidenholmes.backstock.common.VersionMismatchException;
import com.aidenholmes.backstock.common.Versioned;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@ActiveProfiles("test")
@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    private static final Instant NOW = Instant.parse("2026-10-19T10:00:00Z");
    private static final UpdateCategoryRequest RENAME_REQUEST = new UpdateCategoryRequest("Renamed");
    private static final Versioned<CategoryResponse> RENAMED_RESPONSE =
            new Versioned<>(new CategoryResponse(1L, "Renamed", "books", NOW, NOW), 2L);
    private static final Versioned<CategoryResponse> BOOKS_RESPONSE =
            new Versioned<>(new CategoryResponse(1L, "Books", "books", NOW, NOW), 1L);

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private CategoryService service;

    @Test
    void categoriesAreReturned() {
        when(service.findAll())
                .thenReturn(List.of(
                        new CategoryResponse(1L, "Books", "books", NOW, NOW),
                        new CategoryResponse(2L, "Phones", "phones", NOW, NOW)));

        MvcTestResult result = mvc.get().uri("/api/v1/categories").exchange();

        assertThat(result).hasStatus(HttpStatus.OK).bodyJson().isStrictlyEqualTo("""
            [{
                "id": 1,
                "name": "Books",
                "slug": "books",
                "createdAt": "2026-10-19T10:00:00Z",
                "updatedAt": "2026-10-19T10:00:00Z"
            },
            {
                "id": 2,
                "name": "Phones",
                "slug": "phones",
                "createdAt": "2026-10-19T10:00:00Z",
                "updatedAt": "2026-10-19T10:00:00Z"
            }]
        """);
    }

    @Test
    void categoryIsReturnedById() {
        when(service.findById(1L)).thenReturn(BOOKS_RESPONSE);

        assertThat(mvc.get().uri("/api/v1/categories/1"))
                .hasStatus(HttpStatus.OK)
                .hasHeader(HttpHeaders.ETAG, EntityTags.of(1L))
                .bodyJson()
                .isStrictlyEqualTo("""
                    {
                        "id": 1,
                        "name": "Books",
                        "slug": "books",
                        "createdAt": "2026-10-19T10:00:00Z",
                        "updatedAt": "2026-10-19T10:00:00Z"
                    }
                """);
    }

    @Test
    void unknownCategoryReturns404() {
        when(service.findById(1L)).thenThrow(new NotFoundException("Category 1 not found"));

        assertThat(mvc.get().uri("/api/v1/categories/1"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.detail")
                .isEqualTo("Category 1 not found");
    }

    @Test
    void invalidIdReturns400() {
        assertThat(mvc.get().uri("/api/v1/categories/abc"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void matchingIfNoneMatchReturns304() {
        when(service.findById(1L)).thenReturn(BOOKS_RESPONSE);

        assertThat(mvc.get().uri("/api/v1/categories/1").header(HttpHeaders.IF_NONE_MATCH, EntityTags.of(1L)))
                .hasStatus(HttpStatus.NOT_MODIFIED)
                .hasHeader(HttpHeaders.ETAG, "\"1\"")
                .body()
                .isEmpty();
    }

    @Test
    void categoryIsCreatedAndReturns201WithLocationEtagAndBody() {
        when(service.create(new CreateCategoryRequest("Books", "books"))).thenReturn(BOOKS_RESPONSE);

        assertThat(mvc.post()
                        .uri("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"name": "Books", "slug": "books"}
                        """))
                .hasStatus(HttpStatus.CREATED)
                .hasHeader(HttpHeaders.LOCATION, "http://localhost/api/v1/categories/1")
                .hasHeader(HttpHeaders.ETAG, EntityTags.of(1L))
                .bodyJson()
                .isStrictlyEqualTo("""
                    {
                        "id": 1,
                        "name": "Books",
                        "slug": "books",
                        "createdAt": "2026-10-19T10:00:00Z",
                        "updatedAt": "2026-10-19T10:00:00Z"
                    }
                """);
    }

    @Test
    void locationIgnoresQueryParameters() {
        when(service.create(new CreateCategoryRequest("Books", "books"))).thenReturn(BOOKS_RESPONSE);

        assertThat(mvc.post()
                        .uri("/api/v1/categories?source=admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"name": "Books", "slug": "books"}
                        """))
                .hasHeader(HttpHeaders.LOCATION, "http://localhost/api/v1/categories/1");
    }

    @Test
    void takenSlugReturns409() {
        when(service.create(any(CreateCategoryRequest.class)))
                .thenThrow(new ConflictException("Category with slug \"books\" already exists"));

        assertThat(mvc.post()
                        .uri("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"name": "Books", "slug": "books"}
                        """))
                .hasStatus(HttpStatus.CONFLICT)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .extractingPath("$.detail")
                .isEqualTo("Category with slug \"books\" already exists");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"Books", "-books", "books-", "bo--oks", "книги", "my books"})
    void invalidSlugReturns400(String slug) {
        assertThat(mvc.post()
                        .uri("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"name": "Books", "slug": "%s"}
                        """.formatted(slug)))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.errors[0].field")
                .isEqualTo("slug");
    }

    @Test
    void invisibleCharacterInNameReturns400() {
        assertThat(mvc.post()
                        .uri("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"name": "\u200B", "slug": "books"}
                        """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .extractingPath("$.errors[0].field")
                .isEqualTo("name");
    }

    @Test
    void categoryIsRenamedAndReturns200WithEtagAndBody() {
        when(service.update(1L, RENAME_REQUEST, 1L)).thenReturn(RENAMED_RESPONSE);

        assertThat(mvc.put()
                        .uri("/api/v1/categories/1")
                        .header(HttpHeaders.IF_MATCH, EntityTags.of(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"name": "Renamed"}
                        """))
                .hasStatus(HttpStatus.OK)
                .hasHeader(HttpHeaders.ETAG, EntityTags.of(2L))
                .bodyJson()
                .isStrictlyEqualTo("""
                    {
                        "id": 1,
                        "name": "Renamed",
                        "slug": "books",
                        "createdAt": "2026-10-19T10:00:00Z",
                        "updatedAt": "2026-10-19T10:00:00Z"
                    }
                """);
    }

    @Test
    void blankNewNameReturns400() {
        assertThat(mvc.put()
                        .uri("/api/v1/categories/1")
                        .header(HttpHeaders.IF_MATCH, EntityTags.of(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"name": "   "}
                        """))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void renamingOfUnknownCategoryReturns404() {
        when(service.update(1L, RENAME_REQUEST, 1L)).thenThrow(new NotFoundException("Category 1 not found"));

        assertThat(mvc.put()
                        .uri("/api/v1/categories/1")
                        .header(HttpHeaders.IF_MATCH, EntityTags.of(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"name": "Renamed"}
                        """))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.detail")
                .isEqualTo("Category 1 not found");
    }

    @Test
    void renamingWithoutIfMatchReturns428() {
        assertThat(mvc.put()
                        .uri("/api/v1/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"name": "Renamed"}
                        """))
                .hasStatus(HttpStatus.PRECONDITION_REQUIRED)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .extractingPath("$.detail")
                .isEqualTo("If-Match header is required. Send the ETag from the latest read of the resource");
        verifyNoInteractions(service);
    }

    @Test
    void renamingWithWildcardIfMatchReturns428() {
        assertThat(mvc.put()
                        .uri("/api/v1/categories/1")
                        .header(HttpHeaders.IF_MATCH, "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"name": "Renamed"}
                        """))
                .hasStatus(HttpStatus.PRECONDITION_REQUIRED)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .extractingPath("$.detail")
                .isEqualTo("If-Match: * is not accepted. Send the ETag from the latest read of the resource");
        verifyNoInteractions(service);
    }

    @Test
    void renamingWithWeakEntityTagReturns412() {
        assertThat(mvc.put()
                        .uri("/api/v1/categories/1")
                        .header(HttpHeaders.IF_MATCH, "W/\"1\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"name": "Renamed"}
                        """))
                .hasStatus(HttpStatus.PRECONDITION_FAILED)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .extractingPath("$.detail")
                .isEqualTo("If-Match does not match the current version of the resource");
        verifyNoInteractions(service);
    }

    @Test
    void renamingWithOutdatedVersionReturns412() {
        when(service.update(1L, RENAME_REQUEST, 1L)).thenThrow(new VersionMismatchException("Version mismatch"));

        assertThat(mvc.put()
                        .uri("/api/v1/categories/1")
                        .header(HttpHeaders.IF_MATCH, EntityTags.of(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"name": "Renamed"}
                        """))
                .hasStatus(HttpStatus.PRECONDITION_FAILED)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .extractingPath("$.detail")
                .isEqualTo("Version mismatch");
    }

    @Test
    void categoryIsDeactivatedAndReturns204() {
        assertThat(mvc.delete().uri("/api/v1/categories/1").header(HttpHeaders.IF_MATCH, EntityTags.of(1L)))
                .hasStatus(HttpStatus.NO_CONTENT);
        verify(service).deactivate(1L, 1L);
    }

    @Test
    void deactivationWithoutIfMatchReturns428() {
        assertThat(mvc.delete().uri("/api/v1/categories/1"))
                .hasStatus(HttpStatus.PRECONDITION_REQUIRED)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .extractingPath("$.detail")
                .isEqualTo("If-Match header is required. Send the ETag from the latest read of the resource");
        verifyNoInteractions(service);
    }
}
