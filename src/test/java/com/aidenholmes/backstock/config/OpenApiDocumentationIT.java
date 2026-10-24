package com.aidenholmes.backstock.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.aidenholmes.backstock.common.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@AutoConfigureMockMvc
class OpenApiDocumentationIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void apiDocsDescribeCategoryEndpoints() {
        assertThat(mvc.get().uri("/v3/api-docs"))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .hasPath("$.paths['/api/v1/categories']")
                .hasPath("$.paths['/api/v1/categories/{id}']");
    }

    @Test
    void apiDocsDescribeProductEndpoints() {
        assertThat(mvc.get().uri("/v3/api-docs"))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .hasPath("$.paths['/api/v1/products']")
                .hasPath("$.paths['/api/v1/products/{id}']");
    }

    @Test
    void apiDocsContainConfiguredInfo() {
        assertThat(mvc.get().uri("/v3/api-docs"))
                .bodyJson()
                .extractingPath("$.info.title")
                .isEqualTo("Backstock API");
    }

    @Test
    void createdProductResponseHasBody() {
        assertThat(mvc.get().uri("/v3/api-docs"))
                .bodyJson()
                .hasPath("$.paths['/api/v1/products'].post.responses['201'].content");
    }
}
