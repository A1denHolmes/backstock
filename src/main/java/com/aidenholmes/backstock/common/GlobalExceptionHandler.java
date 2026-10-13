package com.aidenholmes.backstock.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
@Slf4j
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String UNEXPECTED_ERROR_DETAIL = "An unexpected error occurred";

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail handleNotFound(NotFoundException ex) {
        return forDomainException(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(ConflictException.class)
    ProblemDetail handleConflict(ConflictException ex) {
        return forDomainException(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(BusinessRuleException.class)
    ProblemDetail handleBusinessRule(BusinessRuleException ex) {
        return forDomainException(HttpStatus.UNPROCESSABLE_CONTENT, ex);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, UNEXPECTED_ERROR_DETAIL);
    }

    private static ProblemDetail forDomainException(HttpStatus status, DomainException ex) {
        return ProblemDetail.forStatusAndDetail(status, ex.getMessage());
    }
}
