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

    private static final String PRODUCT_LISTING = "$.paths['/api/v1/products'].get";

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
    void apiDocsDescribeProductListingParameters() {
        var docs = assertThat(mvc.get().uri("/v3/api-docs"))
                .hasStatus(HttpStatus.OK)
                .bodyJson();
        String parameters = PRODUCT_LISTING + ".parameters";
        docs.extractingPath(parameters + "[?(@.in == 'query')].name").asArray().contains("page", "size", "sort");
        docs.extractingPath(parameters + "[?(@.in == 'query' && @.required == false)].name")
                .asArray()
                .contains("categoryId", "minPrice", "maxPrice");
        String sort = parameters + "[?(@.in == 'query' && @.name == 'sort')].schema";
        docs.extractingPath(sort + ".type").asArray().containsExactly("array");
        docs.extractingPath(sort + ".items.type").asArray().containsExactly("string");
    }

    @Test
    void apiDocsDescribeProductListingPage() {
        var docs = assertThat(mvc.get().uri("/v3/api-docs"))
                .hasStatus(HttpStatus.OK)
                .bodyJson();
        String response = PRODUCT_LISTING + ".responses['200']";
        String properties = "$.components.schemas.PagedModelProductResponse.properties";

        docs.extractingPath(response + ".content['application/json'].schema['$ref']")
                .isEqualTo("#/components/schemas/PagedModelProductResponse");
        docs.extractingPath(properties + ".content.type").isEqualTo("array");
        docs.extractingPath(properties + ".content.items['$ref']").isEqualTo("#/components/schemas/ProductResponse");
        docs.extractingPath(properties + ".page['$ref']").isEqualTo("#/components/schemas/PageMetadata");
        docs.extractingPath("$.components.schemas.PageMetadata.properties")
                .asMap()
                .containsKeys("number", "size", "totalElements", "totalPages");
    }

    @Test
    void apiDocsDescribeInvalidProductListingRequest() {
        assertThat(mvc.get().uri("/v3/api-docs"))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath(
                        PRODUCT_LISTING + ".responses['400'].content['application/problem+json'].schema['$ref']")
                .isEqualTo("#/components/schemas/ProblemDetail");
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
