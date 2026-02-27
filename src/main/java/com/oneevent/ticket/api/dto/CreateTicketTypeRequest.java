package com.oneevent.ticket.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Requête de création d'un type de ticket")
public record CreateTicketTypeRequest(
    @Schema(
            description = "Identifiant UUID de l'événement auquel est rattaché le type de ticket",
            example = "123e4567-e89b-12d3-a456-426614174000")
        @NotNull
        UUID eventId,
    @Schema(description = "Nom du type de ticket", example = "Early Bird") @NotBlank String name,
    @Schema(description = "Prix unitaire (ex: 25.00). Utiliser 0 pour gratuit.", example = "25.00")
        @NotNull
        @Min(0)
        BigDecimal price,
    @Schema(
            description = "Quantité disponible pour ce type de ticket (doit être >= 1)",
            example = "100")
        @Min(1)
        int quantityAvailable,
    @Schema(
            description = "Date/heure de début de mise en vente (ISO-8601 UTC). Optionnel.",
            example = "2026-03-01T10:00:00Z")
        Instant saleStart,
    @Schema(
            description = "Date/heure de fin de mise en vente (ISO-8601 UTC). Optionnel.",
            example = "2026-03-10T23:59:59Z")
        Instant saleEnd) {}
