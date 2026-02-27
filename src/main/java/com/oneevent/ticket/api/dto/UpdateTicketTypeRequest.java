package com.oneevent.ticket.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description =
        "Requête de mise à jour partielle d'un type de ticket — tous les champs sont optionnels")
public record UpdateTicketTypeRequest(
    @Schema(description = "Nouveau nom du type de ticket", example = "VIP") String name,
    @Schema(
            description = "Nouveau prix unitaire (ex: 150.00). Mettre null pour ne pas modifier.",
            example = "150.00")
        BigDecimal price,
    @Schema(
            description =
                "Nouvelle quantité disponible (doit être >= tickets déjà vendus si fourni)",
            example = "200")
        Integer quantityAvailable,
    @Schema(
            description =
                "Nouvelle date/heure de début de mise en vente (ISO-8601 UTC). Optionnel.",
            example = "2026-03-01T10:00:00Z")
        Instant saleStart,
    @Schema(
            description = "Nouvelle date/heure de fin de mise en vente (ISO-8601 UTC). Optionnel.",
            example = "2026-03-10T23:59:59Z")
        Instant saleEnd) {}
