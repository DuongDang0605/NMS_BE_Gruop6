package com.mvc.nms_be.exception;

import com.mvc.nms_be.controller.UserController;
import com.mvc.nms_be.dto.response.ApiErrorResponse;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
// Keep the shared handler scoped to User until the team adopts it in other modules.
@RestControllerAdvice(assignableTypes = UserController.class)
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleNotFound(ResourceNotFoundException ex, WebRequest request) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<Object> handleConflict(ResourceConflictException ex, WebRequest request) {
        Map<String, String> errors = ex.getField() == null
                ? Map.of() : Map.of(ex.getField(), ex.getMessage());
        return error(HttpStatus.CONFLICT, ex.getMessage(), errors, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fieldError ->
                errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage()));
        return error(HttpStatus.BAD_REQUEST, "Validation failed", errors, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (ex.isForReturnValue()) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", Map.of(), request);
        }
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getParameterValidationResults().forEach(result -> {
            if (result instanceof ParameterErrors parameterErrors) {
                parameterErrors.getFieldErrors().forEach(fieldError ->
                        errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage()));
            } else {
                String name = result.getMethodParameter().getParameterName();
                result.getResolvableErrors().forEach(validationError ->
                        errors.putIfAbsent(name == null ? "parameter" : name, validationError.getDefaultMessage()));
            }
        });
        return error(HttpStatus.BAD_REQUEST, "Validation failed", errors, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String field = "request";
            for (var node : violation.getPropertyPath()) {
                if (node.getName() != null) {
                    field = node.getName();
                }
            }
            errors.putIfAbsent(field, violation.getMessage());
        });
        return error(HttpStatus.BAD_REQUEST, "Validation failed", errors, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Request body is missing or contains invalid JSON or field types",
                Map.of(), request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String field = ex.getPropertyName() == null ? "parameter" : ex.getPropertyName();
        return error(HttpStatus.BAD_REQUEST, "Invalid parameter type",
                Map.of(field, "Value has an invalid type"), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrity(DataIntegrityViolationException ex, WebRequest request) {
        return error(HttpStatus.CONFLICT, "Data conflicts with an existing record or a database constraint",
                Map.of(), request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail detail = body instanceof ProblemDetail problemDetail ? problemDetail
                : ex instanceof ErrorResponse errorResponse ? errorResponse.getBody() : null;
        String message = status.is5xxServerError() ? "An unexpected error occurred"
                : detail != null && detail.getDetail() != null
                ? detail.getDetail() : "Request could not be processed";
        return new ResponseEntity<>(body(status, message, Map.of(), request), headers, status);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
        log.error("Unexpected API failure", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", Map.of(), request);
    }

    private ResponseEntity<Object> error(HttpStatus status, String message,
            Map<String, String> errors, WebRequest request) {
        return ResponseEntity.status(status).body(body(status, message, errors, request));
    }

    private ApiErrorResponse body(HttpStatusCode status, String message,
            Map<String, String> errors, WebRequest request) {
        String path = ((ServletWebRequest) request).getRequest().getRequestURI();
        HttpStatus resolved = HttpStatus.resolve(status.value());
        return new ApiErrorResponse(Instant.now(), status.value(),
                resolved == null ? "Error" : resolved.getReasonPhrase(), message, path, errors);
    }
}
