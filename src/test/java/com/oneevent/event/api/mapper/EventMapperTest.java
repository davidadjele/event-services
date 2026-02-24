package com.oneevent.event.api.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import com.oneevent.event.api.dto.EventResponse;
import com.oneevent.event.api.dto.PublicEventResponse;
import com.oneevent.event.domain.Event;
import com.oneevent.event.domain.EventStatus;

/**
 * Tests unitaires pour le mapper EventMapper.
 *
 * <p>Cette classe teste les fonctionnalités de mapping :
 *
 * <ul>
 *   <li>Conversion Event → EventResponse (avec tous les champs)
 *   <li>Conversion Event → PublicEventResponse (sans organizationId)
 *   <li>Gestion des valeurs null
 *   <li>Conversion correcte des UUID en String
 *   <li>Préservation des dates et enum
 * </ul>
 */
@DisplayName("EventMapper - Tests du mapper d'événements")
class EventMapperTest {

  private EventMapper mapper;

  private static final UUID EVENT_ID = UUID.randomUUID();
  private static final UUID ORG_ID = UUID.randomUUID();
  private static final String TITLE = "Conférence Tech Africa 2025";
  private static final String DESCRIPTION = "Une conférence sur les technologies en Afrique";
  private static final String LOCATION = "Lomé, Togo";
  private static final Instant START_DATE = Instant.now().plus(30, ChronoUnit.DAYS);
  private static final Instant END_DATE = START_DATE.plus(1, ChronoUnit.DAYS);
  private static final Instant CREATED_AT = Instant.now().minus(7, ChronoUnit.DAYS);
  private static final Instant UPDATED_AT = Instant.now().minus(1, ChronoUnit.DAYS);

  @BeforeEach
  void setUp() {
    // Initialiser le mapper généré par MapStruct
    mapper = Mappers.getMapper(EventMapper.class);
  }

  @Nested
  @DisplayName("toResponse - Conversion vers EventResponse")
  class ToResponseTests {

    @Test
    @DisplayName("Devrait mapper tous les champs d'un Event vers EventResponse")
    void shouldMapAllFieldsToEventResponse() {
      // Given
      Event event =
          Event.builder()
              .id(EVENT_ID)
              .organizationId(ORG_ID)
              .title(TITLE)
              .description(DESCRIPTION)
              .location(LOCATION)
              .startDate(START_DATE)
              .endDate(END_DATE)
              .status(EventStatus.PUBLISHED)
              .build();
      event.setCreatedAt(CREATED_AT);
      event.setUpdatedAt(UPDATED_AT);

      // When
      EventResponse response = mapper.toResponse(event);

      // Then
      assertThat(response).isNotNull();
      assertThat(response.id()).isEqualTo(EVENT_ID.toString());
      assertThat(response.organizationId()).isEqualTo(ORG_ID.toString());
      assertThat(response.title()).isEqualTo(TITLE);
      assertThat(response.description()).isEqualTo(DESCRIPTION);
      assertThat(response.location()).isEqualTo(LOCATION);
      assertThat(response.startDate()).isEqualTo(START_DATE);
      assertThat(response.endDate()).isEqualTo(END_DATE);
      assertThat(response.status()).isEqualTo(EventStatus.PUBLISHED);
      assertThat(response.createdAt()).isEqualTo(CREATED_AT);
      assertThat(response.updatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    @DisplayName("Devrait mapper un Event avec status DRAFT")
    void shouldMapEventWithDraftStatus() {
      // Given
      Event event =
          Event.builder()
              .id(EVENT_ID)
              .organizationId(ORG_ID)
              .title("Event Brouillon")
              .status(EventStatus.DRAFT)
              .startDate(START_DATE)
              .build();

      // When
      EventResponse response = mapper.toResponse(event);

      // Then
      assertThat(response).isNotNull();
      assertThat(response.status()).isEqualTo(EventStatus.DRAFT);
      assertThat(response.title()).isEqualTo("Event Brouillon");
    }

    @Test
    @DisplayName("Devrait mapper un Event avec status CANCELLED")
    void shouldMapEventWithCancelledStatus() {
      // Given
      Event event =
          Event.builder()
              .id(EVENT_ID)
              .organizationId(ORG_ID)
              .title("Event Annulé")
              .status(EventStatus.CANCELLED)
              .startDate(START_DATE)
              .build();

      // When
      EventResponse response = mapper.toResponse(event);

      // Then
      assertThat(response).isNotNull();
      assertThat(response.status()).isEqualTo(EventStatus.CANCELLED);
    }

    @Test
    @DisplayName("Devrait gérer les champs optionnels null (description, location, endDate)")
    void shouldHandleOptionalNullFields() {
      // Given
      Event event =
          Event.builder()
              .id(EVENT_ID)
              .organizationId(ORG_ID)
              .title(TITLE)
              .description(null) // optionnel
              .location(null) // optionnel
              .startDate(START_DATE)
              .endDate(null) // optionnel
              .status(EventStatus.DRAFT)
              .build();

      // When
      EventResponse response = mapper.toResponse(event);

      // Then
      assertThat(response).isNotNull();
      assertThat(response.id()).isEqualTo(EVENT_ID.toString());
      assertThat(response.title()).isEqualTo(TITLE);
      assertThat(response.description()).isNull();
      assertThat(response.location()).isNull();
      assertThat(response.endDate()).isNull();
      assertThat(response.startDate()).isNotNull();
    }

    @Test
    @DisplayName("Devrait retourner null si l'Event source est null")
    void shouldReturnNullIfEventIsNull() {
      // When
      EventResponse response = mapper.toResponse(null);

      // Then
      assertThat(response).isNull();
    }

    @Test
    @DisplayName("Devrait convertir correctement les UUID en String")
    void shouldConvertUuidToString() {
      // Given
      UUID customEventId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
      UUID customOrgId = UUID.fromString("987e6543-e89b-12d3-a456-426614174111");

      Event event =
          Event.builder()
              .id(customEventId)
              .organizationId(customOrgId)
              .title(TITLE)
              .startDate(START_DATE)
              .status(EventStatus.PUBLISHED)
              .build();

      // When
      EventResponse response = mapper.toResponse(event);

      // Then
      assertThat(response.id()).isEqualTo("123e4567-e89b-12d3-a456-426614174000");
      assertThat(response.organizationId()).isEqualTo("987e6543-e89b-12d3-a456-426614174111");
    }

    @Test
    @DisplayName("Devrait préserver les dates exactement (sans modification)")
    void shouldPreserveDatesExactly() {
      // Given
      Instant specificStart = Instant.parse("2025-06-15T09:00:00Z");
      Instant specificEnd = Instant.parse("2025-06-15T18:00:00Z");
      Instant specificCreated = Instant.parse("2025-01-01T10:00:00Z");
      Instant specificUpdated = Instant.parse("2025-02-01T15:30:00Z");

      Event event =
          Event.builder()
              .id(EVENT_ID)
              .organizationId(ORG_ID)
              .title(TITLE)
              .startDate(specificStart)
              .endDate(specificEnd)
              .status(EventStatus.PUBLISHED)
              .build();
      event.setCreatedAt(specificCreated);
      event.setUpdatedAt(specificUpdated);

      // When
      EventResponse response = mapper.toResponse(event);

      // Then
      assertThat(response.startDate()).isEqualTo(specificStart);
      assertThat(response.endDate()).isEqualTo(specificEnd);
      assertThat(response.createdAt()).isEqualTo(specificCreated);
      assertThat(response.updatedAt()).isEqualTo(specificUpdated);
    }
  }

  @Nested
  @DisplayName("toPublicResponse - Conversion vers PublicEventResponse")
  class ToPublicResponseTests {

    @Test
    @DisplayName("Devrait mapper les champs publics sans organizationId")
    void shouldMapPublicFieldsWithoutOrganizationId() {
      // Given
      Event event =
          Event.builder()
              .id(EVENT_ID)
              .organizationId(ORG_ID) // Ne doit PAS apparaître dans PublicEventResponse
              .title(TITLE)
              .description(DESCRIPTION)
              .location(LOCATION)
              .startDate(START_DATE)
              .endDate(END_DATE)
              .status(EventStatus.PUBLISHED)
              .build();

      // When
      PublicEventResponse response = mapper.toPublicResponse(event);

      // Then
      assertThat(response).isNotNull();
      assertThat(response.id()).isEqualTo(EVENT_ID.toString());
      assertThat(response.title()).isEqualTo(TITLE);
      assertThat(response.description()).isEqualTo(DESCRIPTION);
      assertThat(response.location()).isEqualTo(LOCATION);
      assertThat(response.startDate()).isEqualTo(START_DATE);
      assertThat(response.endDate()).isEqualTo(END_DATE);
      assertThat(response.status()).isEqualTo(EventStatus.PUBLISHED);

      // Vérifier que organizationId n'existe pas dans PublicEventResponse
      // (le record ne contient pas ce champ)
    }

    @Test
    @DisplayName("Devrait mapper un Event publié vers PublicEventResponse")
    void shouldMapPublishedEvent() {
      // Given
      Event event =
          Event.builder()
              .id(EVENT_ID)
              .organizationId(ORG_ID)
              .title("Festival de Musique")
              .description("Un grand festival de musique africaine")
              .location("Accra, Ghana")
              .startDate(START_DATE)
              .endDate(END_DATE)
              .status(EventStatus.PUBLISHED)
              .build();

      // When
      PublicEventResponse response = mapper.toPublicResponse(event);

      // Then
      assertThat(response).isNotNull();
      assertThat(response.title()).isEqualTo("Festival de Musique");
      assertThat(response.location()).isEqualTo("Accra, Ghana");
      assertThat(response.status()).isEqualTo(EventStatus.PUBLISHED);
    }

    @Test
    @DisplayName("Devrait gérer les champs optionnels null dans PublicEventResponse")
    void shouldHandleOptionalNullFieldsInPublicResponse() {
      // Given
      Event event =
          Event.builder()
              .id(EVENT_ID)
              .organizationId(ORG_ID)
              .title(TITLE)
              .description(null)
              .location(null)
              .startDate(START_DATE)
              .endDate(null)
              .status(EventStatus.PUBLISHED)
              .build();

      // When
      PublicEventResponse response = mapper.toPublicResponse(event);

      // Then
      assertThat(response).isNotNull();
      assertThat(response.description()).isNull();
      assertThat(response.location()).isNull();
      assertThat(response.endDate()).isNull();
    }

    @Test
    @DisplayName("Devrait retourner null si l'Event source est null")
    void shouldReturnNullIfEventIsNull() {
      // When
      PublicEventResponse response = mapper.toPublicResponse(null);

      // Then
      assertThat(response).isNull();
    }

    @Test
    @DisplayName("Ne devrait pas exposer createdAt et updatedAt dans PublicEventResponse")
    void shouldNotExposeAuditFieldsInPublicResponse() {
      // Given
      Event event =
          Event.builder()
              .id(EVENT_ID)
              .organizationId(ORG_ID)
              .title(TITLE)
              .startDate(START_DATE)
              .status(EventStatus.PUBLISHED)
              .build();
      event.setCreatedAt(CREATED_AT);
      event.setUpdatedAt(UPDATED_AT);

      // When
      PublicEventResponse response = mapper.toPublicResponse(event);

      // Then
      assertThat(response).isNotNull();
      // PublicEventResponse ne contient pas createdAt ni updatedAt
      // Vérifier juste que les champs de base sont présents
      assertThat(response.id()).isNotNull();
      assertThat(response.title()).isEqualTo(TITLE);
    }

    @Test
    @DisplayName("Devrait mapper correctement tous les statuts d'événement")
    void shouldMapAllEventStatuses() {
      // Test pour chaque statut
      for (EventStatus status : EventStatus.values()) {
        // Given
        Event event =
            Event.builder()
                .id(EVENT_ID)
                .organizationId(ORG_ID)
                .title("Event " + status)
                .startDate(START_DATE)
                .status(status)
                .build();

        // When
        PublicEventResponse response = mapper.toPublicResponse(event);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.status())
            .as("Status devrait être mappé correctement pour " + status)
            .isEqualTo(status);
      }
    }
  }

  @Nested
  @DisplayName("Tests de comparaison EventResponse vs PublicEventResponse")
  class ComparisonTests {

    @Test
    @DisplayName(
        "PublicEventResponse devrait contenir un sous-ensemble des champs de EventResponse")
    void publicResponseShouldBeSubsetOfEventResponse() {
      // Given
      Event event =
          Event.builder()
              .id(EVENT_ID)
              .organizationId(ORG_ID)
              .title(TITLE)
              .description(DESCRIPTION)
              .location(LOCATION)
              .startDate(START_DATE)
              .endDate(END_DATE)
              .status(EventStatus.PUBLISHED)
              .build();
      event.setCreatedAt(CREATED_AT);
      event.setUpdatedAt(UPDATED_AT);

      // When
      EventResponse fullResponse = mapper.toResponse(event);
      PublicEventResponse publicResponse = mapper.toPublicResponse(event);

      // Then - Les champs communs doivent être identiques
      assertThat(publicResponse.id()).isEqualTo(fullResponse.id());
      assertThat(publicResponse.title()).isEqualTo(fullResponse.title());
      assertThat(publicResponse.description()).isEqualTo(fullResponse.description());
      assertThat(publicResponse.location()).isEqualTo(fullResponse.location());
      assertThat(publicResponse.startDate()).isEqualTo(fullResponse.startDate());
      assertThat(publicResponse.endDate()).isEqualTo(fullResponse.endDate());
      assertThat(publicResponse.status()).isEqualTo(fullResponse.status());

      // EventResponse contient des champs supplémentaires
      assertThat(fullResponse.organizationId()).isNotNull();
      assertThat(fullResponse.createdAt()).isNotNull();
      assertThat(fullResponse.updatedAt()).isNotNull();
    }
  }

  @Nested
  @DisplayName("Tests de cas limites")
  class EdgeCaseTests {

    @Test
    @DisplayName("Devrait gérer un Event avec uniquement les champs obligatoires")
    void shouldHandleEventWithOnlyRequiredFields() {
      // Given - Seulement les champs marqués @Column(nullable = false)
      Event minimalEvent =
          Event.builder()
              .id(EVENT_ID)
              .organizationId(ORG_ID)
              .title("Titre minimal")
              .startDate(START_DATE)
              .status(EventStatus.DRAFT)
              .build();

      // When
      EventResponse response = mapper.toResponse(minimalEvent);
      PublicEventResponse publicResponse = mapper.toPublicResponse(minimalEvent);

      // Then
      assertThat(response).isNotNull();
      assertThat(response.id()).isNotNull();
      assertThat(response.title()).isEqualTo("Titre minimal");
      assertThat(response.description()).isNull();
      assertThat(response.location()).isNull();
      assertThat(response.endDate()).isNull();

      assertThat(publicResponse).isNotNull();
      assertThat(publicResponse.title()).isEqualTo("Titre minimal");
    }

    @Test
    @DisplayName("Devrait gérer des titres avec caractères spéciaux et émojis")
    void shouldHandleSpecialCharactersInTitle() {
      // Given
      String specialTitle = "Conférence Tech 🚀 2025 - L'IA & l'Afrique";
      Event event =
          Event.builder()
              .id(EVENT_ID)
              .organizationId(ORG_ID)
              .title(specialTitle)
              .startDate(START_DATE)
              .status(EventStatus.PUBLISHED)
              .build();

      // When
      EventResponse response = mapper.toResponse(event);
      PublicEventResponse publicResponse = mapper.toPublicResponse(event);

      // Then
      assertThat(response.title()).isEqualTo(specialTitle);
      assertThat(publicResponse.title()).isEqualTo(specialTitle);
    }

    @Test
    @DisplayName("Devrait gérer des descriptions très longues")
    void shouldHandleLongDescription() {
      // Given
      String longDescription = "A".repeat(5000); // Description de 5000 caractères
      Event event =
          Event.builder()
              .id(EVENT_ID)
              .organizationId(ORG_ID)
              .title(TITLE)
              .description(longDescription)
              .startDate(START_DATE)
              .status(EventStatus.PUBLISHED)
              .build();

      // When
      EventResponse response = mapper.toResponse(event);
      PublicEventResponse publicResponse = mapper.toPublicResponse(event);

      // Then
      assertThat(response.description()).hasSize(5000);
      assertThat(publicResponse.description()).hasSize(5000);
    }
  }
}
