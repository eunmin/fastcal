package com.fastcal.domain.model.vo;

public record CalendarDescription(String value) {

  private static final int MAX_LENGTH = 1000;

  public CalendarDescription {
    if (value != null) {
      value = value.trim();
      if (value.isEmpty()) {
        value = null;
      } else if (value.length() > MAX_LENGTH) {
        throw new IllegalArgumentException("CalendarDescription cannot exceed " + MAX_LENGTH + " characters");
      }
    }
  }

  public static CalendarDescription of(String value) {
    return new CalendarDescription(value);
  }

  public static CalendarDescription ofNullable(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return new CalendarDescription(value);
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return value;
  }
}
