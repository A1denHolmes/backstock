package com.aidenholmes.backstock.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.aidenholmes.backstock.common.ConflictException;
import com.aidenholmes.backstock.common.NotFoundException;
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
@WebMvcTest(ProductStockController.class)
class ProductStockControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ProductService service;

    @Test
    void stockIsReturnedByProductId() {
        when(service.findStock(1L)).thenReturn(new StockResponse(10));

        assertThat(mvc.get().uri("/api/v1/products/1/stock"))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .isStrictlyEqualTo("""
                    { "quantity": 10 }
                """);
    }

    @Test
    void stockResponseHasNoEntityTag() {
        when(service.findStock(1L)).thenReturn(new StockResponse(10));

        assertThat(mvc.get().uri("/api/v1/products/1/stock"))
                .hasStatus(HttpStatus.OK)
                .doesNotContainHeader(HttpHeaders.ETAG);
    }

    @Test
    void unknownProductStockReturns404() {
        when(service.findStock(1L)).thenThrow(new NotFoundException("Product 1 not found"));

        assertThat(mvc.get().uri("/api/v1/products/1/stock"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .extractingPath("$.detail")
                .isEqualTo("Product 1 not found");
    }

    @Test
    void invalidIdReturns400() {
        assertThat(mvc.get().uri("/api/v1/products/abc/stock"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void stockIsAdjustedAndReturnsResultingStock() {
        when(service.adjustStock(1L, new AdjustStockRequest(-5))).thenReturn(new StockResponse(5));

        assertThat(patch(-5)).hasStatus(HttpStatus.OK).bodyJson().isStrictlyEqualTo("""
                    { "quantity": 5 }
                """);
    }

    @Test
    void missingDeltaReturns400() {
        assertThat(mvc.patch()
                        .uri("/api/v1/products/1/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {}
                        """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .extractingPath("$.errors[0].field")
                .isEqualTo("delta");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(ints = {-Product.STOCK_QUANTITY_MAX - 1, Product.STOCK_QUANTITY_MAX + 1})
    void outOfRangeDeltaReturns400(int delta) {
        assertThat(patch(delta))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .extractingPath("$.errors[0].field")
                .isEqualTo("delta");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(ints = {-Product.STOCK_QUANTITY_MAX, Product.STOCK_QUANTITY_MAX})
    void deltaAtRangeBoundIsAccepted(int delta) {
        when(service.adjustStock(1L, new AdjustStockRequest(delta))).thenReturn(new StockResponse(0));

        assertThat(patch(delta)).hasStatus(HttpStatus.OK);
    }

    @Test
    void fractionalDeltaReturns400() {
        assertThat(mvc.patch()
                        .uri("/api/v1/products/1/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            { "delta": 1.8 }
                        """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void stockAdjustmentOfUnknownProductReturns404() {
        when(service.adjustStock(1L, new AdjustStockRequest(-5)))
                .thenThrow(new NotFoundException("Product 1 not found"));

        assertThat(patch(-5))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .extractingPath("$.detail")
                .isEqualTo("Product 1 not found");
    }

    @Test
    void stockAdjustmentOutOfRangeReturns409() {
        when(service.adjustStock(1L, new AdjustStockRequest(-5)))
                .thenThrow(new ConflictException("Stock is out of range"));

        assertThat(patch(-5))
                .hasStatus(HttpStatus.CONFLICT)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .extractingPath("$.detail")
                .isEqualTo("Stock is out of range");
    }

    private MvcTestResult patch(int delta) {
        return mvc.patch()
                .uri("/api/v1/products/1/stock")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "delta": %d }
                """.formatted(delta))
                .exchange();
    }
}
