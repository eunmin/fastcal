package com.fastcal.domain.model.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("EventSummary")
class EventSummaryTest {

  @Nested
  @DisplayName("생성")
  class Creation {

    @Test
    @DisplayName("유효한 요약으로 생성할 수 있다")
    void shouldCreateWithValidSummary() {
      EventSummary summary = EventSummary.of("Meeting with team");

      assertThat(summary.getValue()).isEqualTo("Meeting with team");
    }

    @Test
    @DisplayName("of 메서드로 생성할 수 있다")
    void shouldCreateWithOfMethod() {
      EventSummary summary = EventSummary.of("Test Summary");

      assertThat(summary).isNotNull();
      assertThat(summary.getValue()).isEqualTo("Test Summary");
    }

    @Test
    @DisplayName("ofNullable은 null 입력시 null을 반환한다")
    void shouldReturnNullForNullInput() {
      EventSummary summary = EventSummary.ofNullable(null);

      assertThat(summary).isNull();
    }

    @Test
    @DisplayName("ofNullable은 빈 문자열 입력시 null을 반환한다")
    void shouldReturnNullForBlankInput() {
      EventSummary summary = EventSummary.ofNullable("   ");

      assertThat(summary).isNull();
    }

    @Test
    @DisplayName("앞뒤 공백이 제거된다")
    void shouldTrimWhitespace() {
      EventSummary summary = EventSummary.of("  Meeting  ");

      assertThat(summary.getValue()).isEqualTo("Meeting");
    }

    @Test
    @DisplayName("null 값으로 생성하면 value는 null이다")
    void shouldAllowNullValue() {
      EventSummary summary = EventSummary.of(null);

      assertThat(summary.getValue()).isNull();
    }

    @Test
    @DisplayName("빈 문자열로 생성하면 value는 null이다")
    void shouldConvertEmptyToNull() {
      EventSummary summary = EventSummary.of("");

      assertThat(summary.getValue()).isNull();
    }
  }

  @Nested
  @DisplayName("유효성 검증")
  class Validation {

    @Test
    @DisplayName("1000자를 초과하면 예외가 발생한다")
    void shouldRejectExceedingMaxLength() {
      String longSummary = "a".repeat(1001);

      assertThatThrownBy(() -> EventSummary.of(longSummary))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("cannot exceed 1000 characters");
    }

    @Test
    @DisplayName("1000자는 허용된다")
    void shouldAcceptMaxLength() {
      String maxLengthSummary = "a".repeat(1000);

      EventSummary summary = EventSummary.of(maxLengthSummary);

      assertThat(summary.getValue()).hasSize(1000);
    }

    @Test
    @DisplayName("줄바꿈은 공백으로 변환된다")
    void shouldConvertNewlinesToSpaces() {
      EventSummary summary = EventSummary.of("Line1\nLine2\r\nLine3");

      assertThat(summary.getValue()).isEqualTo("Line1 Line2 Line3");
    }

    @Test
    @DisplayName("캐리지 리턴은 공백으로 변환된다")
    void shouldConvertCarriageReturnToSpace() {
      EventSummary summary = EventSummary.of("Part1\rPart2");

      assertThat(summary.getValue()).isEqualTo("Part1 Part2");
    }
  }

  @Nested
  @DisplayName("isEmpty")
  class IsEmpty {

    @Test
    @DisplayName("null 값이면 isEmpty는 true를 반환한다")
    void shouldReturnTrueForNullValue() {
      EventSummary summary = EventSummary.of(null);

      assertThat(summary.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("빈 문자열이면 isEmpty는 true를 반환한다")
    void shouldReturnTrueForEmptyValue() {
      EventSummary summary = EventSummary.of("");

      assertThat(summary.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("값이 있으면 isEmpty는 false를 반환한다")
    void shouldReturnFalseForNonEmptyValue() {
      EventSummary summary = EventSummary.of("Meeting");

      assertThat(summary.isEmpty()).isFalse();
    }
  }

  @Nested
  @DisplayName("동등성")
  class Equality {

    @Test
    @DisplayName("같은 값을 가진 EventSummary는 동등하다")
    void shouldBeEqualForSameValue() {
      EventSummary summary1 = EventSummary.of("Meeting");
      EventSummary summary2 = EventSummary.of("Meeting");

      assertThat(summary1).isEqualTo(summary2);
      assertThat(summary1.hashCode()).isEqualTo(summary2.hashCode());
    }

    @Test
    @DisplayName("다른 값을 가진 EventSummary는 동등하지 않다")
    void shouldNotBeEqualForDifferentValue() {
      EventSummary summary1 = EventSummary.of("Meeting 1");
      EventSummary summary2 = EventSummary.of("Meeting 2");

      assertThat(summary1).isNotEqualTo(summary2);
    }

    @Test
    @DisplayName("null 값을 가진 EventSummary끼리 동등하다")
    void shouldBeEqualForNullValues() {
      EventSummary summary1 = EventSummary.of(null);
      EventSummary summary2 = EventSummary.of("");

      assertThat(summary1).isEqualTo(summary2);
    }
  }

  @Nested
  @DisplayName("toString")
  class ToStringTest {

    @Test
    @DisplayName("toString은 값을 반환한다")
    void shouldReturnValue() {
      EventSummary summary = EventSummary.of("Meeting");

      assertThat(summary.toString()).isEqualTo("Meeting");
    }

    @Test
    @DisplayName("null 값이면 toString은 빈 문자열을 반환한다")
    void shouldReturnEmptyStringForNull() {
      EventSummary summary = EventSummary.of(null);

      assertThat(summary.toString()).isEqualTo("");
    }
  }
}
