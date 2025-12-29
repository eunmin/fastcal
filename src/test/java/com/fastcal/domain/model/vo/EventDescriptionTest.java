package com.fastcal.domain.model.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("EventDescription")
class EventDescriptionTest {

  @Nested
  @DisplayName("생성")
  class Creation {

    @Test
    @DisplayName("유효한 설명으로 생성할 수 있다")
    void shouldCreateWithValidDescription() {
      EventDescription description = EventDescription.of("This is a meeting description.");

      assertThat(description.getValue()).isEqualTo("This is a meeting description.");
    }

    @Test
    @DisplayName("of 메서드로 생성할 수 있다")
    void shouldCreateWithOfMethod() {
      EventDescription description = EventDescription.of("Test Description");

      assertThat(description).isNotNull();
      assertThat(description.getValue()).isEqualTo("Test Description");
    }

    @Test
    @DisplayName("ofNullable은 null 입력시 null을 반환한다")
    void shouldReturnNullForNullInput() {
      EventDescription description = EventDescription.ofNullable(null);

      assertThat(description).isNull();
    }

    @Test
    @DisplayName("ofNullable은 빈 문자열 입력시 null을 반환한다")
    void shouldReturnNullForBlankInput() {
      EventDescription description = EventDescription.ofNullable("   ");

      assertThat(description).isNull();
    }

    @Test
    @DisplayName("앞뒤 공백이 제거된다")
    void shouldTrimWhitespace() {
      EventDescription description = EventDescription.of("  Description  ");

      assertThat(description.getValue()).isEqualTo("Description");
    }

    @Test
    @DisplayName("null 값으로 생성하면 value는 null이다")
    void shouldAllowNullValue() {
      EventDescription description = EventDescription.of(null);

      assertThat(description.getValue()).isNull();
    }

    @Test
    @DisplayName("빈 문자열로 생성하면 value는 null이다")
    void shouldConvertEmptyToNull() {
      EventDescription description = EventDescription.of("");

      assertThat(description.getValue()).isNull();
    }
  }

  @Nested
  @DisplayName("유효성 검증")
  class Validation {

    @Test
    @DisplayName("10000자를 초과하면 예외가 발생한다")
    void shouldRejectExceedingMaxLength() {
      String longDescription = "a".repeat(10001);

      assertThatThrownBy(() -> EventDescription.of(longDescription))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("cannot exceed 10000 characters");
    }

    @Test
    @DisplayName("10000자는 허용된다")
    void shouldAcceptMaxLength() {
      String maxLengthDescription = "a".repeat(10000);

      EventDescription description = EventDescription.of(maxLengthDescription);

      assertThat(description.getValue()).hasSize(10000);
    }

    @Test
    @DisplayName("줄바꿈은 유지된다 (Summary와 다름)")
    void shouldPreserveNewlines() {
      EventDescription description = EventDescription.of("Line1\nLine2\r\nLine3");

      assertThat(description.getValue()).contains("\n");
    }
  }

  @Nested
  @DisplayName("isEmpty")
  class IsEmpty {

    @Test
    @DisplayName("null 값이면 isEmpty는 true를 반환한다")
    void shouldReturnTrueForNullValue() {
      EventDescription description = EventDescription.of(null);

      assertThat(description.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("빈 문자열이면 isEmpty는 true를 반환한다")
    void shouldReturnTrueForEmptyValue() {
      EventDescription description = EventDescription.of("");

      assertThat(description.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("값이 있으면 isEmpty는 false를 반환한다")
    void shouldReturnFalseForNonEmptyValue() {
      EventDescription description = EventDescription.of("Some description");

      assertThat(description.isEmpty()).isFalse();
    }
  }

  @Nested
  @DisplayName("toICalText")
  class ToICalText {

    @Test
    @DisplayName("백슬래시를 이스케이프한다")
    void shouldEscapeBackslash() {
      EventDescription description = EventDescription.of("Path: C:\\Users\\test");

      assertThat(description.toICalText()).isEqualTo("Path: C:\\\\Users\\\\test");
    }

    @Test
    @DisplayName("세미콜론을 이스케이프한다")
    void shouldEscapeSemicolon() {
      EventDescription description = EventDescription.of("Item1; Item2");

      assertThat(description.toICalText()).isEqualTo("Item1\\; Item2");
    }

    @Test
    @DisplayName("콤마를 이스케이프한다")
    void shouldEscapeComma() {
      EventDescription description = EventDescription.of("Item1, Item2");

      assertThat(description.toICalText()).isEqualTo("Item1\\, Item2");
    }

    @Test
    @DisplayName("줄바꿈을 \\n으로 이스케이프한다")
    void shouldEscapeNewlines() {
      EventDescription description = EventDescription.of("Line1\nLine2");

      assertThat(description.toICalText()).isEqualTo("Line1\\nLine2");
    }

    @Test
    @DisplayName("CRLF를 \\n으로 이스케이프한다")
    void shouldEscapeCRLF() {
      EventDescription description = EventDescription.of("Line1\r\nLine2");

      assertThat(description.toICalText()).isEqualTo("Line1\\nLine2");
    }

    @Test
    @DisplayName("null 값이면 빈 문자열을 반환한다")
    void shouldReturnEmptyForNull() {
      EventDescription description = EventDescription.of(null);

      assertThat(description.toICalText()).isEqualTo("");
    }
  }

  @Nested
  @DisplayName("동등성")
  class Equality {

    @Test
    @DisplayName("같은 값을 가진 EventDescription은 동등하다")
    void shouldBeEqualForSameValue() {
      EventDescription desc1 = EventDescription.of("Description");
      EventDescription desc2 = EventDescription.of("Description");

      assertThat(desc1).isEqualTo(desc2);
      assertThat(desc1.hashCode()).isEqualTo(desc2.hashCode());
    }

    @Test
    @DisplayName("다른 값을 가진 EventDescription은 동등하지 않다")
    void shouldNotBeEqualForDifferentValue() {
      EventDescription desc1 = EventDescription.of("Description 1");
      EventDescription desc2 = EventDescription.of("Description 2");

      assertThat(desc1).isNotEqualTo(desc2);
    }

    @Test
    @DisplayName("null 값을 가진 EventDescription끼리 동등하다")
    void shouldBeEqualForNullValues() {
      EventDescription desc1 = EventDescription.of(null);
      EventDescription desc2 = EventDescription.of("");

      assertThat(desc1).isEqualTo(desc2);
    }
  }

  @Nested
  @DisplayName("toString")
  class ToStringTest {

    @Test
    @DisplayName("toString은 값을 반환한다")
    void shouldReturnValue() {
      EventDescription description = EventDescription.of("Description");

      assertThat(description.toString()).isEqualTo("Description");
    }

    @Test
    @DisplayName("null 값이면 toString은 빈 문자열을 반환한다")
    void shouldReturnEmptyStringForNull() {
      EventDescription description = EventDescription.of(null);

      assertThat(description.toString()).isEqualTo("");
    }
  }
}
