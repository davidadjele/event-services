package com.oneevent.event.api.dto;

import java.time.Instant;

import com.oneevent.event.domain.EventStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description =
        "Réponse publique contenant les détails d'un événement. "
            + "Les informations sensibles (organizationId, dates d'audit) ne sont pas exposées.")
public record PublicEventResponse(
    @Schema(
            description = "Identifiant unique de l'événement",
            example = "123e4567-e89b-12d3-a456-426614174000")
        String id,
    @Schema(description = "Titre de l'événement", example = "Festival de Musique Afrique 2025")
        String title,
    @Schema(
            description = "Description de l'événement",
            example = "Un grand festival de musique africaine avec des artistes internationaux")
        String description,
    @Schema(description = "Lieu de l'événement", example = "Stade National, Accra, Ghana")
        String location,
    @Schema(
            description = "Date et heure de début (format ISO-8601 UTC)",
            example = "2025-07-20T14:00:00Z")
        Instant startDate,
    @Schema(
            description = "Date et heure de fin (format ISO-8601 UTC)",
            example = "2025-07-21T23:00:00Z",
            nullable = true)
        Instant endDate,
    @Schema(
            description = "Statut de l'événement (toujours PUBLISHED pour les endpoints publics)",
            example = "PUBLISHED",
            allowableValues = {"PUBLISHED"})
        EventStatus status) {}
