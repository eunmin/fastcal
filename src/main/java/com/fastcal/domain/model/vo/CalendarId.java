package com.fastcal.domain.model.vo;

import java.util.regex.Pattern;

public record CalendarId(String value) {

  private static final int MAX_LENGTH = 100;
  private static final Pattern CALENDAR_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");

  public CalendarId {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("CalendarId cannot be null or blank");
    }
    if (value.length() > MAX_LENGTH) {
      throw new IllegalArgumentException("CalendarId cannot exceed " + MAX_LENGTH + " characters");
    }
    if (!CALENDAR_ID_PATTERN.matcher(value).matches()) {
      throw new IllegalArgumentException("CalendarId must contain only alphanumeric characters, underscores, or hyphens");
    }
  }

  public static CalendarId of(String value) {
    return new CalendarId(value);
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return value;
  }
}
