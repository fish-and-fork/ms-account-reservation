package com.example.ms_account_reservation.exception;

import com.example.dto.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

@ControllerAdvice
@Slf4j
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request) {

        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .toList();

        ApiError error = ApiError.VALIDATION_ERROR;
        log.debug("Validation failed: {}", details, ex);

        ErrorResponse body = error.toResponse()
                .details(details);

        return ResponseEntity.status(error.getStatusCode()).body(body);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String paramName = e.getName();
        Object value = e.getValue();
        String requiredType = e.getRequiredType() != null
                ? e.getRequiredType().getSimpleName()
                : "unknown";

        String detail = String.format("Параметр '%s' со значением '%s' не может быть преобразован в тип %s",
                paramName, value, requiredType);

        ApiError error = ApiError.INVALID_REQUEST_PARAMETER;
        log.debug("Type mismatch: {}", detail, e);

        ErrorResponse body = error.toResponse()
                .details(List.of(detail));

        return ResponseEntity.status(error.getStatusCode()).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException e) {
        List<String> details = e.getConstraintViolations().stream()
                .map(violation -> {
                    String path = violation.getPropertyPath().toString();
                    String field = path.contains(".") // например: "searchClients.size"
                            ? path.substring(path.lastIndexOf('.') + 1)
                            : path;
                    return field + ": " + violation.getMessage();
                })
                .toList();

        ApiError error = ApiError.VALIDATION_ERROR;
        log.warn("Constraint violation: {}", details);

        ErrorResponse body = error.toResponse()
                .details(details);

        return ResponseEntity.status(error.getStatusCode()).body(body);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException e) {
        ApiError error = e.getApiError();
        log.debug("API error occurred: {}", error.getErrorCode(), e);
        return ResponseEntity
                .status(error.getStatusCode())
                .body(error.toResponse());
    }
}
