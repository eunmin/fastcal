package com.fastcal.domain.model.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CalendarId")
class CalendarIdTest {

  @Nested
  @DisplayName("of()")
  class OfTests {

    @Test
    @DisplayName("should create CalendarId with valid alphanumeric value")
    void shouldCreateWithValidValue() {
      CalendarId calendarId = CalendarId.of("my-calendar_123");
      assertThat(calendarId.getValue()).isEqualTo("my-calendar_123");
    }

    @Test
    @DisplayName("should throw on value with whitespace")
    void shouldThrowOnWhitespace() {
      assertThatThrownBy(() -> CalendarId.of("  calendar123  "))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("should throw on null value")
    void shouldThrowOnNull() {
      assertThatThrownBy(() -> CalendarId.of(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("cannot be null or blank");
    }

    @Test
    @DisplayName("should throw on blank value")
    void shouldThrowOnBlank() {
      assertThatThrownBy(() -> CalendarId.of("   "))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("cannot be null or blank");
    }

    @Test
    @DisplayName("should throw on invalid characters")
    void shouldThrowOnInvalidCharacters() {
      assertThatThrownBy(() -> CalendarId.of("calendar@123"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("alphanumeric");
    }

    @Test
    @DisplayName("should throw on exceeding max length")
    void shouldThrowOnExceedingMaxLength() {
      String longId = "a".repeat(101);
      assertThatThrownBy(() -> CalendarId.of(longId))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("100");
    }

    @Test
    @DisplayName("should accept maximum length")
    void shouldAcceptMaxLength() {
      String maxId = "a".repeat(100);
      CalendarId calendarId = CalendarId.of(maxId);
      assertThat(calendarId.getValue()).hasSize(100);
    }
  }

  @Nested
  @DisplayName("equality")
  class EqualityTests {

    @Test
    @DisplayName("should be equal for same value")
    void shouldBeEqualForSameValue() {
      CalendarId id1 = CalendarId.of("calendar123");
      CalendarId id2 = CalendarId.of("calendar123");
      assertThat(id1).isEqualTo(id2);
    }
  }
}
