package com.fastcal.domain.model.vo;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Value object representing an iCalendar RRULE (Recurrence Rule) property.
 *
 * According to RFC 5545, the RECUR value type is used to identify properties
 * that contain a recurrence rule specification.
 *
 * Key constraints:
 * - The FREQ rule part is REQUIRED and must not occur more than once
 * - UNTIL and COUNT are optional but must not occur together
 * - Valid FREQ values: SECONDLY, MINUTELY, HOURLY, DAILY, WEEKLY, MONTHLY, YEARLY
 * - BYDAY values: SU, MO, TU, WE, TH, FR, SA (optionally with +/- prefix for nth occurrence)
 * - BYSECOND: 0-60, BYMINUTE: 0-59, BYHOUR: 0-23
 *
 * @see <a href="https://icalendar.org/iCalendar-RFC-5545/3-3-10-recurrence-rule.html">RFC 5545 RRULE</a>
 */
public record RecurrenceRule(String value) {

  private static final int MAX_LENGTH = 1000;

  private static final Set<String> VALID_FREQUENCIES = Set.of(
      "SECONDLY", "MINUTELY", "HOURLY", "DAILY", "WEEKLY", "MONTHLY", "YEARLY"
  );

  private static final Set<String> VALID_WEEKDAYS = Set.of(
      "SU", "MO", "TU", "WE", "TH", "FR", "SA"
  );

  // Pattern to match FREQ=value in the rule
  private static final Pattern FREQ_PATTERN = Pattern.compile("FREQ=([A-Z]+)");

  // Pattern to validate the overall RRULE structure (basic validation)
  private static final Pattern RRULE_PART_PATTERN = Pattern.compile(
      "^[A-Z]+=[-+]?[A-Za-z0-9,+-]+$"
  );

  public RecurrenceRule {
    if (value != null) {
      value = value.trim();
      if (value.isEmpty()) {
        value = null;
      } else {
        if (value.length() > MAX_LENGTH) {
          throw new IllegalArgumentException(
              "RecurrenceRule cannot exceed " + MAX_LENGTH + " characters");
        }

        // Remove RRULE: prefix if present
        if (value.toUpperCase().startsWith("RRULE:")) {
          value = value.substring(6);
        }

        validateRrule(value);
      }
    }
  }

  private static void validateRrule(String rrule) {
    String[] parts = rrule.split(";");

    boolean hasFreq = false;
    boolean hasUntil = false;
    boolean hasCount = false;

    for (String part : parts) {
      part = part.trim();
      if (part.isEmpty()) {
        continue;
      }

      // Basic structure validation
      if (!RRULE_PART_PATTERN.matcher(part).matches()) {
        throw new IllegalArgumentException("Invalid RRULE part format: " + part);
      }

      String[] keyValue = part.split("=", 2);
      if (keyValue.length != 2) {
        throw new IllegalArgumentException("Invalid RRULE part: " + part);
      }

      String key = keyValue[0].toUpperCase();
      String val = keyValue[1];

      switch (key) {
        case "FREQ":
          if (hasFreq) {
            throw new IllegalArgumentException("FREQ must not occur more than once in RRULE");
          }
          if (!VALID_FREQUENCIES.contains(val.toUpperCase())) {
            throw new IllegalArgumentException(
                "Invalid FREQ value: " + val + ". Must be one of: " + VALID_FREQUENCIES);
          }
          hasFreq = true;
          break;

        case "UNTIL":
          hasUntil = true;
          break;

        case "COUNT":
          hasCount = true;
          try {
            int count = Integer.parseInt(val);
            if (count < 1) {
              throw new IllegalArgumentException("COUNT must be a positive integer");
            }
          } catch (NumberFormatException e) {
            throw new IllegalArgumentException("COUNT must be a valid integer: " + val);
          }
          break;

        case "INTERVAL":
          try {
            int interval = Integer.parseInt(val);
            if (interval < 1) {
              throw new IllegalArgumentException("INTERVAL must be a positive integer");
            }
          } catch (NumberFormatException e) {
            throw new IllegalArgumentException("INTERVAL must be a valid integer: " + val);
          }
          break;

        case "BYDAY":
          validateByDay(val);
          break;

        case "BYSECOND":
          validateNumericList(val, 0, 60, "BYSECOND");
          break;

        case "BYMINUTE":
          validateNumericList(val, 0, 59, "BYMINUTE");
          break;

        case "BYHOUR":
          validateNumericList(val, 0, 23, "BYHOUR");
          break;

        case "BYMONTHDAY":
          validateNumericList(val, -31, 31, "BYMONTHDAY", true);
          break;

        case "BYYEARDAY":
          validateNumericList(val, -366, 366, "BYYEARDAY", true);
          break;

        case "BYWEEKNO":
          validateNumericList(val, -53, 53, "BYWEEKNO", true);
          break;

        case "BYMONTH":
          validateNumericList(val, 1, 12, "BYMONTH");
          break;

        case "BYSETPOS":
          validateNumericList(val, -366, 366, "BYSETPOS", true);
          break;

        case "WKST":
          if (!VALID_WEEKDAYS.contains(val.toUpperCase())) {
            throw new IllegalArgumentException(
                "Invalid WKST value: " + val + ". Must be one of: " + VALID_WEEKDAYS);
          }
          break;

        default:
          // Unknown rule parts are allowed for forward compatibility
          break;
      }
    }

    if (!hasFreq) {
      throw new IllegalArgumentException("RRULE must contain FREQ rule part");
    }

    if (hasUntil && hasCount) {
      throw new IllegalArgumentException("RRULE cannot contain both UNTIL and COUNT");
    }
  }

  private static void validateByDay(String value) {
    String[] days = value.split(",");
    Pattern dayPattern = Pattern.compile("^([+-]?[0-9]*)?(SU|MO|TU|WE|TH|FR|SA)$");

    for (String day : days) {
      if (!dayPattern.matcher(day.toUpperCase().trim()).matches()) {
        throw new IllegalArgumentException("Invalid BYDAY value: " + day);
      }
    }
  }

  private static void validateNumericList(String value, int min, int max, String partName) {
    validateNumericList(value, min, max, partName, false);
  }

  private static void validateNumericList(String value, int min, int max, String partName,
      boolean excludeZero) {
    String[] values = value.split(",");
    for (String v : values) {
      try {
        int num = Integer.parseInt(v.trim());
        if (num < min || num > max || (excludeZero && num == 0)) {
          throw new IllegalArgumentException(
              partName + " value " + num + " is out of valid range [" + min + ", " + max + "]");
        }
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException(
            partName + " must contain valid integers: " + v);
      }
    }
  }

  /**
   * Creates a new RecurrenceRule from the given string value.
   */
  public static RecurrenceRule of(String value) {
    return new RecurrenceRule(value);
  }

  /**
   * Creates a new RecurrenceRule from a nullable string value.
   * Returns null if the input is null or blank.
   */
  public static RecurrenceRule ofNullable(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return new RecurrenceRule(value);
  }

  /**
   * Returns true if this rule is empty or null.
   */
  public boolean isEmpty() {
    return value == null || value.isBlank();
  }

  /**
   * Extracts the frequency (FREQ) from this recurrence rule.
   */
  public String getFrequency() {
    if (value == null) {
      return null;
    }
    var matcher = FREQ_PATTERN.matcher(value);
    if (matcher.find()) {
      return matcher.group(1);
    }
    return null;
  }

  /**
   * Returns the value formatted for iCalendar RRULE property.
   */
  public String toICalFormat() {
    return value != null ? "RRULE:" + value : "";
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return value != null ? value : "";
  }
}
