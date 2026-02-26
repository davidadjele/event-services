package com.oneevent.ticket.api.mapper;

import org.mapstruct.Mapper;

import com.oneevent.shared.config.GlobalMapperConfig;
import com.oneevent.ticket.api.dto.PublicTicketTypeResponse;
import com.oneevent.ticket.api.dto.TicketTypeResponse;
import com.oneevent.ticket.domain.TicketType;

@Mapper(config = GlobalMapperConfig.class)
public interface TicketTypeMapper {
  TicketTypeResponse toResponse(TicketType ticketType);

  PublicTicketTypeResponse toPublicResponse(TicketType ticketType);
}
