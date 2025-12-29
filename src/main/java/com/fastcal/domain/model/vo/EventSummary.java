package com.fastcal.domain.model.vo;

/**
 * Value object representing an iCalendar SUMMARY property.
 *
 * According to RFC 5545, SUMMARY is a TEXT property that defines a short summary
 * or subject for the calendar component. While RFC 5545 does not specify a maximum
 * length for TEXT properties, practical limits are applied for storage and display.
 *
 * The SUMMARY property is typically used as the event title in calendar applications.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc5545">RFC 5545</a>
 */
public record EventSummary(String value) {

  // Practical limit for summary/title field
  private static final int MAX_LENGTH = 1000;

  public EventSummary {
    if (value != null) {
      value = value.trim();
      if (value.isEmpty()) {
        value = null;
      } else {
        if (value.length() > MAX_LENGTH) {
          throw new IllegalArgumentException(
              "EventSummary cannot exceed " + MAX_LENGTH + " characters");
        }
        // RFC 5545: TEXT values cannot contain raw CRLF (must be escaped as \\n)
        // Remove any control characters except for escaped sequences
        value = sanitizeText(value);
      }
    }
  }

  private static String sanitizeText(String text) {
    // Replace actual newlines with escaped newlines as per RFC 5545
    // (in iCalendar format, newlines in TEXT are represented as \n or \N)
    return text
        .replace("\r\n", " ")
        .replace("\r", " ")
        .replace("\n", " ");
  }

  /**
   * Creates a new EventSummary from the given string value.
   */
  public static EventSummary of(String value) {
    return new EventSummary(value);
  }

  /**
   * Creates a new EventSummary from a nullable string value.
   * Returns null if the input is null or blank.
   */
  public static EventSummary ofNullable(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return new EventSummary(value);
  }

  /**
   * Returns true if this summary is empty or null.
   */
  public boolean isEmpty() {
    return value == null || value.isBlank();
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return value != null ? value : "";
  }
}
