package com.aidenholmes.backstock.common;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @Test
    void invalidFieldReturns400WithFieldError() {
        MvcTestResult result = mvc.post()
                .uri("/test/validated")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": ""}
                    """)
                .exchange();

        assertThat(result)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .extractingPath("$.errors[0].field")
                .isEqualTo("name");
        assertThat(result).bodyJson().extractingPath("$.errors[0].message").isNotEmpty();
    }

    @Test
    void malformedJsonReturns400WithoutFieldErrors() {
        MvcTestResult result = mvc.post()
                .uri("/test/validated")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":
                    """)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().doesNotHavePath("$.errors");
        assertThat(result).bodyText().doesNotContain("line:");
    }

    @Test
    void dataIntegrityViolationReturns409WithoutInternalDetails() {
        MvcTestResult result = mvc.post().uri("/test/duplicate").exchange();

        assertThat(result)
                .hasStatus(HttpStatus.CONFLICT)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .extractingPath("$.detail")
                .isEqualTo("The request conflicts with the current state of the data");
        assertThat(result).bodyText().doesNotContain("categories_slug_key");
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

        @PostMapping("/test/validated")
        void validated(@Valid @RequestBody ValidatedRequest request) {}

        @PostMapping("/test/duplicate")
        void duplicate() {
            throw new DataIntegrityViolationException(
                    "Duplicate key value violates unique constraint \"categories_slug_key\"");
        }
    }

    record ValidatedRequest(@NotBlank String name) {}
}
