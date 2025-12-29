package com.fastcal.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class InputValidatorTest {

  @Nested
  @DisplayName("isValidUserId()")
  class IsValidUserIdTests {

    @ParameterizedTest
    @ValueSource(strings = {
        "user@example.com",
        "test.user@example.com",
        "user+tag@example.com",
        "user123@example.co.kr"
    })
    @DisplayName("should return true for valid user IDs (email format)")
    void shouldReturnTrueForValidUserIds(String userId) {
      assertThat(InputValidator.isValidUserId(userId)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
        "invalid",
        "user@",
        "@example.com",
        "user@.com",
        "user@example",
        "user name@example.com"
    })
    @DisplayName("should return false for invalid user IDs")
    void shouldReturnFalseForInvalidUserIds(String userId) {
      assertThat(InputValidator.isValidUserId(userId)).isFalse();
    }
  }

  @Nested
  @DisplayName("isValidCalendarId()")
  class IsValidCalendarIdTests {

    @ParameterizedTest
    @ValueSource(strings = {
        "calendar1",
        "my-calendar",
        "my_calendar",
        "Calendar123",
        "a",
        "calendar-with-dashes_and_underscores"
    })
    @DisplayName("should return true for valid calendar IDs")
    void shouldReturnTrueForValidCalendarIds(String calendarId) {
      assertThat(InputValidator.isValidCalendarId(calendarId)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
        "calendar with spaces",
        "calendar@special",
        "calendar/path"
    })
    @DisplayName("should return false for invalid calendar IDs")
    void shouldReturnFalseForInvalidCalendarIds(String calendarId) {
      assertThat(InputValidator.isValidCalendarId(calendarId)).isFalse();
    }

    @Test
    @DisplayName("should return false for calendar ID exceeding 100 characters")
    void shouldReturnFalseForTooLongCalendarId() {
      String longId = "a".repeat(101);
      assertThat(InputValidator.isValidCalendarId(longId)).isFalse();
    }

    @Test
    @DisplayName("should return true for calendar ID at max length")
    void shouldReturnTrueForMaxLengthCalendarId() {
      String maxLengthId = "a".repeat(100);
      assertThat(InputValidator.isValidCalendarId(maxLengthId)).isTrue();
    }
  }

  @Nested
  @DisplayName("isValidEventUid()")
  class IsValidEventUidTests {

    @ParameterizedTest
    @ValueSource(strings = {
        "event123",
        "event-123",
        "event_123",
        "event.123",
        "event@123",
        "550e8400-e29b-41d4-a716-446655440000"
    })
    @DisplayName("should return true for valid event UIDs")
    void shouldReturnTrueForValidEventUids(String eventUid) {
      assertThat(InputValidator.isValidEventUid(eventUid)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
        "event with spaces",
        "event/uid",
        "event:uid"
    })
    @DisplayName("should return false for invalid event UIDs")
    void shouldReturnFalseForInvalidEventUids(String eventUid) {
      assertThat(InputValidator.isValidEventUid(eventUid)).isFalse();
    }

    @Test
    @DisplayName("should return false for event UID exceeding 255 characters")
    void shouldReturnFalseForTooLongEventUid() {
      String longUid = "a".repeat(256);
      assertThat(InputValidator.isValidEventUid(longUid)).isFalse();
    }

    @Test
    @DisplayName("should return true for event UID at max length")
    void shouldReturnTrueForMaxLengthEventUid() {
      String maxLengthUid = "a".repeat(255);
      assertThat(InputValidator.isValidEventUid(maxLengthUid)).isTrue();
    }
  }

  @Nested
  @DisplayName("isValidDepth()")
  class IsValidDepthTests {

    @ParameterizedTest
    @ValueSource(strings = {"0", "1", "infinity"})
    @DisplayName("should return true for valid depth values")
    void shouldReturnTrueForValidDepth(String depth) {
      assertThat(InputValidator.isValidDepth(depth)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("should return true for null or empty depth")
    void shouldReturnTrueForNullOrEmptyDepth(String depth) {
      assertThat(InputValidator.isValidDepth(depth)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"2", "3", "Infinity", "INFINITY", "-1", "abc"})
    @DisplayName("should return false for invalid depth values")
    void shouldReturnFalseForInvalidDepth(String depth) {
      assertThat(InputValidator.isValidDepth(depth)).isFalse();
    }
  }

  @Nested
  @DisplayName("sanitizeDepth()")
  class SanitizeDepthTests {

    @Test
    @DisplayName("should return original value for valid depth")
    void shouldReturnOriginalValueForValidDepth() {
      assertThat(InputValidator.sanitizeDepth("0")).isEqualTo("0");
      assertThat(InputValidator.sanitizeDepth("1")).isEqualTo("1");
      assertThat(InputValidator.sanitizeDepth("infinity")).isEqualTo("infinity");
    }

    @Test
    @DisplayName("should return '0' for null depth")
    void shouldReturnDefaultForNullDepth() {
      assertThat(InputValidator.sanitizeDepth(null)).isEqualTo("0");
    }

    @Test
    @DisplayName("should return '0' for invalid depth")
    void shouldReturnDefaultForInvalidDepth() {
      assertThat(InputValidator.sanitizeDepth("invalid")).isEqualTo("0");
      assertThat(InputValidator.sanitizeDepth("2")).isEqualTo("0");
    }
  }
}
