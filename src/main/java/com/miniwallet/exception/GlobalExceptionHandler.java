package com.miniwallet.exception;

import com.miniwallet.dto.response.ErrorResponse;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationExceptions(
      MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult().getAllErrors().forEach((error) -> {
      String fieldName = ((FieldError) error).getField();
      String errorMessage = error.getDefaultMessage();
      errors.put(fieldName, errorMessage);
    });

    ErrorResponse errorResponse = ErrorResponse.builder()
        .status(HttpStatus.BAD_REQUEST.value())
        .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
        .message("Validation error: " + errors.toString())
        .timestamp(LocalDateTime.now())
        .build();
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
  }

  @ExceptionHandler(DailyLimitExceededException.class)
  public ResponseEntity<ErrorResponse> handleDailyLimitExceededException(
      DailyLimitExceededException ex) {
    log.error("DailyLimitExceededException: ", ex);
    ErrorResponse errorResponse = ErrorResponse.builder()
        .status(HttpStatus.CONFLICT.value())
        .error(HttpStatus.CONFLICT.getReasonPhrase())
        .message(ex.getMessage())
        .timestamp(LocalDateTime.now())
        .build();
    return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
    log.error("IllegalArgumentException: ", ex);
    ErrorResponse errorResponse = ErrorResponse.builder()
        .status(HttpStatus.BAD_REQUEST.value())
        .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
        .message(ex.getMessage())
        .timestamp(LocalDateTime.now())
        .build();
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException ex) {
    log.error("BadCredentialsException: ", ex);
    ErrorResponse errorResponse = ErrorResponse.builder()
        .status(HttpStatus.UNAUTHORIZED.value())
        .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
        .message("Invalid username or password")
        .timestamp(LocalDateTime.now())
        .build();
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
    log.error("AccessDeniedException: ", ex);
    ErrorResponse errorResponse = ErrorResponse.builder()
        .status(HttpStatus.FORBIDDEN.value())
        .error(HttpStatus.FORBIDDEN.getReasonPhrase())
        .message("Access denied")
        .timestamp(LocalDateTime.now())
        .build();
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
    log.error("AuthenticationException: ", ex);
    ErrorResponse errorResponse = ErrorResponse.builder()
        .status(HttpStatus.UNAUTHORIZED.value())
        .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
        .message("Unauthorized: " + ex.getMessage())
        .timestamp(LocalDateTime.now())
        .build();
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex) {
    log.error("Unhandled exception: ", ex);
    ErrorResponse errorResponse = ErrorResponse.builder()
        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
        .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
        .message(ex.getMessage())
        .timestamp(LocalDateTime.now())
        .build();
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
  }
}
