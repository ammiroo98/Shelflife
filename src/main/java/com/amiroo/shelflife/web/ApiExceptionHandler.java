package com.amiroo.shelflife.web;

import java.util.List;

import com.amiroo.shelflife.lending.BorrowingException;
import com.amiroo.shelflife.lending.LoanNotFoundException;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BorrowingException.class)
    public ResponseEntity<ApiError> handleBorrowing(BorrowingException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(error(HttpStatus.CONFLICT, exception.getMessage(), List.of()));
    }

    @ExceptionHandler(LoanNotFoundException.class)
    public ResponseEntity<ApiError> handleLoanNotFound(LoanNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(error(HttpStatus.NOT_FOUND, exception.getMessage(), List.of()));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldError> fields = exception.getBindingResult().getFieldErrors().stream()
                .map(field -> new FieldError(field.getField(), field.getDefaultMessage()))
                .toList();
        return super.handleExceptionInternal(exception, error(status, "Request validation failed.", fields),
                headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String message = reason(status);
        if (exception instanceof ResponseStatusException responseStatus && responseStatus.getReason() != null) {
            message = responseStatus.getReason();
        } else if (exception instanceof HttpMessageNotReadableException) {
            message = "Malformed or missing JSON request body.";
        } else if (exception instanceof MethodArgumentTypeMismatchException mismatch) {
            message = "Invalid value for parameter '" + mismatch.getName() + "'.";
        }
        return super.handleExceptionInternal(exception, error(status, message, List.of()), headers, status, request);
    }

    private static ApiError error(HttpStatusCode status, String message, List<FieldError> fields) {
        return new ApiError(status.value(), reason(status), message, fields);
    }

    private static String reason(HttpStatusCode status) {
        HttpStatus knownStatus = HttpStatus.resolve(status.value());
        return knownStatus == null ? "HTTP error" : knownStatus.getReasonPhrase();
    }

    public record ApiError(int status, String error, String message,
            @JsonInclude(JsonInclude.Include.NON_EMPTY) List<FieldError> fieldErrors) {
    }

    public record FieldError(String field, String message) {
    }
}
