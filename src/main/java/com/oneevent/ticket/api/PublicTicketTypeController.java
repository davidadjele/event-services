package com.oneevent.ticket.api;

import static com.oneevent.shared.constants.ApiPaths.PUBLIC_TICKET_TYPES;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.*;

import com.oneevent.ticket.api.dto.PublicTicketTypeResponse;
import com.oneevent.ticket.api.mapper.TicketTypeMapper;
import com.oneevent.ticket.application.TicketTypeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Contrôleur public donnant accès aux types de tickets d'un événement publié.
 *
 * <p>Ces endpoints sont accessibles sans authentification et ne retournent que les types de tickets
 * rattachés à un événement dont le statut est {@code PUBLISHED}.
 *
 * <p>URL de base : {@code /api/v1/public/ticket-types}
 */
@Tag(
    name = "Types de tickets – Public",
    description =
        "Endpoint public permettant de consulter les types de tickets disponibles "
            + "pour un événement publié, sans nécessiter d'authentification.")
@RestController
@RequestMapping(PUBLIC_TICKET_TYPES)
@RequiredArgsConstructor
public class PublicTicketTypeController {

  private final TicketTypeService service;
  private final TicketTypeMapper mapper;

  /**
   * Retourne la liste des types de tickets disponibles pour un événement publié.
   *
   * <p>Seuls les types de tickets non supprimés ({@code deletedAt IS NULL}) liés à un événement
   * avec le statut {@code PUBLISHED} sont retournés.
   *
   * @param eventId identifiant UUID de l'événement
   * @return liste des types de tickets sous forme de {@link PublicTicketTypeResponse}
   */
  @Operation(
      summary = "Lister les types de tickets d'un événement publié",
      description =
          "Retourne tous les types de tickets actifs (non supprimés) associés à un événement "
              + "dont le statut est PUBLISHED. Accessible sans authentification.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Liste des types de tickets retournée avec succès",
            content =
                @Content(
                    array =
                        @ArraySchema(
                            schema = @Schema(implementation = PublicTicketTypeResponse.class)))),
        @ApiResponse(responseCode = "404", description = "Événement introuvable ou non publié"),
        @ApiResponse(
            responseCode = "400",
            description = "L'identifiant fourni n'est pas un UUID valide")
      })
  @GetMapping("/by-event/{eventId}")
  public List<PublicTicketTypeResponse> listForPublishedEvent(
      @Parameter(
              description = "Identifiant UUID de l'événement publié",
              example = "123e4567-e89b-12d3-a456-426614174000")
          @PathVariable
          UUID eventId) {
    return service.listForPublishedEvent(eventId).stream().map(mapper::toPublicResponse).toList();
  }
}
