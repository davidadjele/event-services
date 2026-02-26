package com.oneevent.shared.constants;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class ErrorCodes {
  public static final String AUTH_REQUIRED = "AUTH_REQUIRED";
  public static final String ORG_REQUIRED = "ORG_REQUIRED";
  public static final String ORG_FILTER_REQUIRED = "ORG_FILTER_REQUIRED";

  public static final String EVENT_NOT_FOUND = "EVENT_NOT_FOUND";
  public static final String EVENT_NOT_PUBLISHED = "EVENT_NOT_PUBLISHED";
  public static final String TICKET_TYPE_NOT_FOUND = "TICKET_TYPE_NOT_FOUND";

  public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
  public static final String INVALID_DATES = "INVALID_DATES";
  public static final String INVALID_TICKET_TYPE = "INVALID_TICKET_TYPE";
}
