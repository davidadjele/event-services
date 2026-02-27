package com.oneevent.shared.exception;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Représentation standardisée d'une erreur API retournée au client")
public record ApiError(
    @Schema(description = "Horodatage UTC de l'erreur", example = "2026-02-26T12:34:56Z")
        Instant timestamp,
    @Schema(
            description = "Chemin de la requête ayant provoqué l'erreur",
            example = "/api/v1/ticket-types")
        String path,
    @Schema(description = "Code HTTP retourné", example = "400") int status,
    @Schema(description = "Libellé HTTP de l'erreur", example = "BAD_REQUEST") String error,
    @Schema(
            description = "Message d'erreur lisible destiné au client",
            example = "Requête invalide : champ 'name' manquant")
        String message,
    @Schema(
            description = "Code d'erreur applicatif (utilisé pour le parsing côté client)",
            example = "VALIDATION_ERROR")
        String code,
    @Schema(
            description = "TraceId unique pour corréler les logs côté serveur",
            example = "cb7aa6a3-7215-4b2e-a2ff-27a6a6bec58a")
        String traceId) {

  public static final String EXAMPLE_JSON =
      "{"
          + "\"timestamp\": \"2026-02-26T12:34:56Z\","
          + "\"path\": \"/api/v1/ticket-types\","
          + "\"status\": 400,"
          + "\"error\": \"BAD_REQUEST\","
          + "\"message\": \"Requête invalide : champ 'name' manquant\","
          + "\"code\": \"VALIDATION_ERROR\","
          + "\"traceId\": \"cb7aa6a3-7215-4b2e-a2ff-27a6a6bec58a\"}";
}
