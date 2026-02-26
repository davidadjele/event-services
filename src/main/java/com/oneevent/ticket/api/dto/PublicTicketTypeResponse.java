package com.oneevent.ticket.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Représentation publique d'un type de ticket — champs exposés au public")
public record PublicTicketTypeResponse(
    @Schema(
            description = "Identifiant UUID du type de ticket",
            example = "a64cb873-9935-4d3b-bed9-c337e195c8be")
        String id,
    @Schema(
            description = "Identifiant UUID de l'événement associé",
            example = "123e4567-e89b-12d3-a456-426614174000")
        String eventId,
    @Schema(description = "Nom du type de ticket", example = "Standard") String name,
    @Schema(description = "Prix unitaire (0 = gratuit)", example = "20.00") BigDecimal price,
    @Schema(description = "Nombre de places encore disponibles pour la vente", example = "100")
        int quantityAvailable,
    @Schema(description = "Nombre de places déjà vendues", example = "10") int quantitySold,
    @Schema(
            description = "Début de la période de vente (UTC). Peut être null.",
            example = "2026-03-01T10:00:00Z")
        Instant saleStart,
    @Schema(
            description = "Fin de la période de vente (UTC). Peut être null.",
            example = "2026-03-10T23:59:59Z")
        Instant saleEnd) {}
