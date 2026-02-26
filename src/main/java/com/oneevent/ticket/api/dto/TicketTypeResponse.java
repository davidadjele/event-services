package com.oneevent.ticket.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Réponse détaillée d'un type de ticket (pour usage interne/admin)")
public record TicketTypeResponse(
    @Schema(
            description = "Identifiant UUID du type de ticket",
            example = "a64cb873-9935-4d3b-bed9-c337e195c8be")
        String id,
    @Schema(
            description = "Identifiant UUID de l'événement associé",
            example = "123e4567-e89b-12d3-a456-426614174000")
        String eventId,
    @Schema(description = "Nom du type de ticket", example = "Early Bird") String name,
    @Schema(description = "Prix unitaire du ticket", example = "25.00") BigDecimal price,
    @Schema(description = "Quantité totale disponible pour ce type", example = "100")
        int quantityAvailable,
    @Schema(description = "Quantité déjà vendue", example = "10") int quantitySold,
    @Schema(description = "Date/heure de début de la vente (UTC)", example = "2026-03-01T10:00:00Z")
        Instant saleStart,
    @Schema(description = "Date/heure de fin de la vente (UTC)", example = "2026-03-10T23:59:59Z")
        Instant saleEnd,
    @Schema(description = "Horodatage de création (UTC)", example = "2026-02-01T12:00:00Z")
        Instant createdAt,
    @Schema(
            description = "Horodatage de dernière mise à jour (UTC)",
            example = "2026-02-02T15:00:00Z")
        Instant updatedAt) {}
