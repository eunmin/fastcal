package com.fastcal.domain.model.vo;

/**
 * Value object representing an iCalendar DESCRIPTION property.
 *
 * According to RFC 5545, DESCRIPTION is a TEXT property that provides a more
 * complete description of the calendar component than that provided by the
 * SUMMARY property.
 *
 * While RFC 5545 does not specify a maximum length for TEXT properties,
 * practical limits are applied for storage considerations.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc5545">RFC 5545</a>
 */
public record EventDescription(String value) {

  // Practical limit for description field (larger than summary as it's meant for detailed content)
  private static final int MAX_LENGTH = 10000;

  public EventDescription {
    if (value != null) {
      value = value.trim();
      if (value.isEmpty()) {
        value = null;
      } else {
        if (value.length() > MAX_LENGTH) {
          throw new IllegalArgumentException(
              "EventDescription cannot exceed " + MAX_LENGTH + " characters");
        }
        // Unlike summary, descriptions can contain line breaks (they'll be escaped in iCal format)
        value = sanitizeText(value);
      }
    }
  }

  private static String sanitizeText(String text) {
    // Remove null characters and other problematic control characters
    // but preserve newlines as they are valid in descriptions
    StringBuilder result = new StringBuilder();
    for (char c : text.toCharArray()) {
      if (c == '\n' || c == '\r' || !Character.isISOControl(c)) {
        result.append(c);
      }
    }
    return result.toString();
  }

  /**
   * Creates a new EventDescription from the given string value.
   */
  public static EventDescription of(String value) {
    return new EventDescription(value);
  }

  /**
   * Creates a new EventDescription from a nullable string value.
   * Returns null if the input is null or blank.
   */
  public static EventDescription ofNullable(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return new EventDescription(value);
  }

  /**
   * Returns true if this description is empty or null.
   */
  public boolean isEmpty() {
    return value == null || value.isBlank();
  }

  /**
   * Returns the description formatted for iCalendar TEXT value
   * with proper escaping of special characters.
   */
  public String toICalText() {
    if (value == null) {
      return "";
    }
    // Escape special characters for iCalendar TEXT value
    return value
        .replace("\\", "\\\\")
        .replace(";", "\\;")
        .replace(",", "\\,")
        .replace("\r\n", "\\n")
        .replace("\n", "\\n")
        .replace("\r", "\\n");
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return value != null ? value : "";
  }
}
