package com.aidenholmes.backstock.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.aidenholmes.backstock.common.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

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
}
