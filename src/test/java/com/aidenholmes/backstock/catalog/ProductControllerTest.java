package com.aidenholmes.backstock.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aidenholmes.backstock.common.ConflictException;
import com.aidenholmes.backstock.common.NotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
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
import tools.jackson.databind.json.JsonMapper;

@ActiveProfiles("test")
@WebMvcTest(ProductController.class)
class ProductControllerTest {

    private static final Instant NOW = Instant.parse("2026-10-19T10:00:00Z");
    private static final CreateProductRequest CREATE_BOOK =
            new CreateProductRequest("Book", "BOOK", null, new BigDecimal("10.00"), 1, 1L, null);
    private static final String CREATE_BOOK_JSON = """
            {
                "name": "Book",
                "sku": "BOOK",
                "description": null,
                "price": 10.00,
                "stockQuantity": 1,
                "categoryId": 1,
                "imageUrl": null
            }
        """;
    private static final ProductResponse BOOK_RESPONSE =
            new ProductResponse(1L, "Book", "BOOK", null, new BigDecimal("10.00"), 1, 1L, null, NOW, NOW);
    private static final String BOOK_RESPONSE_JSON = """
            {
                "id": 1,
                "name": "Book",
                "sku": "BOOK",
                "description": null,
                "price": 10.00,
                "stockQuantity": 1,
                "categoryId": 1,
                "imageUrl": null,
                "createdAt": "2026-10-19T10:00:00Z",
                "updatedAt": "2026-10-19T10:00:00Z"
            }
        """;
    private static final UpdateProductRequest UPDATE_TO_PHONE = new UpdateProductRequest(
            "Phone", "Description", new BigDecimal("20.00"), 2L, "https://example.com/picture.jpg");
    private static final String UPDATE_TO_PHONE_JSON = """
            {
                "name": "Phone",
                "description": "Description",
                "price": 20.00,
                "categoryId": 2,
                "imageUrl": "https://example.com/picture.jpg"
            }
        """;

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ProductService service;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void productIsReturnedById() {
        when(service.findById(1L)).thenReturn(BOOK_RESPONSE);

        assertThat(mvc.get().uri("/api/v1/products/1"))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .isStrictlyEqualTo(BOOK_RESPONSE_JSON);
    }

    @Test
    void priceIsSerializedWithTwoDecimals() {
        when(service.findById(1L)).thenReturn(BOOK_RESPONSE);

        assertThat(mvc.get().uri("/api/v1/products/1")).bodyText().contains("\"price\":10.00");
    }

    @Test
    void unknownProductReturns404() {
        when(service.findById(1L)).thenThrow(new NotFoundException("Product 1 not found"));

        assertThat(mvc.get().uri("/api/v1/products/1"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .extractingPath("$.detail")
                .isEqualTo("Product 1 not found");
    }

    @Test
    void invalidIdReturns400() {
        assertThat(mvc.get().uri("/api/v1/products/abc"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void productIsCreatedAndReturns201WithLocationAndBody() {
        when(service.create(CREATE_BOOK)).thenReturn(BOOK_RESPONSE);

        assertThat(mvc.post()
                        .uri("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BOOK_JSON))
                .hasStatus(HttpStatus.CREATED)
                .hasHeader(HttpHeaders.LOCATION, "http://localhost/api/v1/products/1")
                .bodyJson()
                .isStrictlyEqualTo(BOOK_RESPONSE_JSON);
    }

    @Test
    void productWithoutOptionalFieldsIsCreated() {
        when(service.create(CREATE_BOOK)).thenReturn(BOOK_RESPONSE);

        assertThat(mvc.post()
                        .uri("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "Book",
                                "sku": "BOOK",
                                "price": 10.00,
                                "stockQuantity": 1,
                                "categoryId": 1
                            }
                        """))
                .hasStatus(HttpStatus.CREATED)
                .hasHeader(HttpHeaders.LOCATION, "http://localhost/api/v1/products/1")
                .bodyJson()
                .isStrictlyEqualTo(BOOK_RESPONSE_JSON);
    }

    @Test
    void takenSkuReturns409() {
        when(service.create(CREATE_BOOK)).thenThrow(new ConflictException("Product with SKU \"BOOK\" already exists"));

        assertThat(mvc.post()
                        .uri("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BOOK_JSON))
                .hasStatus(HttpStatus.CONFLICT)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .extractingPath("$.detail")
                .isEqualTo("Product with SKU \"BOOK\" already exists");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"name", "sku", "price", "stockQuantity", "categoryId"})
    void missingRequiredFieldOnCreateReturns400(String field) {
        Map<String, Object> body = new HashMap<>(validCreateBody());
        body.remove(field);

        assertFieldError(post(body), field);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"book", "-BOOK", "BOOK-", "BO--OK", "BOOK 1", "КНИГА"})
    void invalidSkuReturns400(String sku) {
        assertFieldError(post(createBodyWith("sku", sku)), "sku");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidPrices")
    void invalidPriceOnCreateReturns400(String price) {
        assertFieldError(post(createBodyWith("price", new BigDecimal(price))), "price");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidUrls")
    void invalidImageUrlOnCreateReturns400(String imageUrl) {
        assertFieldError(post(createBodyWith("imageUrl", imageUrl)), "imageUrl");
    }

    @Test
    void emptyDescriptionOnCreateReturns400() {
        assertFieldError(post(createBodyWith("description", "")), "description");
    }

    @Test
    void invisibleCharacterInNameOnCreateReturns400() {
        assertFieldError(post(createBodyWith("name", "\u200B")), "name");
    }

    @Test
    void blankNameOnCreateReturns400() {
        assertFieldError(post(createBodyWith("name", "   ")), "name");
    }

    @Test
    void negativeStockQuantityReturns400() {
        assertFieldError(post(createBodyWith("stockQuantity", -1)), "stockQuantity");
    }

    @Test
    void productIsUpdatedAndReturns200WithBody() {
        when(service.update(1L, UPDATE_TO_PHONE))
                .thenReturn(new ProductResponse(
                        1L,
                        "Phone",
                        "BOOK",
                        "Description",
                        new BigDecimal("20.00"),
                        1,
                        2L,
                        "https://example.com/picture.jpg",
                        NOW,
                        NOW));

        assertThat(mvc.put()
                        .uri("/api/v1/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_TO_PHONE_JSON))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .isStrictlyEqualTo("""
                    {
                        "id": 1,
                        "name": "Phone",
                        "sku": "BOOK",
                        "description": "Description",
                        "price": 20.00,
                        "stockQuantity": 1,
                        "categoryId": 2,
                        "imageUrl": "https://example.com/picture.jpg",
                        "createdAt": "2026-10-19T10:00:00Z",
                        "updatedAt": "2026-10-19T10:00:00Z"
                    }
                """);
    }

    @Test
    void updateOfUnknownProductReturns404() {
        when(service.update(1L, UPDATE_TO_PHONE)).thenThrow(new NotFoundException("Product 1 not found"));

        assertThat(mvc.put()
                        .uri("/api/v1/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_TO_PHONE_JSON))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .extractingPath("$.detail")
                .isEqualTo("Product 1 not found");
    }

    @Test
    void invisibleCharacterInNameOnUpdateReturns400() {
        assertFieldError(put(updateBodyWith("name", "\u200B")), "name");
    }

    @Test
    void blankNameOnUpdateReturns400() {
        assertFieldError(put(updateBodyWith("name", "   ")), "name");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"name", "price", "categoryId"})
    void missingRequiredFieldOnUpdateReturns400(String field) {
        Map<String, Object> body = new HashMap<>(validUpdateBody());
        body.remove(field);

        assertFieldError(put(body), field);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidPrices")
    void invalidPriceOnUpdateReturns400(String price) {
        assertFieldError(put(updateBodyWith("price", new BigDecimal(price))), "price");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidUrls")
    void invalidImageUrlOnUpdateReturns400(String imageUrl) {
        assertFieldError(put(updateBodyWith("imageUrl", imageUrl)), "imageUrl");
    }

    @Test
    void emptyDescriptionOnUpdateReturns400() {
        assertFieldError(put(updateBodyWith("description", "")), "description");
    }

    @Test
    void productIsDeactivatedAndReturns204() {
        assertThat(mvc.delete().uri("/api/v1/products/1")).hasStatus(HttpStatus.NO_CONTENT);
        verify(service).deactivate(1L);
    }

    private static Stream<String> invalidPrices() {
        return Stream.of("0", "-1", "10.001", "1234567890123456");
    }

    private static Stream<String> invalidUrls() {
        return Stream.of("http://example.com/a.jpg", "", "not a url");
    }

    private static Map<String, Object> validCreateBody() {
        return Map.of(
                "name", "Book", "sku", "BOOK", "price", new BigDecimal("10.00"), "stockQuantity", 1, "categoryId", 1);
    }

    private static Map<String, Object> createBodyWith(String field, Object value) {
        Map<String, Object> body = new HashMap<>(validCreateBody());
        body.put(field, value);
        return body;
    }

    private MvcTestResult post(Map<String, Object> body) {
        return mvc.post()
                .uri("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(body))
                .exchange();
    }

    private static Map<String, Object> validUpdateBody() {
        return Map.of("name", "Phone", "price", new BigDecimal("10.00"), "categoryId", 2L);
    }

    private static Map<String, Object> updateBodyWith(String field, Object value) {
        Map<String, Object> body = new HashMap<>(validUpdateBody());
        body.put(field, value);
        return body;
    }

    private MvcTestResult put(Map<String, Object> body) {
        return mvc.put()
                .uri("/api/v1/products/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(body))
                .exchange();
    }

    private static void assertFieldError(MvcTestResult result, String field) {
        assertThat(result)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .extractingPath("$.errors[0].field")
                .isEqualTo(field);
    }
}
