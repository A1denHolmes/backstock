package com.aidenholmes.backstock.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.aidenholmes.backstock.common.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"'/api/v1/categories/{id}'", "'/api/v1/products/{id}'"})
    void ifMatchIsRequiredForEntityUpdateAndDeactivation(String path) {
        assertThat(mvc.get().uri("/v3/api-docs"))
                .bodyJson()
                .extractingPath("$.paths[%s].put.parameters[?(@.name == 'If-Match')].required".formatted(path))
                .asArray()
                .containsExactly(true);
        assertThat(mvc.get().uri("/v3/api-docs"))
                .bodyJson()
                .extractingPath("$.paths[%s].delete.parameters[?(@.name == 'If-Match')].required".formatted(path))
                .asArray()
                .containsExactly(true);
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
