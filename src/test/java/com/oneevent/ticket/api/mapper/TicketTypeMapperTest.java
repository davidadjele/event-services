package com.oneevent.ticket.api.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import com.oneevent.ticket.api.dto.PublicTicketTypeResponse;
import com.oneevent.ticket.api.dto.TicketTypeResponse;
import com.oneevent.ticket.domain.TicketType;

/**
 * Tests unitaires pour le mapper TicketTypeMapper.
 *
 * <p>Cette classe teste les fonctionnalités de mapping :
 *
 * <ul>
 *   <li>Conversion TicketType → TicketTypeResponse (avec tous les champs)
 *   <li>Conversion TicketType → PublicTicketTypeResponse (sans les dates d'audit)
 *   <li>Gestion des valeurs null
 *   <li>Conversion correcte des UUID en String
 *   <li>Préservation des dates et BigDecimal
 * </ul>
 */
@DisplayName("TicketTypeMapper - Tests du mapper de types de tickets")
class TicketTypeMapperTest {

  private TicketTypeMapper mapper;

  private static final UUID TICKET_TYPE_ID = UUID.randomUUID();
  private static final UUID EVENT_ID = UUID.randomUUID();
  private static final UUID ORG_ID = UUID.randomUUID();
  private static final String NAME = "Early Bird";
  private static final BigDecimal PRICE = new BigDecimal("25.00");
  private static final int QUANTITY_AVAILABLE = 100;
  private static final int QUANTITY_SOLD = 25;
  private static final Instant SALE_START = Instant.now().plus(1, ChronoUnit.DAYS);
  private static final Instant SALE_END = Instant.now().plus(30, ChronoUnit.DAYS);
  private static final Instant CREATED_AT = Instant.now().minus(7, ChronoUnit.DAYS);
  private static final Instant UPDATED_AT = Instant.now().minus(1, ChronoUnit.DAYS);

  @BeforeEach
  void setUp() {
    mapper = Mappers.getMapper(TicketTypeMapper.class);
  }

  @Nested
  @DisplayName("toResponse - Conversion vers TicketTypeResponse")
  class ToResponseTests {

    @Test
    @DisplayName("Devrait mapper tous les champs d'un TicketType vers TicketTypeResponse")
    void shouldMapAllFieldsToTicketTypeResponse() {
      // Given
      TicketType ticketType = createFullTicketType();

      // When
      TicketTypeResponse response = mapper.toResponse(ticketType);

      // Then
      assertThat(response).isNotNull();
      assertThat(response.id()).isEqualTo(TICKET_TYPE_ID.toString());
      assertThat(response.eventId()).isEqualTo(EVENT_ID.toString());
      assertThat(response.name()).isEqualTo(NAME);
      assertThat(response.price()).isEqualByComparingTo(PRICE);
      assertThat(response.quantityAvailable()).isEqualTo(QUANTITY_AVAILABLE);
      assertThat(response.quantitySold()).isEqualTo(QUANTITY_SOLD);
      assertThat(response.saleStart()).isEqualTo(SALE_START);
      assertThat(response.saleEnd()).isEqualTo(SALE_END);
      assertThat(response.createdAt()).isEqualTo(CREATED_AT);
      assertThat(response.updatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    @DisplayName("Devrait mapper un TicketType avec prix à zéro (ticket gratuit)")
    void shouldMapTicketTypeWithZeroPrice() {
      // Given
      TicketType ticketType =
          TicketType.builder()
              .id(TICKET_TYPE_ID)
              .organizationId(ORG_ID)
              .eventId(EVENT_ID)
              .name("Gratuit")
              .price(BigDecimal.ZERO)
              .quantityAvailable(50)
              .quantitySold(0)
              .build();

      // When
      TicketTypeResponse response = mapper.toResponse(ticketType);

      // Then
      assertThat(response).isNotNull();
      assertThat(response.price()).isEqualByComparingTo(BigDecimal.ZERO);
      assertThat(response.name()).isEqualTo("Gratuit");
    }

    @Test
    @DisplayName("Devrait gérer les champs optionnels null (saleStart, saleEnd)")
    void shouldHandleOptionalNullFields() {
      // Given
      TicketType ticketType =
          TicketType.builder()
              .id(TICKET_TYPE_ID)
              .organizationId(ORG_ID)
              .eventId(EVENT_ID)
              .name(NAME)
              .price(PRICE)
              .quantityAvailable(QUANTITY_AVAILABLE)
              .quantitySold(0)
              .saleStart(null)
              .saleEnd(null)
              .build();

      // When
      TicketTypeResponse response = mapper.toResponse(ticketType);

      // Then
      assertThat(response).isNotNull();
      assertThat(response.saleStart()).isNull();
      assertThat(response.saleEnd()).isNull();
      assertThat(response.name()).isEqualTo(NAME);
    }

    @Test
    @DisplayName("Devrait retourner null si l'entité source est null")
    void shouldReturnNullWhenSourceIsNull() {
      // When
      TicketTypeResponse response = mapper.toResponse(null);

      // Then
      assertThat(response).isNull();
    }

    @Test
    @DisplayName("Devrait mapper correctement un TicketType avec quantité vendue élevée")
    void shouldMapTicketTypeWithHighQuantitySold() {
      // Given
      TicketType ticketType =
          TicketType.builder()
              .id(TICKET_TYPE_ID)
              .organizationId(ORG_ID)
              .eventId(EVENT_ID)
              .name("VIP")
              .price(new BigDecimal("150.00"))
              .quantityAvailable(100)
              .quantitySold(99)
              .saleStart(SALE_START)
              .saleEnd(SALE_END)
              .build();

      // When
      TicketTypeResponse response = mapper.toResponse(ticketType);

      // Then
      assertThat(response.quantityAvailable()).isEqualTo(100);
      assertThat(response.quantitySold()).isEqualTo(99);
    }
  }

  @Nested
  @DisplayName("toPublicResponse - Conversion vers PublicTicketTypeResponse")
  class ToPublicResponseTests {

    @Test
    @DisplayName("Devrait mapper tous les champs vers PublicTicketTypeResponse")
    void shouldMapAllFieldsToPublicResponse() {
      // Given
      TicketType ticketType = createFullTicketType();

      // When
      PublicTicketTypeResponse response = mapper.toPublicResponse(ticketType);

      // Then
      assertThat(response).isNotNull();
      assertThat(response.id()).isEqualTo(TICKET_TYPE_ID.toString());
      assertThat(response.eventId()).isEqualTo(EVENT_ID.toString());
      assertThat(response.name()).isEqualTo(NAME);
      assertThat(response.price()).isEqualByComparingTo(PRICE);
      assertThat(response.quantityAvailable()).isEqualTo(QUANTITY_AVAILABLE);
      assertThat(response.quantitySold()).isEqualTo(QUANTITY_SOLD);
      assertThat(response.saleStart()).isEqualTo(SALE_START);
      assertThat(response.saleEnd()).isEqualTo(SALE_END);
    }

    @Test
    @DisplayName("Devrait retourner null si l'entité source est null")
    void shouldReturnNullWhenSourceIsNull() {
      // When
      PublicTicketTypeResponse response = mapper.toPublicResponse(null);

      // Then
      assertThat(response).isNull();
    }

    @Test
    @DisplayName("Devrait mapper un TicketType avec fenêtre de vente sans dates")
    void shouldMapTicketTypeWithoutSaleDates() {
      // Given
      TicketType ticketType =
          TicketType.builder()
              .id(TICKET_TYPE_ID)
              .organizationId(ORG_ID)
              .eventId(EVENT_ID)
              .name("Standard")
              .price(new BigDecimal("50.00"))
              .quantityAvailable(200)
              .quantitySold(50)
              .saleStart(null)
              .saleEnd(null)
              .build();

      // When
      PublicTicketTypeResponse response = mapper.toPublicResponse(ticketType);

      // Then
      assertThat(response).isNotNull();
      assertThat(response.saleStart()).isNull();
      assertThat(response.saleEnd()).isNull();
    }
  }

  @Nested
  @DisplayName("Tests de comparaison et cohérence")
  class ComparisonTests {

    @Test
    @DisplayName(
        "toResponse et toPublicResponse devraient avoir les mêmes valeurs pour les champs communs")
    void shouldHaveSameCommonFieldValues() {
      // Given
      TicketType ticketType = createFullTicketType();

      // When
      TicketTypeResponse fullResponse = mapper.toResponse(ticketType);
      PublicTicketTypeResponse publicResponse = mapper.toPublicResponse(ticketType);

      // Then
      assertThat(fullResponse.id()).isEqualTo(publicResponse.id());
      assertThat(fullResponse.eventId()).isEqualTo(publicResponse.eventId());
      assertThat(fullResponse.name()).isEqualTo(publicResponse.name());
      assertThat(fullResponse.price()).isEqualByComparingTo(publicResponse.price());
      assertThat(fullResponse.quantityAvailable()).isEqualTo(publicResponse.quantityAvailable());
      assertThat(fullResponse.quantitySold()).isEqualTo(publicResponse.quantitySold());
      assertThat(fullResponse.saleStart()).isEqualTo(publicResponse.saleStart());
      assertThat(fullResponse.saleEnd()).isEqualTo(publicResponse.saleEnd());
    }

    @Test
    @DisplayName("Devrait mapper différents types de tickets avec des prix variés")
    void shouldMapDifferentPriceRanges() {
      // Given - prix très élevé
      TicketType expensiveTicket =
          TicketType.builder()
              .id(UUID.randomUUID())
              .organizationId(ORG_ID)
              .eventId(EVENT_ID)
              .name("Platinum VIP")
              .price(new BigDecimal("9999.99"))
              .quantityAvailable(10)
              .quantitySold(2)
              .build();

      // When
      TicketTypeResponse response = mapper.toResponse(expensiveTicket);

      // Then
      assertThat(response.price()).isEqualByComparingTo(new BigDecimal("9999.99"));
      assertThat(response.name()).isEqualTo("Platinum VIP");
    }
  }

  @Nested
  @DisplayName("Tests des cas limites")
  class EdgeCaseTests {

    @Test
    @DisplayName("Devrait mapper un TicketType avec quantité disponible égale à vendue")
    void shouldMapSoldOutTicketType() {
      // Given
      TicketType soldOutTicket =
          TicketType.builder()
              .id(TICKET_TYPE_ID)
              .organizationId(ORG_ID)
              .eventId(EVENT_ID)
              .name("Sold Out Ticket")
              .price(PRICE)
              .quantityAvailable(50)
              .quantitySold(50)
              .build();

      // When
      TicketTypeResponse response = mapper.toResponse(soldOutTicket);

      // Then
      assertThat(response.quantityAvailable()).isEqualTo(response.quantitySold());
    }

    @Test
    @DisplayName("Devrait mapper un nom de ticket avec caractères spéciaux")
    void shouldMapTicketNameWithSpecialCharacters() {
      // Given
      TicketType ticketType =
          TicketType.builder()
              .id(TICKET_TYPE_ID)
              .organizationId(ORG_ID)
              .eventId(EVENT_ID)
              .name("Billet VIP - Accès Backstage & Meet'n'Greet")
              .price(PRICE)
              .quantityAvailable(20)
              .quantitySold(5)
              .build();

      // When
      TicketTypeResponse response = mapper.toResponse(ticketType);

      // Then
      assertThat(response.name()).isEqualTo("Billet VIP - Accès Backstage & Meet'n'Greet");
    }

    @Test
    @DisplayName("Devrait mapper les dates de vente proches (même jour)")
    void shouldMapSameDaySaleWindow() {
      // Given
      Instant sameDay = Instant.now();
      TicketType ticketType =
          TicketType.builder()
              .id(TICKET_TYPE_ID)
              .organizationId(ORG_ID)
              .eventId(EVENT_ID)
              .name("Flash Sale")
              .price(new BigDecimal("10.00"))
              .quantityAvailable(100)
              .quantitySold(0)
              .saleStart(sameDay)
              .saleEnd(sameDay.plus(1, ChronoUnit.HOURS))
              .build();

      // When
      TicketTypeResponse response = mapper.toResponse(ticketType);

      // Then
      assertThat(response.saleStart()).isBefore(response.saleEnd());
    }
  }

  // ===== Helper methods =====

  private TicketType createFullTicketType() {
    TicketType ticketType =
        TicketType.builder()
            .id(TICKET_TYPE_ID)
            .organizationId(ORG_ID)
            .eventId(EVENT_ID)
            .name(NAME)
            .price(PRICE)
            .quantityAvailable(QUANTITY_AVAILABLE)
            .quantitySold(QUANTITY_SOLD)
            .saleStart(SALE_START)
            .saleEnd(SALE_END)
            .build();
    ticketType.setCreatedAt(CREATED_AT);
    ticketType.setUpdatedAt(UPDATED_AT);
    return ticketType;
  }
}
