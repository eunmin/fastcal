package com.fastcal.service.caldav;

public record EventSaveResult(
    String etag,
    boolean created) {
  public String getEtag() {
    return etag;
  }

  public boolean isCreated() {
    return created;
  }
}
