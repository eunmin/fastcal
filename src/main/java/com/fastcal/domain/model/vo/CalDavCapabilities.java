package com.fastcal.domain.model.vo;

import java.util.List;

public final class CalDavCapabilities {

  private CalDavCapabilities() {
  }

  public static final String CALENDAR_CONTENT_TYPE = "text/calendar; charset=utf-8";

  public static final List<String> SUPPORTED_CALENDAR_COMPONENTS = List.of("VEVENT", "VTODO");

  public static final List<String> SUPPORTED_REPORTS = List.of(
      "calendar-query",
      "calendar-multiget",
      "sync-collection",
      "free-busy-query"
  );

  public static final List<String> CALENDAR_HOME_REPORTS = List.of(
      "calendar-query",
      "calendar-multiget",
      "sync-collection"
  );

  public static final List<String> PRIVILEGES = List.of(
      "read",
      "write",
      "write-content",
      "write-properties",
      "bind",
      "unbind"
  );
}
