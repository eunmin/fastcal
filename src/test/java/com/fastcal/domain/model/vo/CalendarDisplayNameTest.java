package com.fastcal.domain.model.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CalendarDisplayName")
class CalendarDisplayNameTest {

  @Nested
  @DisplayName("of()")
  class OfTests {

    @Test
    @DisplayName("should create with valid name")
    void shouldCreateWithValidName() {
      CalendarDisplayName name = CalendarDisplayName.of("My Calendar");
      assertThat(name.getValue()).isEqualTo("My Calendar");
    }

    @Test
    @DisplayName("should trim whitespace")
    void shouldTrimWhitespace() {
      CalendarDisplayName name = CalendarDisplayName.of("  My Calendar  ");
      assertThat(name.getValue()).isEqualTo("My Calendar");
    }

    @Test
    @DisplayName("should handle null as null value")
    void shouldHandleNull() {
      CalendarDisplayName name = CalendarDisplayName.of(null);
      assertThat(name.getValue()).isNull();
    }

    @Test
    @DisplayName("should convert blank to null")
    void shouldConvertBlankToNull() {
      CalendarDisplayName name = CalendarDisplayName.of("   ");
      assertThat(name.getValue()).isNull();
    }

    @Test
    @DisplayName("should throw on exceeding max length")
    void shouldThrowOnExceedingMaxLength() {
      String longName = "a".repeat(256);
      assertThatThrownBy(() -> CalendarDisplayName.of(longName))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("255");
    }

    @Test
    @DisplayName("should accept maximum length")
    void shouldAcceptMaxLength() {
      String maxName = "a".repeat(255);
      CalendarDisplayName name = CalendarDisplayName.of(maxName);
      assertThat(name.getValue()).hasSize(255);
    }

    @Test
    @DisplayName("should support unicode characters")
    void shouldSupportUnicode() {
      CalendarDisplayName name = CalendarDisplayName.of("캘린더 日本語");
      assertThat(name.getValue()).isEqualTo("캘린더 日本語");
    }
  }

  @Nested
  @DisplayName("ofNullable()")
  class OfNullableTests {

    @Test
    @DisplayName("should return null for null input")
    void shouldReturnNullForNull() {
      assertThat(CalendarDisplayName.ofNullable(null)).isNull();
    }

    @Test
    @DisplayName("should return null for blank input")
    void shouldReturnNullForBlank() {
      assertThat(CalendarDisplayName.ofNullable("   ")).isNull();
    }

    @Test
    @DisplayName("should create for valid input")
    void shouldCreateForValidInput() {
      CalendarDisplayName name = CalendarDisplayName.ofNullable("My Calendar");
      assertThat(name).isNotNull();
      assertThat(name.getValue()).isEqualTo("My Calendar");
    }
  }
}
