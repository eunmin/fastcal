package com.fastcal.domain.model.vo;

public record CTag(String value) {

  public CTag {
    if (value != null) {
      value = value.trim();
      if (value.isEmpty()) {
        value = null;
      }
    }
  }

  public static CTag of(String value) {
    return new CTag(value);
  }

  public static CTag ofNullable(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return new CTag(value);
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return value;
  }
}
