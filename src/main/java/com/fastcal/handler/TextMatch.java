package com.fastcal.handler;

public record TextMatch(
    String value,
    String collation,
    boolean negate) {
  public TextMatch(String value) {
    this(value, "i;unicode-casemap", false);
  }
}
