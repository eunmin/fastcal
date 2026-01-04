package com.fastcal.domain.model.vo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public record ICalDateTime(LocalDateTime value) {

  private static final DateTimeFormatter ICAL_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

  public ICalDateTime {
    if (value == null) {
      throw new IllegalArgumentException("ICalDateTime value cannot be null");
    }
  }

  public static ICalDateTime of(LocalDateTime value) {
    return new ICalDateTime(value);
  }

  public static ICalDateTime parse(String isoString) {
    if (isoString == null) {
      return new ICalDateTime(LocalDateTime.MIN);
    }
    try {
      if (isoString.contains("T") && !isoString.contains("-")) {
        return new ICalDateTime(LocalDateTime.parse(isoString, ICAL_FORMAT));
      }
      return new ICalDateTime(LocalDateTime.parse(isoString));
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException("Invalid date-time format: " + isoString);
    }
  }

  public String toICalFormat() {
    return value.format(ICAL_FORMAT);
  }

  public LocalDateTime getValue() {
    return value;
  }
}
