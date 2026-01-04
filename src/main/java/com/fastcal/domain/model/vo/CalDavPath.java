package com.fastcal.domain.model.vo;

public final class CalDavPath {

  private CalDavPath() {
  }

  public static String principal(UserId userId) {
    return "/principals/" + userId.getValue() + "/";
  }

  public static String calendarHome(UserId userId) {
    return "/calendars/" + userId.getValue() + "/";
  }

  public static String calendar(UserId userId, CalendarId calendarId) {
    return "/calendars/" + userId.getValue() + "/" + calendarId.getValue() + "/";
  }

  public static String event(UserId userId, CalendarId calendarId, String eventUid) {
    return "/calendars/" + userId.getValue() + "/" + calendarId.getValue() + "/" + eventUid + ".ics";
  }

  public static String scheduleInbox(UserId userId) {
    return "/calendars/" + userId.getValue() + "/inbox/";
  }

  public static String scheduleOutbox(UserId userId) {
    return "/calendars/" + userId.getValue() + "/outbox/";
  }

  public static String extractEventUid(String href) {
    String[] parts = href.split("/");
    String filename = parts[parts.length - 1];
    return filename.replace(".ics", "");
  }
}
