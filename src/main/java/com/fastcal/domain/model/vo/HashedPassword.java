package com.fastcal.domain.model.vo;

public record HashedPassword(String value) {

  public HashedPassword {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("HashedPassword cannot be null or blank");
    }
    value = value.trim();
  }

  public static HashedPassword of(String value) {
    return new HashedPassword(value);
  }

  public static HashedPassword ofNullable(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return new HashedPassword(value);
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return "[PROTECTED]";
  }
}
