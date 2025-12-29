package com.fastcal.domain.model.vo;

public record SyncToken(String value) {

  private static final String SYNC_TOKEN_PREFIX = "http://fastcal.com/ns/sync/";

  public SyncToken {
    if (value != null) {
      value = value.trim();
      if (value.isEmpty()) {
        value = null;
      }
    }
  }

  public static SyncToken of(String value) {
    return new SyncToken(value);
  }

  public static SyncToken ofNullable(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return new SyncToken(value);
  }

  public static SyncToken generate() {
    return new SyncToken(SYNC_TOKEN_PREFIX + System.currentTimeMillis());
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return value;
  }
}
