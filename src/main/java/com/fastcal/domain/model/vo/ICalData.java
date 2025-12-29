package com.fastcal.domain.model.vo;

/**
 * Value object representing raw iCalendar data.
 *
 * According to RFC 5545, iCalendar data must follow specific formatting rules:
 * - Content lines should be limited to 75 octets (with line folding for longer lines)
 * - Must begin with BEGIN:VCALENDAR and end with END:VCALENDAR
 * - Must contain valid component types (VEVENT, VTODO, VJOURNAL, etc.)
 * - Must NOT contain METHOD property for CalDAV stored objects (RFC 4791)
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc5545">RFC 5545</a>
 * @see <a href="https://datatracker.ietf.org/doc/rfc4791/">RFC 4791 - CalDAV</a>
 */
public record ICalData(String value) {

  private static final String VCALENDAR_BEGIN = "BEGIN:VCALENDAR";
  private static final String VCALENDAR_END = "END:VCALENDAR";
  private static final String METHOD_PROPERTY = "METHOD:";
  private static final int MAX_SIZE = 1024 * 1024; // 1MB limit for practical purposes

  public ICalData {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("ICalData cannot be null or blank");
    }
    value = value.trim();

    if (value.length() > MAX_SIZE) {
      throw new IllegalArgumentException("ICalData cannot exceed 1MB");
    }

    // Normalize line endings to CRLF as per RFC 5545
    String normalizedValue = value.replace("\r\n", "\n").replace("\r", "\n");

    // Validate basic iCalendar structure
    if (!normalizedValue.contains(VCALENDAR_BEGIN)) {
      throw new IllegalArgumentException("ICalData must contain BEGIN:VCALENDAR");
    }
    if (!normalizedValue.contains(VCALENDAR_END)) {
      throw new IllegalArgumentException("ICalData must contain END:VCALENDAR");
    }

    // RFC 4791: Calendar object resources MUST NOT specify the iCalendar METHOD property
    if (containsMethodProperty(normalizedValue)) {
      throw new IllegalArgumentException(
          "ICalData must not contain METHOD property for CalDAV storage (RFC 4791)");
    }

    // Validate contains at least one valid component
    if (!containsValidComponent(normalizedValue)) {
      throw new IllegalArgumentException(
          "ICalData must contain at least one valid component (VEVENT, VTODO, VJOURNAL, or VFREEBUSY)");
    }
  }

  private static boolean containsMethodProperty(String data) {
    String[] lines = data.split("\n");
    for (String line : lines) {
      String trimmedLine = line.trim();
      if (trimmedLine.startsWith(METHOD_PROPERTY)) {
        return true;
      }
    }
    return false;
  }

  private static boolean containsValidComponent(String data) {
    return data.contains("BEGIN:VEVENT") ||
        data.contains("BEGIN:VTODO") ||
        data.contains("BEGIN:VJOURNAL") ||
        data.contains("BEGIN:VFREEBUSY");
  }

  /**
   * Creates a new ICalData from the given string value.
   */
  public static ICalData of(String value) {
    return new ICalData(value);
  }

  /**
   * Creates a new ICalData from a nullable string value.
   * Returns null if the input is null or blank.
   */
  public static ICalData ofNullable(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return new ICalData(value);
  }

  /**
   * Checks if this iCalendar data contains a VEVENT component.
   */
  public boolean hasVEvent() {
    return value.contains("BEGIN:VEVENT");
  }

  /**
   * Checks if this iCalendar data contains a VTODO component.
   */
  public boolean hasVTodo() {
    return value.contains("BEGIN:VTODO");
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return value;
  }
}
