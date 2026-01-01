package com.fastcal.domain.model.vo;

import java.util.regex.Pattern;

public record UserId(String value) {

  private static final int MAX_LENGTH = 255;
  private static final Pattern USER_ID_PATTERN =
      Pattern.compile("^[a-zA-Z0-9._%+@-]+$");

  public UserId {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("UserId cannot be null or blank");
    }
    if (value.length() > MAX_LENGTH) {
      throw new IllegalArgumentException("UserId cannot exceed " + MAX_LENGTH + " characters");
    }
    if (!USER_ID_PATTERN.matcher(value).matches()) {
      throw new IllegalArgumentException("UserId contains invalid characters");
    }
  }

  public static UserId of(String value) {
    return new UserId(value);
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return value;
  }
}
