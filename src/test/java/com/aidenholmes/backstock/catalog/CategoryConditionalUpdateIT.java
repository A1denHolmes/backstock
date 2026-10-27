package com.aidenholmes.backstock.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.aidenholmes.backstock.common.AbstractIntegrationTest;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@AutoConfigureMockMvc
class CategoryConditionalUpdateIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private CategoryRepository repository;

    @Test
    void secondRenameWithSameEntityTagReturns412AndKeepsFirstRename() {
        long id = Objects.requireNonNull(
                repository.save(new Category("Books", "books")).getId());

        String entityTag = Objects.requireNonNull(mvc.get()
                .uri("/api/v1/categories/{id}", id)
                .exchange()
                .getResponse()
                .getHeader(HttpHeaders.ETAG));
        assertThat(mvc.put()
                        .uri("/api/v1/categories/{id}", id)
                        .header(HttpHeaders.IF_MATCH, entityTag)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {"name": "Renamed"}
                """))
                .hasStatus(HttpStatus.OK);

        assertThat(mvc.put()
                        .uri("/api/v1/categories/{id}", id)
                        .header(HttpHeaders.IF_MATCH, entityTag)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"name": "RenamedTwice"}
                        """))
                .hasStatus(HttpStatus.PRECONDITION_FAILED)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .extractingPath("$.detail")
                .isEqualTo("Category " + id + " was modified by another request. Reload it and try again");
        assertThat(mvc.get().uri("/api/v1/categories/{id}", id))
                .bodyJson()
                .extractingPath("$.name")
                .isEqualTo("Renamed");
    }

    @Test
    void entityTagFromUpdateIsAcceptedByNextUpdate() {
        long id = Objects.requireNonNull(
                repository.save(new Category("Books", "books")).getId());

        String readTag = Objects.requireNonNull(mvc.get()
                .uri("/api/v1/categories/{id}", id)
                .exchange()
                .getResponse()
                .getHeader(HttpHeaders.ETAG));
        String updatedTag = Objects.requireNonNull(mvc.put()
                .uri("/api/v1/categories/{id}", id)
                .header(HttpHeaders.IF_MATCH, readTag)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "Renamed"}
                """)
                .exchange()
                .getResponse()
                .getHeader(HttpHeaders.ETAG));

        assertThat(mvc.put()
                        .uri("/api/v1/categories/{id}", id)
                        .header(HttpHeaders.IF_MATCH, updatedTag)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {"name": "RenamedTwice"}
                """))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.name")
                .isEqualTo("RenamedTwice");
    }
}
