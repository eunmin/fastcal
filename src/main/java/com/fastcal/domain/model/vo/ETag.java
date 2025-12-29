package com.fastcal.domain.model.vo;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Value object representing a CalDAV ETag (Entity Tag).
 *
 * According to RFC 4791 (CalDAV), the DAV:getetag property MUST be defined and
 * set to a strong entity tag on all calendar object resources.
 *
 * ETags are used for:
 * - Concurrency control (optimistic locking)
 * - Cache validation
 * - Synchronization
 *
 * Constraints based on RFC 4791 and HTTP ETag specification:
 * - Must be a strong entity tag (not weak)
 * - Should change when the resource content changes
 * - Typically enclosed in double quotes in HTTP headers
 *
 * @see <a href="https://datatracker.ietf.org/doc/rfc4791/">RFC 4791 - CalDAV</a>
 */
public record ETag(String value) {

  private static final int MAX_LENGTH = 128;

  public ETag {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("ETag cannot be null or blank");
    }
    value = value.trim();
    // Remove surrounding quotes if present (HTTP format)
    if (value.startsWith("\"") && value.endsWith("\"")) {
      value = value.substring(1, value.length() - 1);
    }
    if (value.isEmpty()) {
      throw new IllegalArgumentException("ETag cannot be empty after trimming");
    }
    if (value.length() > MAX_LENGTH) {
      throw new IllegalArgumentException("ETag cannot exceed " + MAX_LENGTH + " characters");
    }
    // ETag should not contain double quotes in the value itself
    if (value.contains("\"")) {
      throw new IllegalArgumentException("ETag value cannot contain double quotes");
    }
  }

  /**
   * Creates a new ETag from the given string value.
   */
  public static ETag of(String value) {
    return new ETag(value);
  }

  /**
   * Creates a new ETag from a nullable string value.
   * Returns null if the input is null or blank.
   */
  public static ETag ofNullable(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return new ETag(value);
  }

  /**
   * Generates an ETag from the given content using MD5 hash.
   * This ensures the ETag changes when content changes.
   */
  public static ETag fromContent(String content) {
    if (content == null) {
      throw new IllegalArgumentException("Content cannot be null for ETag generation");
    }
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] digest = md.digest(content.getBytes());
      String hash = HexFormat.of().formatHex(digest);
      return new ETag(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("MD5 algorithm not available", e);
    }
  }

  /**
   * Returns the ETag value in HTTP header format (enclosed in double quotes).
   */
  public String toHttpFormat() {
    return "\"" + value + "\"";
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return value;
  }
}
