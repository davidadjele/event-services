package com.oneevent.shared.exception;

import java.time.Instant;
import java.util.stream.Collectors;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
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

  // java
  @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
  public ResponseEntity<ApiError> handleValidation(Exception ex, HttpServletRequest req) {
    String traceId = traceId();

    String msg;
    if (ex instanceof MethodArgumentNotValidException methodArgumentNotValidException) {
      msg =
          methodArgumentNotValidException.getBindingResult().getFieldErrors().stream()
              .map(this::fieldMsg)
              .collect(Collectors.joining("; "));
    } else if (ex instanceof HttpMessageNotReadableException) {
      msg = "Corps de requête invalide ou mal formé.";
    } else {
      msg = "Requête invalide.";
    }

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

  /**
   * Gère les erreurs de conversion de paramètres (ex: UUID invalide, format de date incorrect).
   *
   * @param ex l'exception de type mismatch
   * @param req la requête HTTP
   * @return une réponse 400 Bad Request avec un message d'erreur clair
   */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiError> handleTypeMismatch(
      MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
    String traceId = traceId();

    String paramName = ex.getName();
    String requiredType =
        ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
    String providedValue = ex.getValue() != null ? ex.getValue().toString() : "null";

    String msg =
        String.format(
            "Paramètre '%s' invalide : valeur '%s' ne peut pas être convertie en %s",
            paramName, providedValue, requiredType);

    log.warn(
        "[{}] Type mismatch for parameter '{}': provided='{}', required={}",
        traceId,
        paramName,
        providedValue,
        requiredType);

    String safeUri = HtmlUtils.htmlEscape(req.getRequestURI());
    ApiError body =
        new ApiError(
            Instant.now(),
            safeUri,
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.name(),
            msg,
            "INVALID_PARAMETER",
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
