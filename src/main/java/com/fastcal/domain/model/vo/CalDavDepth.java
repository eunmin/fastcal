package com.fastcal.domain.model.vo;

public enum CalDavDepth {
  ZERO("0"),
  ONE("1"),
  INFINITY("infinity");

  private final String value;

  CalDavDepth(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public boolean includesChildren() {
    return this == ONE || this == INFINITY;
  }

  public static CalDavDepth of(String value) {
    if (value == null) {
      return ZERO;
    }
    return switch (value.toLowerCase()) {
      case "1" -> ONE;
      case "infinity" -> INFINITY;
      default -> ZERO;
    };
  }
}
