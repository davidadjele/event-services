package com.oneevent.event.api;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import com.oneevent.event.api.dto.PublicEventResponse;
import com.oneevent.event.api.mapper.EventMapper;
import com.oneevent.event.application.EventService;
import com.oneevent.shared.api.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(
    name = "Événements publics",
    description =
        "API publique pour consulter les événements publiés. Aucune authentification requise.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/public/events")
public class PublicEventController {

  private final EventService service;
  private final EventMapper mapper;

  @Operation(
      summary = "Lister les événements publiés",
      description =
          "Retourne la liste paginée de tous les événements publiés (status = PUBLISHED). "
              + "Accessible sans authentification. Les informations sensibles (organizationId, dates d'audit) ne sont pas exposées.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Liste des événements publics récupérée avec succès",
            content = @Content(schema = @Schema(implementation = PageResponse.class)))
      })
  @GetMapping
  public PageResponse<PublicEventResponse> listPublished(
      @Parameter(
              description = "Paramètres de pagination (page, size, sort)",
              example = "page=0&size=20&sort=startDate,asc")
          Pageable pageable) {
    Page<PublicEventResponse> mapped =
        service.listPublished(pageable).map(mapper::toPublicResponse);
    return PageResponse.of(mapped);
  }

  @Operation(
      summary = "Récupérer un événement publié par son ID",
      description =
          "Retourne les détails d'un événement publié spécifique. "
              + "Accessible sans authentification. Retourne 404 si l'événement n'existe pas ou n'est pas publié.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Événement public récupéré avec succès",
            content = @Content(schema = @Schema(implementation = PublicEventResponse.class))),
        @ApiResponse(responseCode = "400", description = "ID invalide (format UUID incorrect)"),
        @ApiResponse(responseCode = "404", description = "Événement introuvable ou non publié")
      })
  @GetMapping("/{id}")
  public PublicEventResponse getPublished(
      @Parameter(description = "Identifiant unique de l'événement (UUID)", required = true)
          @PathVariable
          UUID id) {
    return mapper.toPublicResponse(service.getPublished(id));
  }
}
