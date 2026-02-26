package com.oneevent.ticket.api;

import static com.oneevent.shared.constants.ApiPaths.TICKET_TYPES;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.oneevent.ticket.api.dto.CreateTicketTypeRequest;
import com.oneevent.ticket.api.dto.TicketTypeResponse;
import com.oneevent.ticket.api.dto.UpdateTicketTypeRequest;
import com.oneevent.ticket.api.mapper.TicketTypeMapper;
import com.oneevent.ticket.application.TicketTypeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Contrôleur de gestion des types de tickets pour les organisateurs et super admins.
 *
 * <p>Fournit les opérations CRUD (création, lecture, mise à jour, suppression douce) sur les types
 * de tickets liés aux événements d'une organisation.
 *
 * <p>Tous les endpoints nécessitent un token JWT valide. Pour les organisateurs, l'organisation est
 * résolue automatiquement depuis le token. Pour les super admins, le paramètre {@code orgId} est
 * obligatoire sur tous les endpoints.
 *
 * <p>URL de base : {@code /api/v1/ticket-types}
 */
@Tag(
    name = "Types de tickets – Organisateur",
    description =
        "API de gestion des types de tickets pour les organisateurs et super admins. "
            + "Permet de créer, consulter, modifier et supprimer les types de tickets "
            + "associés aux événements de l'organisation. "
            + "Pour les super admins, le paramètre orgId est obligatoire sur tous les endpoints.")
@RestController
@RequestMapping(TICKET_TYPES)
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class TicketTypeController {

  private final TicketTypeService service;
  private final TicketTypeMapper mapper;

  /**
   * Crée un nouveau type de ticket pour un événement appartenant à l'organisation résolue.
   *
   * <p>Pour un organisateur, l'organisation est déduite du token JWT. Pour un super admin, {@code
   * orgId} doit être fourni en paramètre de requête.
   *
   * @param orgId identifiant de l'organisation (obligatoire pour SUPER_ADMIN, ignoré pour
   *     ORGANIZER)
   * @param req les informations du type de ticket à créer
   * @return le type de ticket créé
   */
  @Operation(
      summary = "Créer un type de ticket",
      description =
          "Crée un nouveau type de ticket pour un événement appartenant à l'organisation résolue. "
              + "Le prix doit être ≥ 0 et la quantité ≥ 1. "
              + "Si une fenêtre de vente est précisée, la date de fin doit être après la date de début. "
              + "Pour les super admins, le paramètre orgId est obligatoire.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Type de ticket créé avec succès",
            content = @Content(schema = @Schema(implementation = TicketTypeResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description =
                "Requête invalide (prix négatif, quantité ≤ 0, fenêtre incohérente, orgId manquant pour super admin)"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(
            responseCode = "404",
            description = "Événement introuvable ou n'appartient pas à l'organisation")
      })
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description =
          "Informations du type de ticket à créer. "
              + "Les dates saleStart et saleEnd sont optionnelles et doivent être au format ISO-8601 (UTC).",
      required = true)
  @PostMapping
  public TicketTypeResponse create(
      @Parameter(
              description =
                  "Identifiant de l'organisation (obligatoire pour SUPER_ADMIN, ignoré pour ORGANIZER)",
              example = "123e4567-e89b-12d3-a456-426614174000")
          @RequestParam(name = "orgId", required = false)
          UUID orgId,
      @RequestBody @Valid CreateTicketTypeRequest req) {
    var created =
        service.create(
            new TicketTypeService.CreateTicketTypeCommand(
                orgId,
                req.eventId(),
                req.name(),
                req.price(),
                req.quantityAvailable(),
                req.saleStart(),
                req.saleEnd()));
    return mapper.toResponse(created);
  }

  /**
   * Retourne la liste des types de tickets actifs (non supprimés) d'un événement appartenant à
   * l'organisation résolue.
   *
   * <p>Pour un organisateur, l'organisation est déduite du token JWT. Pour un super admin, {@code
   * orgId} doit être fourni en paramètre de requête.
   *
   * @param orgId identifiant de l'organisation (obligatoire pour SUPER_ADMIN, ignoré pour
   *     ORGANIZER)
   * @param eventId identifiant UUID de l'événement
   * @return liste des types de tickets de l'événement
   */
  @Operation(
      summary = "Lister les types de tickets d'un événement",
      description =
          "Retourne tous les types de tickets actifs (non supprimés) pour un événement "
              + "appartenant à l'organisation résolue. "
              + "Pour les super admins, le paramètre orgId est obligatoire.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Liste des types de tickets retournée avec succès",
            content =
                @Content(
                    array =
                        @ArraySchema(schema = @Schema(implementation = TicketTypeResponse.class)))),
        @ApiResponse(
            responseCode = "400",
            description = "UUID invalide ou orgId manquant pour super admin"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(
            responseCode = "404",
            description = "Événement introuvable ou n'appartient pas à l'organisation")
      })
  @GetMapping("/by-event/{eventId}")
  public List<TicketTypeResponse> listByEvent(
      @Parameter(
              description =
                  "Identifiant de l'organisation (obligatoire pour SUPER_ADMIN, ignoré pour ORGANIZER)",
              example = "123e4567-e89b-12d3-a456-426614174000")
          @RequestParam(name = "orgId", required = false)
          UUID orgId,
      @Parameter(
              description = "Identifiant UUID de l'événement",
              example = "123e4567-e89b-12d3-a456-426614174000")
          @PathVariable
          UUID eventId) {
    return service.listByEventMine(orgId, eventId).stream().map(mapper::toResponse).toList();
  }

  /**
   * Met à jour partiellement un type de ticket existant. Seuls les champs non-null de la requête
   * sont appliqués.
   *
   * <p>Pour un organisateur, l'organisation est déduite du token JWT. Pour un super admin, {@code
   * orgId} doit être fourni en paramètre de requête.
   *
   * @param id identifiant UUID du type de ticket à modifier
   * @param orgId identifiant de l'organisation (obligatoire pour SUPER_ADMIN, ignoré pour
   *     ORGANIZER)
   * @param req les champs à mettre à jour (tous optionnels)
   * @return le type de ticket mis à jour
   */
  @Operation(
      summary = "Mettre à jour un type de ticket",
      description =
          "Mise à jour partielle (PATCH) d'un type de ticket. "
              + "Seuls les champs présents dans la requête sont modifiés. "
              + "La quantité disponible ne peut pas être inférieure au nombre de tickets déjà vendus. "
              + "Pour les super admins, le paramètre orgId est obligatoire.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Type de ticket mis à jour avec succès",
            content = @Content(schema = @Schema(implementation = TicketTypeResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description =
                "Requête invalide (quantité < vendus, fenêtre incohérente, UUID invalide, orgId manquant pour super admin)"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "404", description = "Type de ticket introuvable")
      })
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "Champs à mettre à jour. Tous les champs sont optionnels.",
      required = true)
  @PatchMapping("/{id}")
  public TicketTypeResponse update(
      @Parameter(
              description = "Identifiant UUID du type de ticket",
              example = "123e4567-e89b-12d3-a456-426614174000")
          @PathVariable
          UUID id,
      @Parameter(
              description =
                  "Identifiant de l'organisation (obligatoire pour SUPER_ADMIN, ignoré pour ORGANIZER)",
              example = "123e4567-e89b-12d3-a456-426614174000")
          @RequestParam(name = "orgId", required = false)
          UUID orgId,
      @RequestBody @Valid UpdateTicketTypeRequest req) {
    var updated =
        service.update(
            id,
            orgId,
            new TicketTypeService.PatchTicketTypeCommand(
                req.name(), req.price(), req.quantityAvailable(), req.saleStart(), req.saleEnd()));
    return mapper.toResponse(updated);
  }

  /**
   * Supprime logiquement un type de ticket (soft delete). Le type de ticket ne peut pas être
   * supprimé s'il a déjà des tickets vendus.
   *
   * <p>Pour un organisateur, l'organisation est déduite du token JWT. Pour un super admin, {@code
   * orgId} doit être fourni en paramètre de requête.
   *
   * @param id identifiant UUID du type de ticket à supprimer
   * @param orgId identifiant de l'organisation (obligatoire pour SUPER_ADMIN, ignoré pour
   *     ORGANIZER)
   */
  @Operation(
      summary = "Supprimer un type de ticket",
      description =
          "Suppression logique (soft delete) d'un type de ticket. "
              + "L'opération est refusée si des tickets de ce type ont déjà été vendus. "
              + "Pour les super admins, le paramètre orgId est obligatoire.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Type de ticket supprimé avec succès"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "404", description = "Type de ticket introuvable"),
        @ApiResponse(
            responseCode = "409",
            description = "Impossible de supprimer : des tickets ont déjà été vendus"),
        @ApiResponse(
            responseCode = "400",
            description = "UUID invalide ou orgId manquant pour super admin")
      })
  @DeleteMapping("/{id}")
  public void delete(
      @Parameter(
              description = "Identifiant UUID du type de ticket à supprimer",
              example = "123e4567-e89b-12d3-a456-426614174000")
          @PathVariable
          UUID id,
      @Parameter(
              description =
                  "Identifiant de l'organisation (obligatoire pour SUPER_ADMIN, ignoré pour ORGANIZER)",
              example = "123e4567-e89b-12d3-a456-426614174000")
          @RequestParam(name = "orgId", required = false)
          UUID orgId) {
    service.softDelete(id, orgId);
  }
}
