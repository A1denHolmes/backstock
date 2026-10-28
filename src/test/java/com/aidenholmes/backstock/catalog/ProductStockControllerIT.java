package com.aidenholmes.backstock.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.aidenholmes.backstock.common.AbstractIntegrationTest;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@AutoConfigureMockMvc
class ProductStockControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void stockOfActiveProductIsReturned() {
        long id = Objects.requireNonNull(productRepository.save(book()).getId());

        assertThat(mvc.get().uri("/api/v1/products/{id}/stock", id))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .isStrictlyEqualTo("""
                    { "quantity": 10 }
                """);
    }

    @Test
    void stockOfDeactivatedProductReturns404() {
        Product product = book();
        product.deactivate();
        long id = Objects.requireNonNull(productRepository.save(product).getId());

        assertThat(mvc.get().uri("/api/v1/products/{id}/stock", id))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .extractingPath("$.detail")
                .isEqualTo("Product " + id + " not found");
    }

    @Test
    void stockOfUnknownProductReturns404() {
        assertThat(mvc.get().uri("/api/v1/products/2/stock"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .extractingPath("$.detail")
                .isEqualTo("Product 2 not found");
    }

    @Test
    void adjustedStockIsPersisted() {
        long id = Objects.requireNonNull(productRepository.save(book()).getId());

        assertThat(patch(id, -5)).hasStatus(HttpStatus.OK).bodyJson().isStrictlyEqualTo("""
                    { "quantity": 5 }
                """);

        assertThat(mvc.get().uri("/api/v1/products/{id}/stock", id))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .isStrictlyEqualTo("""
                    { "quantity": 5 }
                """);
    }

    @Test
    void decreaseBelowZeroReturns409AndKeepsStock() {
        long id = Objects.requireNonNull(productRepository.save(book()).getId());

        assertThat(patch(id, -20))
                .hasStatus(HttpStatus.CONFLICT)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .extractingPath("$.detail")
                .isEqualTo("""
                        Stock of product %d cannot be changed by -20: current stock is 10, and the result \
                        must be between 0 and 1000000""".formatted(id));
        assertThat(mvc.get().uri("/api/v1/products/{id}/stock", id))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .isStrictlyEqualTo("""
                    { "quantity": 10 }
                """);
    }

    @Test
    void increaseAboveMaximumReturns409AndKeepsStock() {
        long id = Objects.requireNonNull(productRepository.save(book()).getId());

        assertThat(patch(id, Product.STOCK_QUANTITY_MAX))
                .hasStatus(HttpStatus.CONFLICT)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .extractingPath("$.detail")
                .isEqualTo("""
                        Stock of product %d cannot be changed by %d: current stock is 10, and the result \
                        must be between 0 and 1000000""".formatted(id, Product.STOCK_QUANTITY_MAX));
        assertThat(mvc.get().uri("/api/v1/products/{id}/stock", id))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .isStrictlyEqualTo("""
                    { "quantity": 10 }
                """);
    }

    @Test
    void stockAdjustmentOfDeactivatedProductReturns404() {
        Product product = book();
        product.deactivate();
        long id = Objects.requireNonNull(productRepository.save(product).getId());

        assertThat(patch(id, -5))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .extractingPath("$.detail")
                .isEqualTo("Product " + id + " not found");
    }

    @Test
    void stockAdjustmentOfUnknownProductReturns404() {
        assertThat(patch(2, -5))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .extractingPath("$.detail")
                .isEqualTo("Product 2 not found");
    }

    @Test
    void stockAdjustmentKeepsProductEntityTagAndBody() throws Exception {
        long id = Objects.requireNonNull(productRepository.save(book()).getId());

        MvcTestResult before = mvc.get().uri("/api/v1/products/{id}", id).exchange();
        String entityTag = Objects.requireNonNull(before.getResponse().getHeader(HttpHeaders.ETAG));
        String body = before.getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(patch(id, -5))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.quantity")
                .isEqualTo(5);
        MvcTestResult after = mvc.get().uri("/api/v1/products/{id}", id).exchange();

        assertThat(after).hasHeader(HttpHeaders.ETAG, entityTag);
        assertThat(after).bodyJson().isStrictlyEqualTo(body);
    }

    private Product book() {
        return new Product(
                "Book",
                "BOOK",
                null,
                new BigDecimal("10.00"),
                10,
                categoryRepository.save(new Category("Books", "books")),
                null);
    }

    private MvcTestResult patch(long id, int delta) {
        return mvc.patch()
                .uri("/api/v1/products/{id}/stock", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "delta": %d }
                """.formatted(delta))
                .exchange();
    }
}
