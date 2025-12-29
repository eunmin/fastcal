package com.fastcal.handler;

import java.util.List;

public record CalendarFilter(
    String componentType,
    TimeRange timeRange,
    List<PropFilter> propFilters) {
  public CalendarFilter {
    if (componentType == null)
      componentType = "VCALENDAR";
    if (propFilters == null)
      propFilters = List.of();
  }
}
