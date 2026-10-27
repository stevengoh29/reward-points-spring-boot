package com.example.rewardspoints.shared.error;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

/**
 * Turns every exception into an RFC 9457 {@link ProblemDetail} carrying a stable {@code code} property.
 *
 * <p>Extends {@link ResponseEntityExceptionHandler} so Spring MVC's own exceptions (405, 415, 404, ...)
 * share the same shape; {@link #handleExceptionInternal} stamps a {@code code} on all of them.
 * Must not reference feature modules, see {@code package-info}.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    static final String CODE = "code";
    static final String ERRORS = "errors";

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Object> handleBusiness(BusinessException ex, WebRequest request) {
        ErrorCode errorCode = ex.getErrorCode();
        log.debug("Business error {}: {}", errorCode.code(), ex.getMessage());
        return problem(toStatus(errorCode.category()), errorCode.code(), ex.getMessage(), null, ex, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        List<ValidationError> errors = ex.getConstraintViolations().stream()
                .map(v -> new ValidationError(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return problem(HttpStatus.BAD_REQUEST, CommonErrorCode.VALIDATION_FAILED.code(), "Validation failed", errors, ex, request);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<Object> handleOptimisticLock(OptimisticLockingFailureException ex, WebRequest request) {
        log.debug("Optimistic lock failure", ex);
        return problem(HttpStatus.CONFLICT, CommonErrorCode.CONCURRENT_MODIFICATION.code(),
                "The resource was modified by another request, please retry", null, ex, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest request) {
        return problem(HttpStatus.BAD_REQUEST, CommonErrorCode.MALFORMED_REQUEST.code(),
                "Invalid value for parameter '" + ex.getName() + "'", null, ex, request);
    }

    /** Last resort: log everything, tell the client nothing about internals. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
        log.error("Unhandled exception", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, CommonErrorCode.INTERNAL_ERROR.code(),
                "An unexpected error occurred", null, ex, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new ValidationError(e.getField(), e.getDefaultMessage()))
                .toList();
        return problem(HttpStatus.BAD_REQUEST, CommonErrorCode.VALIDATION_FAILED.code(), "Validation failed", errors, ex, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ValidationError> errors = ex.getParameterValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream()
                        .map(e -> new ValidationError(r.getMethodParameter().getParameterName(), e.getDefaultMessage())))
                .toList();
        return problem(HttpStatus.BAD_REQUEST, CommonErrorCode.VALIDATION_FAILED.code(), "Validation failed", errors, ex, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return problem(HttpStatus.BAD_REQUEST, CommonErrorCode.MALFORMED_REQUEST.code(),
                "Malformed or unreadable request body", null, ex, request);
    }

    /** Gives Spring MVC's built-in exceptions (405, 415, 404, ...) a {@code code} like everything else. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        // super builds the ProblemDetail itself when body is null, so stamp the code on its result
        ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (response != null && response.getBody() instanceof ProblemDetail problem
                && (problem.getProperties() == null || !problem.getProperties().containsKey(CODE))) {
            problem.setProperty(CODE, defaultCode(statusCode));
        }
        return response;
    }

    private ResponseEntity<Object> problem(
            HttpStatus status, String code, String detail, @Nullable List<ValidationError> errors,
            Exception ex, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty(CODE, code);
        if (errors != null) {
            problem.setProperty(ERRORS, errors);
        }
        return handleExceptionInternal(ex, problem, new HttpHeaders(), status, request);
    }

    private static String defaultCode(HttpStatusCode status) {
        if (status.value() == HttpStatus.NOT_FOUND.value()) {
            return CommonErrorCode.RESOURCE_NOT_FOUND.code();
        }
        HttpStatus resolved = HttpStatus.resolve(status.value());
        return "COMMON_" + (resolved != null ? resolved.name() : "HTTP_" + status.value());
    }

    private static HttpStatus toStatus(ErrorCategory category) {
        return switch (category) {
            case VALIDATION -> HttpStatus.BAD_REQUEST;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case BUSINESS_RULE -> HttpStatus.UNPROCESSABLE_CONTENT;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case RATE_LIMITED -> HttpStatus.TOO_MANY_REQUESTS;
            case INTERNAL -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
