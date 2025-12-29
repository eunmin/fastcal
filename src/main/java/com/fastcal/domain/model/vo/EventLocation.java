package com.fastcal.domain.model.vo;

/**
 * Value object representing an iCalendar LOCATION property.
 *
 * According to RFC 5545, LOCATION is a TEXT property that defines the intended
 * venue for the activity defined by a calendar component.
 *
 * The property value is typically a simple text description of the location,
 * such as a room name, street address, or geographic coordinates.
 *
 * @see <a href="https://icalendar.org/iCalendar-RFC-5545/3-8-1-7-location.html">RFC 5545 LOCATION</a>
 */
public record EventLocation(String value) {

  // Practical limit for location field
  private static final int MAX_LENGTH = 2000;

  public EventLocation {
    if (value != null) {
      value = value.trim();
      if (value.isEmpty()) {
        value = null;
      } else {
        if (value.length() > MAX_LENGTH) {
          throw new IllegalArgumentException(
              "EventLocation cannot exceed " + MAX_LENGTH + " characters");
        }
        // Sanitize the text value
        value = sanitizeText(value);
      }
    }
  }

  private static String sanitizeText(String text) {
    // Replace actual newlines with spaces as location is typically single-line
    return text
        .replace("\r\n", " ")
        .replace("\r", " ")
        .replace("\n", " ");
  }

  /**
   * Creates a new EventLocation from the given string value.
   */
  public static EventLocation of(String value) {
    return new EventLocation(value);
  }

  /**
   * Creates a new EventLocation from a nullable string value.
   * Returns null if the input is null or blank.
   */
  public static EventLocation ofNullable(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return new EventLocation(value);
  }

  /**
   * Returns true if this location is empty or null.
   */
  public boolean isEmpty() {
    return value == null || value.isBlank();
  }

  /**
   * Returns the location formatted for iCalendar TEXT value
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
        .replace(",", "\\,");
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return value != null ? value : "";
  }
}
