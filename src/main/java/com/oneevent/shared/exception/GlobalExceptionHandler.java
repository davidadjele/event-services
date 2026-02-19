package com.oneevent.shared.exception;

import java.time.Instant;
import java.util.stream.Collectors;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.util.HtmlUtils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(AppException.class)
  public ResponseEntity<ApiError> handleApp(AppException ex, HttpServletRequest req) {
    String traceId = traceId();

    if (ex.getLogMessage() != null && !ex.getLogMessage().isBlank()) {
      log.warn("[{}] {}", traceId, ex.getLogMessage(), ex.getCause());
    } else {
      log.warn("[{}] AppException", traceId, ex.getCause());
    }

    String safeUri = HtmlUtils.htmlEscape(req.getRequestURI());

    ApiError body =
        new ApiError(
            Instant.now(),
            safeUri,
            ex.getHttpStatus().value(),
            ex.getHttpStatus().name(),
            ex.getMessage(),
            ex.getErrorCode(),
            traceId);
    return ResponseEntity.status(ex.getHttpStatus()).body(body);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(
      MethodArgumentNotValidException ex, HttpServletRequest req) {
    String traceId = traceId();
    String msg =
        ex.getBindingResult().getFieldErrors().stream()
            .map(this::fieldMsg)
            .collect(Collectors.joining("; "));

    String safeUri = HtmlUtils.htmlEscape(req.getRequestURI());
    ApiError body =
        new ApiError(
            Instant.now(),
            safeUri,
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.name(),
            msg.isBlank() ? "Requête invalide." : msg,
            "VALIDATION_ERROR",
            traceId);
    return ResponseEntity.badRequest().body(body);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest req) {
    String traceId = traceId();
    log.error("[{}] Unexpected error", traceId, ex);

    String safeUri = HtmlUtils.htmlEscape(req.getRequestURI());
    ApiError body =
        new ApiError(
            Instant.now(),
            safeUri,
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            HttpStatus.INTERNAL_SERVER_ERROR.name(),
            "Une erreur interne est survenue.",
            "INTERNAL_ERROR",
            traceId);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
  }

  private String fieldMsg(FieldError fe) {
    return fe.getField()
        + ": "
        + (fe.getDefaultMessage() == null ? "invalide" : fe.getDefaultMessage());
  }

  private String traceId() {
    String t = MDC.get("traceId");
    return (t == null || t.isBlank()) ? "n/a" : t;
  }
}
