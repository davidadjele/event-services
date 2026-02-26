package com.oneevent.shared.constants;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class ApiPaths {
  public static final String API_V1 = "/api/v1";

  public static final String AUTH = API_V1 + "/auth";
  public static final String PUBLIC = API_V1 + "/public";

  public static final String EVENTS = API_V1 + "/events";
  public static final String TICKET_TYPES = API_V1 + "/ticket-types";

  public static final String PUBLIC_EVENTS = PUBLIC + "/events";
  public static final String PUBLIC_TICKET_TYPES = PUBLIC + "/ticket-types";
}
