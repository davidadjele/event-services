package com.oneevent.event.api;

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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

  private final EventService service;
  private final EventMapper mapper;

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

  @GetMapping
  public PageResponse<EventResponse> listMine(
      @RequestParam(name = "orgId", required = false) UUID orgId, Pageable pageable) {
    Page<EventResponse> mapped = service.listMine(orgId, pageable).map(mapper::toResponse);
    return PageResponse.of(mapped);
  }

  @GetMapping("/{id}")
  public EventResponse getMine(
      @PathVariable UUID id, @RequestParam(name = "orgId", required = false) UUID orgId) {
    return mapper.toResponse(service.getMine(orgId, id));
  }

  @PatchMapping("/{id}")
  public EventResponse update(
      @PathVariable UUID id,
      @RequestParam(name = "orgId", required = false) UUID orgId,
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

  @DeleteMapping("/{id}")
  public void delete(
      @PathVariable UUID id, @RequestParam(name = "orgId", required = false) UUID orgId) {
    service.softDelete(id, orgId);
  }
}
