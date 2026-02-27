package com.oneevent.event.api;

import static com.oneevent.shared.constants.ApiPaths.EVENTS;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import com.oneevent.event.api.dto.CreateEventRequest;
import com.oneevent.event.api.dto.EventResponse;
import com.oneevent.event.api.dto.UpdateEventRequest;
import com.oneevent.event.api.mapper.EventMapper;
import com.oneevent.event.application.EventService;
import com.oneevent.event.domain.Event;
import com.oneevent.shared.api.PageResponse;
import com.oneevent.shared.exception.ApiError;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(
    name = "Gestion des événements",
    description = "API de gestion des événements pour les organisateurs et super admins")
@RestController
@RequestMapping(EVENTS)
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class EventController {

  private final EventService service;
  private final EventMapper mapper;

  @Operation(
      summary = "Créer un nouvel événement",
      description =
          "Crée un nouvel événement pour l'organisation de l'utilisateur connecté. "
              + "Pour les organisateurs, l'organizationId est automatiquement résolu. "
              + "Pour les super admins, l'organizationId doit être fourni dans la requête.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Événement créé avec succès",
            content = @Content(schema = @Schema(implementation = EventResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description =
                "Requête invalide (dates invalides, champs manquants, organizationId requis pour super admin)",
            content =
                @Content(
                    schema = @Schema(implementation = ApiError.class),
                    examples = @ExampleObject(name = "BadRequest", value = ApiError.EXAMPLE_JSON))),
        @ApiResponse(
            responseCode = "401",
            description = "Non authentifié",
            content =
                @Content(
                    schema = @Schema(implementation = ApiError.class),
                    examples =
                        @ExampleObject(name = "Unauthorized", value = ApiError.EXAMPLE_JSON))),
        @ApiResponse(
            responseCode = "403",
            description = "Accès refusé (organization requise)",
            content =
                @Content(
                    schema = @Schema(implementation = ApiError.class),
                    examples = @ExampleObject(name = "Forbidden", value = ApiError.EXAMPLE_JSON)))
      })
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description =
          "Informations de l'événement à créer. Les dates doivent être au format ISO-8601 (UTC). "
              + "La date de fin doit être après la date de début.",
      required = true)
  @PostMapping
  public EventResponse create(@RequestBody @Valid CreateEventRequest req) {
    Event e =
        service.create(
            new EventService.CreateEventCommand(
                req.organizationId(),
                req.title(),
                req.description(),
                req.location(),
                req.startDate(),
                req.endDate(),
                req.status()));
    return mapper.toResponse(e);
  }

  @Operation(
      summary = "Lister les événements de mon organisation",
      description =
          "Retourne la liste paginée des événements de l'organisation de l'utilisateur connecté. "
              + "Pour les super admins, le paramètre orgId est obligatoire pour filtrer par organisation.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Liste des événements récupérée avec succès",
            content = @Content(schema = @Schema(implementation = PageResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Paramètre orgId requis pour super admin",
            content =
                @Content(
                    schema = @Schema(implementation = ApiError.class),
                    examples = @ExampleObject(name = "BadRequest", value = ApiError.EXAMPLE_JSON))),
        @ApiResponse(
            responseCode = "401",
            description = "Non authentifié",
            content =
                @Content(
                    schema = @Schema(implementation = ApiError.class),
                    examples =
                        @ExampleObject(name = "Unauthorized", value = ApiError.EXAMPLE_JSON))),
        @ApiResponse(
            responseCode = "403",
            description = "Accès refusé",
            content =
                @Content(
                    schema = @Schema(implementation = ApiError.class),
                    examples = @ExampleObject(name = "Forbidden", value = ApiError.EXAMPLE_JSON)))
      })
  @GetMapping
  public PageResponse<EventResponse> listMine(
      @Parameter(
              description =
                  "Identifiant de l'organisation à filtrer (obligatoire pour super admin, ignoré pour organisateur)",
              example = "123e4567-e89b-12d3-a456-426614174000")
          @RequestParam(name = "orgId", required = false)
          UUID orgId,
      @Parameter(
              description = "Paramètres de pagination (page, size, sort)",
              example = "page=0&size=20&sort=createdAt,desc")
          Pageable pageable) {
    Page<EventResponse> mapped = service.listMine(orgId, pageable).map(mapper::toResponse);
    return PageResponse.of(mapped);
  }

  @Operation(
      summary = "Récupérer un événement par son ID",
      description =
          "Retourne les détails d'un événement spécifique appartenant à l'organisation de l'utilisateur connecté.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Événement récupéré avec succès",
            content = @Content(schema = @Schema(implementation = EventResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "ID invalide ou paramètre orgId requis pour super admin",
            content =
                @Content(
                    schema = @Schema(implementation = ApiError.class),
                    examples = @ExampleObject(name = "BadRequest", value = ApiError.EXAMPLE_JSON))),
        @ApiResponse(
            responseCode = "401",
            description = "Non authentifié",
            content =
                @Content(
                    schema = @Schema(implementation = ApiError.class),
                    examples =
                        @ExampleObject(name = "Unauthorized", value = ApiError.EXAMPLE_JSON))),
        @ApiResponse(
            responseCode = "403",
            description = "Accès refusé",
            content =
                @Content(
                    schema = @Schema(implementation = ApiError.class),
                    examples = @ExampleObject(name = "Forbidden", value = ApiError.EXAMPLE_JSON))),
        @ApiResponse(
            responseCode = "404",
            description = "Événement introuvable ou n'appartient pas à votre organisation",
            content =
                @Content(
                    schema = @Schema(implementation = ApiError.class),
                    examples = @ExampleObject(name = "NotFound", value = ApiError.EXAMPLE_JSON)))
      })
  @GetMapping("/{id}")
  public EventResponse getMine(
      @Parameter(description = "Identifiant unique de l'événement (UUID)", required = true)
          @PathVariable
          UUID id,
      @Parameter(
              description =
                  "Identifiant de l'organisation (obligatoire pour super admin, ignoré pour organisateur)",
              example = "123e4567-e89b-12d3-a456-426614174000")
          @RequestParam(name = "orgId", required = false)
          UUID orgId) {
    return mapper.toResponse(service.getMine(orgId, id));
  }

  @Operation(
      summary = "Mettre à jour un événement",
      description =
          "Met à jour partiellement un événement existant. Seuls les champs fournis dans la requête seront modifiés.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Événement mis à jour avec succès",
            content = @Content(schema = @Schema(implementation = EventResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description =
                "Requête invalide (dates invalides, ID invalide, paramètre orgId requis pour super admin)",
            content =
                @Content(
                    schema = @Schema(implementation = ApiError.class),
                    examples = @ExampleObject(name = "BadRequest", value = ApiError.EXAMPLE_JSON))),
        @ApiResponse(
            responseCode = "401",
            description = "Non authentifié",
            content =
                @Content(
                    schema = @Schema(implementation = ApiError.class),
                    examples =
                        @ExampleObject(name = "Unauthorized", value = ApiError.EXAMPLE_JSON))),
        @ApiResponse(
            responseCode = "403",
            description = "Accès refusé",
            content =
                @Content(
                    schema = @Schema(implementation = ApiError.class),
                    examples = @ExampleObject(name = "Forbidden", value = ApiError.EXAMPLE_JSON))),
        @ApiResponse(
            responseCode = "404",
            description = "Événement introuvable ou n'appartient pas à votre organisation",
            content =
                @Content(
                    schema = @Schema(implementation = ApiError.class),
                    examples = @ExampleObject(name = "NotFound", value = ApiError.EXAMPLE_JSON)))
      })
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description =
          "Champs de l'événement à mettre à jour. Tous les champs sont optionnels (mise à jour partielle). "
              + "Les champs null ne sont pas modifiés.",
      required = true)
  @PatchMapping("/{id}")
  public EventResponse update(
      @Parameter(description = "Identifiant unique de l'événement (UUID)", required = true)
          @PathVariable
          UUID id,
      @Parameter(
              description =
                  "Identifiant de l'organisation (obligatoire pour super admin, ignoré pour organisateur)",
              example = "123e4567-e89b-12d3-a456-426614174000")
          @RequestParam(name = "orgId", required = false)
          UUID orgId,
      @RequestBody UpdateEventRequest req) {
    var e =
        service.update(
            id,
            orgId,
            new EventService.UpdateEventCommand(
                req.title(),
                req.description(),
                req.location(),
                req.startDate(),
                req.endDate(),
                req.status()));
    return mapper.toResponse(e);
  }

  @Operation(
      summary = "Supprimer un événement",
      description =
          "Supprime logiquement un événement (soft delete). L'événement n'est pas supprimé physiquement mais marqué comme supprimé.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Événement supprimé avec succès"),
        @ApiResponse(
            responseCode = "400",
            description = "ID invalide ou paramètre orgId requis pour super admin",
            content =
                @Content(
                    schema = @Schema(implementation = ApiError.class),
                    examples = @ExampleObject(name = "BadRequest", value = ApiError.EXAMPLE_JSON))),
        @ApiResponse(
            responseCode = "401",
            description = "Non authentifié",
            content =
                @Content(
                    schema = @Schema(implementation = ApiError.class),
                    examples =
                        @ExampleObject(name = "Unauthorized", value = ApiError.EXAMPLE_JSON))),
        @ApiResponse(
            responseCode = "403",
            description = "Accès refusé",
            content =
                @Content(
                    schema = @Schema(implementation = ApiError.class),
                    examples = @ExampleObject(name = "Forbidden", value = ApiError.EXAMPLE_JSON))),
        @ApiResponse(
            responseCode = "404",
            description = "Événement introuvable ou n'appartient pas à votre organisation",
            content =
                @Content(
                    schema = @Schema(implementation = ApiError.class),
                    examples = @ExampleObject(name = "NotFound", value = ApiError.EXAMPLE_JSON)))
      })
  @DeleteMapping("/{id}")
  public void delete(
      @Parameter(description = "Identifiant unique de l'événement (UUID)", required = true)
          @PathVariable
          UUID id,
      @Parameter(
              description =
                  "Identifiant de l'organisation (obligatoire pour super admin, ignoré pour organisateur)",
              example = "123e4567-e89b-12d3-a456-426614174000")
          @RequestParam(name = "orgId", required = false)
          UUID orgId) {
    service.softDelete(id, orgId);
  }
}
