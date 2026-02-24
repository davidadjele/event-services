package com.oneevent.event.api.mapper;

import org.mapstruct.Mapper;

import com.oneevent.event.api.dto.EventResponse;
import com.oneevent.event.api.dto.PublicEventResponse;
import com.oneevent.event.domain.Event;

@Mapper(componentModel = "spring")
public interface EventMapper {
  EventResponse toResponse(Event event);

  PublicEventResponse toPublicResponse(Event event);
}
