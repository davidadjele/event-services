package com.oneevent.event.api.dto;

import java.time.Instant;

import com.oneevent.event.domain.EventStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description =
        "Requête de mise à jour partielle d'un événement. Tous les champs sont optionnels. Les champs null ne seront pas modifiés.")
public record UpdateEventRequest(
    @Schema(
            description = "Nouveau titre de l'événement",
            example = "Conférence Tech Africa 2025 - Édition Spéciale",
            nullable = true)
        String title,
    @Schema(
            description = "Nouvelle description de l'événement",
            example = "Description mise à jour avec plus de détails",
            nullable = true)
        String description,
    @Schema(
            description = "Nouveau lieu de l'événement",
            example = "Nouvel Espace Events, Lomé, Togo",
            nullable = true)
        String location,
    @Schema(
            description = "Nouvelle date et heure de début (format ISO-8601 UTC)",
            example = "2025-06-20T09:00:00Z",
            nullable = true)
        Instant startDate,
    @Schema(
            description = "Nouvelle date et heure de fin (format ISO-8601 UTC)",
            example = "2025-06-20T18:00:00Z",
            nullable = true)
        Instant endDate,
    @Schema(
            description = "Nouveau statut de l'événement",
            example = "PUBLISHED",
            allowableValues = {"DRAFT", "PUBLISHED", "CANCELLED", "COMPLETED"},
            nullable = true)
        EventStatus status) {}
