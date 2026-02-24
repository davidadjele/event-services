package com.oneevent.event.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.oneevent.event.domain.EventStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Requête de création d'un événement")
public record CreateEventRequest(
    @Schema(
            description =
                "Identifiant de l'organisation (optionnel pour organisateur, requis pour super admin)",
            example = "123e4567-e89b-12d3-a456-426614174000",
            nullable = true)
        UUID organizationId,
    @Schema(
            description = "Titre de l'événement",
            example = "Conférence Tech Africa 2025",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String title,
    @Schema(
            description = "Description détaillée de l'événement",
            example =
                "Une journée complète dédiée aux technologies émergentes en Afrique avec des experts internationaux")
        String description,
    @Schema(description = "Lieu de l'événement", example = "Centre de Conférence, Lomé, Togo")
        String location,
    @Schema(
            description = "Date et heure de début (format ISO-8601 UTC)",
            example = "2025-06-15T09:00:00Z",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        Instant startDate,
    @Schema(
            description = "Date et heure de fin (format ISO-8601 UTC)",
            example = "2025-06-15T18:00:00Z",
            nullable = true)
        Instant endDate,
    @Schema(
            description = "Statut de l'événement",
            example = "DRAFT",
            allowableValues = {"DRAFT", "PUBLISHED", "CANCELLED", "COMPLETED"},
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        EventStatus status) {}
