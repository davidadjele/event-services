package com.oneevent.shared.exception;

import java.time.Instant;

public record ApiError(
    Instant timestamp,
    String path,
    int status,
    String error,
    String message,
    String code,
    String traceId) {}
