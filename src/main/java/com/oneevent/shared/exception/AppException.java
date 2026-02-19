package com.oneevent.shared.exception;

import org.springframework.http.HttpStatus;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Exception applicative réutilisable avec builder Lombok. Utiliser la factory {@code
 * builder(HttpStatus)} pour forcer la fourniture d'un statut HTTP.
 */
@Getter
@ToString(callSuper = true)
public class AppException extends RuntimeException {
  private final HttpStatus httpStatus;
  private final String message;
  private final String logMessage;
  private final String errorCode;

  @Builder(builderMethodName = "lombokBuilder")
  private AppException(
      HttpStatus httpStatus, String message, String logMessage, String errorCode, Throwable cause) {
    super(cause);
    this.httpStatus = httpStatus;
    this.message = (message == null || message.isBlank()) ? "Une erreur est survenue." : message;
    this.logMessage = logMessage;
    this.errorCode = errorCode;
  }

  /**
   * Factory pour démarrer le builder en exigeant un {@link HttpStatus}. Exemple : {@code
   * AppException.builder(HttpStatus.BAD_REQUEST).message("...").build();}
   */
  public static AppExceptionBuilder builder(HttpStatus status) {
    return AppException.lombokBuilder().httpStatus(status);
  }
}
