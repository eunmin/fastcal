package com.fastcal.domain.model.vo;

import java.time.ZoneId;
import java.util.regex.Pattern;

/**
 * Value object representing a calendar's timezone.
 *
 * Supports two formats per RFC 4791:
 * 1. VTIMEZONE component (iCalendar format) - stored as-is
 * 2. IANA timezone identifier (e.g., "Asia/Seoul") - validated against ZoneId
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc4791#section-5.2.2">RFC 4791 CALDAV:calendar-timezone</a>
 */
public record CalendarTimezone(String value) {

  private static final Pattern VTIMEZONE_PATTERN = Pattern.compile(
      "^\\s*BEGIN:VTIMEZONE.*END:VTIMEZONE\\s*$", Pattern.DOTALL);

  private static final Pattern VCALENDAR_WITH_VTIMEZONE_PATTERN = Pattern.compile(
      "^\\s*BEGIN:VCALENDAR.*BEGIN:VTIMEZONE.*END:VTIMEZONE.*END:VCALENDAR\\s*$", Pattern.DOTALL);

  public CalendarTimezone {
    if (value != null) {
      value = value.trim();
      if (value.isEmpty()) {
        value = null;
      } else if (!isValidTimezone(value)) {
        throw new IllegalArgumentException("Invalid timezone: must be VTIMEZONE component or valid IANA timezone ID");
      }
    }
  }

  public static CalendarTimezone of(String value) {
    return new CalendarTimezone(value);
  }

  public static CalendarTimezone ofNullable(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return new CalendarTimezone(value);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  public static CalendarTimezone ofZoneId(ZoneId zoneId) {
    return zoneId != null ? new CalendarTimezone(zoneId.getId()) : null;
  }

  public String getValue() {
    return value;
  }

  public boolean isVTimezone() {
    return value != null && (VTIMEZONE_PATTERN.matcher(value).matches()
        || VCALENDAR_WITH_VTIMEZONE_PATTERN.matcher(value).matches());
  }

  public boolean isIanaTimezone() {
    return value != null && !isVTimezone();
  }

  public ZoneId toZoneId() {
    if (value == null || isVTimezone()) {
      return null;
    }
    return ZoneId.of(value);
  }

  private static boolean isValidTimezone(String tz) {
    if (VTIMEZONE_PATTERN.matcher(tz).matches()
        || VCALENDAR_WITH_VTIMEZONE_PATTERN.matcher(tz).matches()) {
      return true;
    }
    try {
      ZoneId.of(tz);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public String toString() {
    return value;
  }
}
