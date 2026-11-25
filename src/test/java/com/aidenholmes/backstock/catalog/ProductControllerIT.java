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
    void activeProductWithZeroStockIsListed() {
        Product book = productRepository.save(
                new Product("Book", "BOOK", null, new BigDecimal("10.00"), 0, saveBooks(), null));

        assertThat(mvc.get().uri("/api/v1/products"))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.content[0].id")
                .asNumber()
                .satisfies(id -> assertThat(id.longValue()).isEqualTo(book.getId()));
    }

    @Test
    void pageBeyondLastProductReturnsEmptyContentAndTotalCount() {
        Category books = saveBooks();
        productRepository.save(newProduct("BOOK", books));
        productRepository.save(newProduct("ANOTHER-BOOK", books));

        MvcTestResult page = mvc.get()
                .uri("/api/v1/products")
                .queryParam("size", "1")
                .queryParam("page", "2")
                .exchange();

        assertThat(page)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.content")
                .asArray()
                .isEmpty();
        assertThat(page).bodyJson().extractingPath("$.page.totalElements").isEqualTo(2);
        assertThat(page).bodyJson().extractingPath("$.page.number").isEqualTo(2);
        assertThat(page).bodyJson().extractingPath("$.page.size").isEqualTo(1);
        assertThat(page).bodyJson().extractingPath("$.page.totalPages").isEqualTo(2);
    }

    @Test
    void pageBeyondFilteredResultsReturnsEmptyContentAndMatchingTotal() {
        Category books = saveBooks();
        Category phones = categoryRepository.save(new Category("Phones", "phones"));
        productRepository.save(newProduct("NINE", books, new BigDecimal("9.00")));
        productRepository.save(newProduct("TEN", books, new BigDecimal("10.00")));
        productRepository.save(newProduct("TWENTY", books, new BigDecimal("20.00")));
        productRepository.save(newProduct("TWENTY-ONE", books, new BigDecimal("21.00")));
        productRepository.save(newProduct("FIFTEEN", phones, new BigDecimal("15.00")));

        MvcTestResult page = mvc.get()
                .uri("/api/v1/products")
                .queryParam("categoryId", Objects.requireNonNull(books.getId()).toString())
                .queryParam("minPrice", "10")
                .queryParam("maxPrice", "20")
                .queryParam("page", "2")
                .queryParam("size", "1")
                .exchange();

        assertThat(page)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.content")
                .asArray()
                .isEmpty();
        assertThat(page).bodyJson().extractingPath("$.page.totalElements").isEqualTo(2);
        assertThat(page).bodyJson().extractingPath("$.page.number").isEqualTo(2);
        assertThat(page).bodyJson().extractingPath("$.page.size").isEqualTo(1);
        assertThat(page).bodyJson().extractingPath("$.page.totalPages").isEqualTo(2);
    }

    @Test
    void unknownCategoryFilterReturnsEmptyPage() {
        productRepository.save(newProduct("BOOK", saveBooks()));

        var json = assertThat(mvc.get()
                        .uri("/api/v1/products")
                        .queryParam("categoryId", String.valueOf(Long.MAX_VALUE))
                        .queryParam("page", "0")
                        .queryParam("size", "7"))
                .hasStatus(HttpStatus.OK)
                .bodyJson();

        json.extractingPath("$.content").asArray().isEmpty();
        json.extractingPath("$.page.number").isEqualTo(0);
        json.extractingPath("$.page.size").isEqualTo(7);
        json.extractingPath("$.page.totalElements").isEqualTo(0);
        json.extractingPath("$.page.totalPages").isEqualTo(0);
    }

    @Test
    void stockQuantityInUpdateBodyIsIgnored() {
        Category books = saveBooks();
        Product product = productRepository.save(newProduct("BOOK", books));
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
        long categoryId = Objects.requireNonNull(saveBooks().getId());

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

    private Category saveBooks() {
        return categoryRepository.save(new Category("Books", "books"));
    }

    private static Product newProduct(String sku, Category books) {
        return new Product("Book", sku, null, new BigDecimal("10.00"), 1, books, null);
    }

    private static Product newProduct(String sku, Category books, BigDecimal price) {
        return new Product("Book", sku, null, price, 1, books, null);
    }
}
