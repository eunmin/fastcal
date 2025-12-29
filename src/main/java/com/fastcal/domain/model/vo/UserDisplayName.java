package com.fastcal.domain.model.vo;

public record UserDisplayName(String value) {

  private static final int MAX_LENGTH = 100;

  public UserDisplayName {
    if (value != null) {
      value = value.trim();
      if (value.isEmpty()) {
        value = null;
      } else if (value.length() > MAX_LENGTH) {
        throw new IllegalArgumentException("UserDisplayName cannot exceed " + MAX_LENGTH + " characters");
      }
    }
  }

  public static UserDisplayName of(String value) {
    return new UserDisplayName(value);
  }

  public static UserDisplayName ofNullable(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return new UserDisplayName(value);
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return value;
  }
}
