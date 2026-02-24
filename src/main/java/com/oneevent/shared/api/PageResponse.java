package com.oneevent.shared.api;

import java.util.List;

import org.springframework.data.domain.Page;

public record PageResponse<T>(
    List<T> content, int page, int size, long totalElements, int totalPages, boolean last) {
  public static <T> PageResponse<T> of(Page<T> page) {
    return new PageResponse<>(
        page.getContent(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages(),
        page.isLast());
  }
}
