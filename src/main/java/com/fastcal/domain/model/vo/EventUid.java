package com.fastcal.domain.model.vo;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Value object representing an iCalendar UID property.
 *
 * According to RFC 5545, the UID property defines the persistent, globally unique
 * identifier for the calendar component. It is REQUIRED for VEVENT, VTODO, VJOURNAL,
 * and VFREEBUSY components.
 *
 * Constraints based on RFC 5545 and RFC 7986:
 * - Must be less than 255 octets in length
 * - Must be globally unique
 * - Recommended to use hex-encoded UUID format
 * - Must not contain security or privacy-sensitive information
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc5545">RFC 5545</a>
 * @see <a href="https://icalendar.org/New-Properties-for-iCalendar-RFC-7986/5-3-uid-property.html">RFC 7986 UID Property</a>
 */
public record EventUid(String value) {

  private static final int MAX_LENGTH = 255;
  // RFC 5545 UID can be in various formats: UUID, email-style, or other unique identifiers
  // Pattern allows alphanumeric, hyphens, underscores, dots, @, and common UID characters
  private static final Pattern UID_PATTERN =
      Pattern.compile("^[a-zA-Z0-9._@\\-]+$");

  public EventUid {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("EventUid cannot be null or blank");
    }
    value = value.trim();
    if (value.length() > MAX_LENGTH) {
      throw new IllegalArgumentException("EventUid cannot exceed " + MAX_LENGTH + " octets as per RFC 5545");
    }
    if (!UID_PATTERN.matcher(value).matches()) {
      throw new IllegalArgumentException("EventUid contains invalid characters. " +
          "Must contain only alphanumeric characters, dots, hyphens, underscores, and @ symbols");
    }
  }

  /**
   * Creates a new EventUid from the given string value.
   */
  public static EventUid of(String value) {
    return new EventUid(value);
  }

  /**
   * Creates a new EventUid from a nullable string value.
   * Returns null if the input is null or blank.
   */
  public static EventUid ofNullable(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return new EventUid(value);
  }

  /**
   * Generates a new EventUid using a random UUID.
   * This is the recommended approach per RFC 7986.
   */
  public static EventUid generate() {
    return new EventUid(UUID.randomUUID().toString());
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return value;
  }
}
