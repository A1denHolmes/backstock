package com.aidenholmes.backstock.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.aidenholmes.backstock.common.AbstractIntegrationTest;
import com.aidenholmes.backstock.common.web.EntityTags;
import java.math.BigDecimal;
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
class ProductControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void stockQuantityInUpdateBodyIsIgnored() {
        Category books = categoryRepository.save(new Category("Books", "books"));
        Product product =
                productRepository.save(new Product("Book", "BOOK", null, new BigDecimal("10.00"), 1, books, null));
        long id = Objects.requireNonNull(product.getId());

        assertThat(mvc.put()
                        .uri("/api/v1/products/{id}", id)
                        .header(HttpHeaders.IF_MATCH, EntityTags.of(product.getVersion()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "Phone",
                                "price": 20.00,
                                "stockQuantity": 999,
                                "categoryId": %d
                            }
                        """.formatted(books.getId())))
                .hasStatus(HttpStatus.OK);
        assertThat(productRepository.findByIdAndActiveTrue(id).orElseThrow().getStockQuantity())
                .isEqualTo(1);
    }

    @Test
    void priceSurvivesRoundTripThroughDatabase() {
        long categoryId = Objects.requireNonNull(
                categoryRepository.save(new Category("Books", "books")).getId());

        MvcTestResult created = mvc.post()
                .uri("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "name": "Book",
                        "sku": "BOOK",
                        "price": 19.90,
                        "stockQuantity": 1,
                        "categoryId": %d
                    }
                """.formatted(categoryId))
                .exchange();
        assertThat(created).hasStatus(HttpStatus.CREATED);
        String location = Objects.requireNonNull(created.getResponse().getHeader(HttpHeaders.LOCATION));

        assertThat(mvc.get().uri(location)).hasStatus(HttpStatus.OK).bodyText().contains("\"price\":19.90");
    }
}
