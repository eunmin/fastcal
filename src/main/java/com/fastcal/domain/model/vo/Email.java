package com.fastcal.domain.model.vo;

import java.util.regex.Pattern;

public record Email(String value) {

  private static final int MAX_LENGTH = 255;
  private static final Pattern EMAIL_PATTERN =
      Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

  public Email {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Email cannot be null or blank");
    }
    value = value.trim().toLowerCase();
    if (value.length() > MAX_LENGTH) {
      throw new IllegalArgumentException("Email cannot exceed " + MAX_LENGTH + " characters");
    }
    if (!EMAIL_PATTERN.matcher(value).matches()) {
      throw new IllegalArgumentException("Email must be a valid email format");
    }
  }

  public static Email of(String value) {
    return new Email(value);
  }

  public static Email ofNullable(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return new Email(value);
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return value;
  }
}
