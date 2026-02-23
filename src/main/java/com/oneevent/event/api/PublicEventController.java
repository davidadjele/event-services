package com.oneevent.event.api;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import com.oneevent.event.api.dto.PublicEventResponse;
import com.oneevent.event.api.mapper.EventMapper;
import com.oneevent.event.application.EventService;
import com.oneevent.shared.api.PageResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/public/events")
public class PublicEventController {

  private final EventService service;
  private final EventMapper mapper;

  @GetMapping
  public PageResponse<PublicEventResponse> listPublished(Pageable pageable) {
    Page<PublicEventResponse> mapped =
        service.listPublished(pageable).map(mapper::toPublicResponse);
    return PageResponse.of(mapped);
  }

  @GetMapping("/{id}")
  public PublicEventResponse getPublished(@PathVariable UUID id) {
    return mapper.toPublicResponse(service.getPublished(id));
  }
}
