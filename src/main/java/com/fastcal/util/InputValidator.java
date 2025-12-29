package com.fastcal.util;

import java.util.regex.Pattern;

public final class InputValidator {

  private InputValidator() {
  }

  private static final Pattern USER_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
  private static final Pattern CALENDAR_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,100}$");
  private static final Pattern EVENT_UID_PATTERN = Pattern.compile("^[a-zA-Z0-9._@-]{1,255}$");
  private static final Pattern DEPTH_PATTERN = Pattern.compile("^(0|1|infinity)$");

  public static boolean isValidUserId(String userId) {
    if (userId == null || userId.isEmpty()) {
      return false;
    }
    return USER_ID_PATTERN.matcher(userId).matches();
  }

  public static boolean isValidCalendarId(String calendarId) {
    if (calendarId == null || calendarId.isEmpty()) {
      return false;
    }
    return CALENDAR_ID_PATTERN.matcher(calendarId).matches();
  }

  public static boolean isValidEventUid(String eventUid) {
    if (eventUid == null || eventUid.isEmpty()) {
      return false;
    }
    return EVENT_UID_PATTERN.matcher(eventUid).matches();
  }

  public static boolean isValidDepth(String depth) {
    if (depth == null || depth.isEmpty()) {
      return true;
    }
    return DEPTH_PATTERN.matcher(depth).matches();
  }

  public static String sanitizeDepth(String depth) {
    if (depth == null || !isValidDepth(depth)) {
      return "0";
    }
    return depth;
  }
}
