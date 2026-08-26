package com.fya.credits.exception;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult().getFieldErrors().forEach(error ->
        errors.put(error.getField(), error.getDefaultMessage()));
    return ResponseEntity.badRequest().body(new ApiError(
        Instant.now(), 400, "VALIDATION_ERROR", "Los datos enviados no son válidos", errors));
  }

  @ExceptionHandler(BadRequestException.class)
  ResponseEntity<ApiError> badRequest(BadRequestException ex) {
    return ResponseEntity.badRequest().body(ApiError.of(400, "BAD_REQUEST", ex.getMessage()));
  }

  @ExceptionHandler(BadCredentialsException.class)
  ResponseEntity<ApiError> badCredentials() {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(ApiError.of(401, "UNAUTHORIZED", "Credenciales inválidas"));
  }

  @ExceptionHandler(NotFoundException.class)
  ResponseEntity<ApiError> notFound(NotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiError.of(404, "NOT_FOUND", ex.getMessage()));
  }

  @ExceptionHandler(DependencyUnavailableException.class)
  ResponseEntity<ApiError> dependency(DependencyUnavailableException ex) {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(ApiError.of(503, "DEPENDENCY_UNAVAILABLE", ex.getMessage()));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiError> generic(Exception ex) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiError.of(500, "INTERNAL_ERROR", "Ocurrió un error inesperado"));
  }
}
