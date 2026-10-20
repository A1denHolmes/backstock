package com.aidenholmes.backstock.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aidenholmes.backstock.common.ConflictException;
import com.aidenholmes.backstock.common.NotFoundException;
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
        when(service.findById(1L)).thenReturn(new CategoryResponse(1L, "Books", "books", NOW, NOW));

        assertThat(mvc.get().uri("/api/v1/categories/1"))
                .hasStatus(HttpStatus.OK)
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
    void categoryIsCreatedAndReturns201WithLocationAndBody() {
        when(service.create(new CreateCategoryRequest("Books", "books")))
                .thenReturn(new CategoryResponse(1L, "Books", "books", NOW, NOW));

        assertThat(mvc.post()
                        .uri("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"name": "Books", "slug": "books"}
                        """))
                .hasStatus(HttpStatus.CREATED)
                .hasHeader(HttpHeaders.LOCATION, "http://localhost/api/v1/categories/1")
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
        when(service.create(new CreateCategoryRequest("Books", "books")))
                .thenReturn(new CategoryResponse(1L, "Books", "books", NOW, NOW));

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
    void categoryIsRenamedAndReturns200WithBody() {
        when(service.update(1L, new UpdateCategoryRequest("Renamed")))
                .thenReturn(new CategoryResponse(1L, "Renamed", "books", NOW, NOW));

        assertThat(mvc.put()
                        .uri("/api/v1/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"name": "Renamed"}
                        """))
                .hasStatus(HttpStatus.OK)
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"name": "   "}
                        """))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void renamingOfUnknownCategoryReturns404() {
        when(service.update(1L, new UpdateCategoryRequest("Renamed")))
                .thenThrow(new NotFoundException("Category 1 not found"));

        assertThat(mvc.put()
                        .uri("/api/v1/categories/1")
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
    void categoryIsDeactivatedAndReturns204() {
        assertThat(mvc.delete().uri("/api/v1/categories/1")).hasStatus(HttpStatus.NO_CONTENT);
        verify(service).deactivate(1L);
    }
}
