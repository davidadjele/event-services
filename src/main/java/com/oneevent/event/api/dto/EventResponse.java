package com.oneevent.event.api.dto;

import java.time.Instant;

import com.oneevent.event.domain.EventStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Réponse contenant les détails complets d'un événement (pour organisateurs)")
public record EventResponse(
    @Schema(
            description = "Identifiant unique de l'événement",
            example = "123e4567-e89b-12d3-a456-426614174000")
        String id,
    @Schema(
            description = "Identifiant de l'organisation propriétaire",
            example = "987e6543-e89b-12d3-a456-426614174111")
        String organizationId,
    @Schema(description = "Titre de l'événement", example = "Conférence Tech Africa 2025")
        String title,
    @Schema(
            description = "Description détaillée de l'événement",
            example = "Une grande conférence sur les technologies en Afrique")
        String description,
    @Schema(description = "Lieu de l'événement", example = "Centre de Conférence, Lomé, Togo")
        String location,
    @Schema(
            description = "Date et heure de début (format ISO-8601 UTC)",
            example = "2025-06-15T09:00:00Z")
        Instant startDate,
    @Schema(
            description = "Date et heure de fin (format ISO-8601 UTC)",
            example = "2025-06-15T18:00:00Z",
            nullable = true)
        Instant endDate,
    @Schema(
            description = "Statut de l'événement",
            example = "PUBLISHED",
            allowableValues = {"DRAFT", "PUBLISHED", "CANCELLED", "COMPLETED"})
        EventStatus status,
    @Schema(description = "Date de création de l'événement", example = "2025-01-01T10:00:00Z")
        Instant createdAt,
    @Schema(description = "Date de dernière modification", example = "2025-02-01T15:30:00Z")
        Instant updatedAt) {}
