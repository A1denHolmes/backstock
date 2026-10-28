package com.aidenholmes.backstock.common.error;

import com.aidenholmes.backstock.common.web.PreconditionRequiredException;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
@Slf4j
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String UNEXPECTED_ERROR_DETAIL = "An unexpected error occurred";
    private static final String DATA_INTEGRITY_VIOLATION_DETAIL =
            "The request conflicts with the current state of the data";
    private static final String OPTIMISTIC_LOCKING_FAILURE_DETAIL =
            "The resource was modified by another request. Reload it and try again";
    private static final String INVALID_FIELD_DEFAULT_MESSAGE = "is invalid";

    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<InvalidField> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new InvalidField(
                        error.getField(),
                        Objects.requireNonNullElse(error.getDefaultMessage(), INVALID_FIELD_DEFAULT_MESSAGE)))
                .toList();
        ProblemDetail body = ex.getBody();
        body.setProperty("errors", errors);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

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

    @ExceptionHandler(VersionMismatchException.class)
    ProblemDetail handleVersionMismatch(VersionMismatchException ex) {
        return forDomainException(HttpStatus.PRECONDITION_FAILED, ex);
    }

    @ExceptionHandler(PreconditionRequiredException.class)
    ProblemDetail handlePreconditionRequired(PreconditionRequiredException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.PRECONDITION_REQUIRED, ex.getMessage());
    }

    // NOT NULL violations also end up here, although they indicate a bug rather than a conflict.
    // They are already logged by Hibernate.
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleDataIntegrityViolation() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, DATA_INTEGRITY_VIOLATION_DETAIL);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail handleOptimisticLockingFailure() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, OPTIMISTIC_LOCKING_FAILURE_DETAIL);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, UNEXPECTED_ERROR_DETAIL);
    }

    private static ProblemDetail forDomainException(HttpStatus status, DomainException ex) {
        return ProblemDetail.forStatusAndDetail(status, ex.getMessage());
    }

    record InvalidField(String field, String message) {}
}
