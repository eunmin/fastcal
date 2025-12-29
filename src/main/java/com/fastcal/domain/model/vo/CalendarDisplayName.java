package com.fastcal.domain.model.vo;

public record CalendarDisplayName(String value) {

  private static final int MAX_LENGTH = 255;

  public CalendarDisplayName {
    if (value != null) {
      value = value.trim();
      if (value.isEmpty()) {
        value = null;
      } else if (value.length() > MAX_LENGTH) {
        throw new IllegalArgumentException("CalendarDisplayName cannot exceed " + MAX_LENGTH + " characters");
      }
    }
  }

  public static CalendarDisplayName of(String value) {
    return new CalendarDisplayName(value);
  }

  public static CalendarDisplayName ofNullable(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return new CalendarDisplayName(value);
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return value;
  }
}
