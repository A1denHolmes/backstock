package com.aidenholmes.backstock.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.aidenholmes.backstock.common.AbstractIntegrationTest;
import java.math.BigDecimal;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

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
}
