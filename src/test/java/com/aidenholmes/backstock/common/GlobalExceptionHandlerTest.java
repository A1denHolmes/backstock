package com.aidenholmes.backstock.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(GlobalExceptionHandlerTest.ThrowingController.class)
@Import(GlobalExceptionHandlerTest.ThrowingController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void notFoundExceptionReturns404WithDetail() {
        assertThat(mvc.get().uri("/test/not-found"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .extractingPath("$.detail")
                .isEqualTo("Content not found");
    }

    @Test
    void conflictExceptionReturns409WithDetail() {
        assertThat(mvc.get().uri("/test/conflict"))
                .hasStatus(HttpStatus.CONFLICT)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .extractingPath("$.detail")
                .isEqualTo("Conflict occurred");
    }

    @Test
    void businessRuleExceptionReturns422WithDetail() {
        assertThat(mvc.get().uri("/test/unprocessable-content"))
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .extractingPath("$.detail")
                .isEqualTo("Unprocessable content");
    }

    @Test
    void unexpectedExceptionReturns500WithoutInternalDetails() {
        MvcTestResult result = mvc.get().uri("/test/unexpected").exchange();

        assertThat(result)
                .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result)
                .bodyJson()
                .doesNotHavePath("$.trace")
                .doesNotHavePath("$.exception")
                .extractingPath("$.detail")
                .isEqualTo("An unexpected error occurred");
        assertThat(result).bodyText().doesNotContain("secret");
    }

    @Test
    void unsupportedMethodReturns405AsProblemDetail() {
        assertThat(mvc.post().uri("/test/not-found"))
                .hasStatus(HttpStatus.METHOD_NOT_ALLOWED)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @RestController
    static class ThrowingController {

        @GetMapping("/test/not-found")
        void notFound() {
            throw new NotFoundException("Content not found");
        }

        @GetMapping("/test/conflict")
        void conflict() {
            throw new ConflictException("Conflict occurred");
        }

        @GetMapping("/test/unprocessable-content")
        void unprocessableContent() {
            throw new BusinessRuleException("Unprocessable content");
        }

        @GetMapping("/test/unexpected")
        void unexpected() {
            throw new IllegalStateException("secret internal detail");
        }
    }
}
