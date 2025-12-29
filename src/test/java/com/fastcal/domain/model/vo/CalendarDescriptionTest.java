package com.fastcal.domain.model.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CalendarDescription")
class CalendarDescriptionTest {

  @Nested
  @DisplayName("of()")
  class OfTests {

    @Test
    @DisplayName("should create with valid description")
    void shouldCreateWithValidDescription() {
      CalendarDescription desc = CalendarDescription.of("My calendar description");
      assertThat(desc.getValue()).isEqualTo("My calendar description");
    }

    @Test
    @DisplayName("should trim whitespace")
    void shouldTrimWhitespace() {
      CalendarDescription desc = CalendarDescription.of("  My description  ");
      assertThat(desc.getValue()).isEqualTo("My description");
    }

    @Test
    @DisplayName("should handle null as null value")
    void shouldHandleNull() {
      CalendarDescription desc = CalendarDescription.of(null);
      assertThat(desc.getValue()).isNull();
    }

    @Test
    @DisplayName("should convert blank to null")
    void shouldConvertBlankToNull() {
      CalendarDescription desc = CalendarDescription.of("   ");
      assertThat(desc.getValue()).isNull();
    }

    @Test
    @DisplayName("should throw on exceeding max length")
    void shouldThrowOnExceedingMaxLength() {
      String longDesc = "a".repeat(1001);
      assertThatThrownBy(() -> CalendarDescription.of(longDesc))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("1000");
    }

    @Test
    @DisplayName("should accept maximum length")
    void shouldAcceptMaxLength() {
      String maxDesc = "a".repeat(1000);
      CalendarDescription desc = CalendarDescription.of(maxDesc);
      assertThat(desc.getValue()).hasSize(1000);
    }

    @Test
    @DisplayName("should support multiline text")
    void shouldSupportMultiline() {
      String multiline = "Line 1\nLine 2\nLine 3";
      CalendarDescription desc = CalendarDescription.of(multiline);
      assertThat(desc.getValue()).isEqualTo(multiline);
    }

    @Test
    @DisplayName("should support unicode characters")
    void shouldSupportUnicode() {
      CalendarDescription desc = CalendarDescription.of("日本語の説明 한글 설명");
      assertThat(desc.getValue()).isEqualTo("日本語の説明 한글 설명");
    }
  }

  @Nested
  @DisplayName("ofNullable()")
  class OfNullableTests {

    @Test
    @DisplayName("should return null for null input")
    void shouldReturnNullForNull() {
      assertThat(CalendarDescription.ofNullable(null)).isNull();
    }

    @Test
    @DisplayName("should return null for blank input")
    void shouldReturnNullForBlank() {
      assertThat(CalendarDescription.ofNullable("   ")).isNull();
    }

    @Test
    @DisplayName("should create for valid input")
    void shouldCreateForValidInput() {
      CalendarDescription desc = CalendarDescription.ofNullable("My description");
      assertThat(desc).isNotNull();
      assertThat(desc.getValue()).isEqualTo("My description");
    }
  }

  @Nested
  @DisplayName("equality")
  class EqualityTests {

    @Test
    @DisplayName("should be equal for same value")
    void shouldBeEqualForSameValue() {
      CalendarDescription desc1 = CalendarDescription.of("description");
      CalendarDescription desc2 = CalendarDescription.of("description");
      assertThat(desc1).isEqualTo(desc2);
    }
  }
}
