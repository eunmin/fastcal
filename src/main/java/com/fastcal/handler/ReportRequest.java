package com.fastcal.handler;

import java.util.List;

public record ReportRequest(
    ReportType type,
    List<String> requestedProps,
    List<String> hrefs,
    CalendarFilter filter,
    String syncToken) {
  public ReportRequest {
    if (requestedProps == null)
      requestedProps = List.of();
    if (hrefs == null)
      hrefs = List.of();
  }
}
