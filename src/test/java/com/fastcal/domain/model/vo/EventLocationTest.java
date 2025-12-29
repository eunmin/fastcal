package com.fastcal.domain.model.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("EventLocation")
class EventLocationTest {

  @Nested
  @DisplayName("생성")
  class Creation {

    @Test
    @DisplayName("유효한 위치로 생성할 수 있다")
    void shouldCreateWithValidLocation() {
      EventLocation location = EventLocation.of("Conference Room A");

      assertThat(location.getValue()).isEqualTo("Conference Room A");
    }

    @Test
    @DisplayName("주소 형식의 위치로 생성할 수 있다")
    void shouldCreateWithAddressFormat() {
      EventLocation location = EventLocation.of("123 Main St, City, Country");

      assertThat(location.getValue()).isEqualTo("123 Main St, City, Country");
    }

    @Test
    @DisplayName("of 메서드로 생성할 수 있다")
    void shouldCreateWithOfMethod() {
      EventLocation location = EventLocation.of("Test Location");

      assertThat(location).isNotNull();
      assertThat(location.getValue()).isEqualTo("Test Location");
    }

    @Test
    @DisplayName("ofNullable은 null 입력시 null을 반환한다")
    void shouldReturnNullForNullInput() {
      EventLocation location = EventLocation.ofNullable(null);

      assertThat(location).isNull();
    }

    @Test
    @DisplayName("ofNullable은 빈 문자열 입력시 null을 반환한다")
    void shouldReturnNullForBlankInput() {
      EventLocation location = EventLocation.ofNullable("   ");

      assertThat(location).isNull();
    }

    @Test
    @DisplayName("앞뒤 공백이 제거된다")
    void shouldTrimWhitespace() {
      EventLocation location = EventLocation.of("  Room 101  ");

      assertThat(location.getValue()).isEqualTo("Room 101");
    }

    @Test
    @DisplayName("null 값으로 생성하면 value는 null이다")
    void shouldAllowNullValue() {
      EventLocation location = EventLocation.of(null);

      assertThat(location.getValue()).isNull();
    }

    @Test
    @DisplayName("빈 문자열로 생성하면 value는 null이다")
    void shouldConvertEmptyToNull() {
      EventLocation location = EventLocation.of("");

      assertThat(location.getValue()).isNull();
    }
  }

  @Nested
  @DisplayName("유효성 검증")
  class Validation {

    @Test
    @DisplayName("2000자를 초과하면 예외가 발생한다")
    void shouldRejectExceedingMaxLength() {
      String longLocation = "a".repeat(2001);

      assertThatThrownBy(() -> EventLocation.of(longLocation))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("cannot exceed 2000 characters");
    }

    @Test
    @DisplayName("2000자는 허용된다")
    void shouldAcceptMaxLength() {
      String maxLengthLocation = "a".repeat(2000);

      EventLocation location = EventLocation.of(maxLengthLocation);

      assertThat(location.getValue()).hasSize(2000);
    }

    @Test
    @DisplayName("줄바꿈은 공백으로 변환된다")
    void shouldConvertNewlinesToSpaces() {
      EventLocation location = EventLocation.of("Building A\nFloor 3");

      assertThat(location.getValue()).isEqualTo("Building A Floor 3");
    }

    @Test
    @DisplayName("CRLF는 공백으로 변환된다")
    void shouldConvertCRLFToSpace() {
      EventLocation location = EventLocation.of("Building A\r\nFloor 3");

      assertThat(location.getValue()).isEqualTo("Building A Floor 3");
    }

    @Test
    @DisplayName("캐리지 리턴은 공백으로 변환된다")
    void shouldConvertCarriageReturnToSpace() {
      EventLocation location = EventLocation.of("Part1\rPart2");

      assertThat(location.getValue()).isEqualTo("Part1 Part2");
    }
  }

  @Nested
  @DisplayName("isEmpty")
  class IsEmpty {

    @Test
    @DisplayName("null 값이면 isEmpty는 true를 반환한다")
    void shouldReturnTrueForNullValue() {
      EventLocation location = EventLocation.of(null);

      assertThat(location.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("빈 문자열이면 isEmpty는 true를 반환한다")
    void shouldReturnTrueForEmptyValue() {
      EventLocation location = EventLocation.of("");

      assertThat(location.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("값이 있으면 isEmpty는 false를 반환한다")
    void shouldReturnFalseForNonEmptyValue() {
      EventLocation location = EventLocation.of("Room 101");

      assertThat(location.isEmpty()).isFalse();
    }
  }

  @Nested
  @DisplayName("toICalText")
  class ToICalText {

    @Test
    @DisplayName("백슬래시를 이스케이프한다")
    void shouldEscapeBackslash() {
      EventLocation location = EventLocation.of("Path: C:\\Users\\test");

      assertThat(location.toICalText()).isEqualTo("Path: C:\\\\Users\\\\test");
    }

    @Test
    @DisplayName("세미콜론을 이스케이프한다")
    void shouldEscapeSemicolon() {
      EventLocation location = EventLocation.of("Room A; Building 1");

      assertThat(location.toICalText()).isEqualTo("Room A\\; Building 1");
    }

    @Test
    @DisplayName("콤마를 이스케이프한다")
    void shouldEscapeComma() {
      EventLocation location = EventLocation.of("123 Main St, City");

      assertThat(location.toICalText()).isEqualTo("123 Main St\\, City");
    }

    @Test
    @DisplayName("null 값이면 빈 문자열을 반환한다")
    void shouldReturnEmptyForNull() {
      EventLocation location = EventLocation.of(null);

      assertThat(location.toICalText()).isEqualTo("");
    }
  }

  @Nested
  @DisplayName("동등성")
  class Equality {

    @Test
    @DisplayName("같은 값을 가진 EventLocation은 동등하다")
    void shouldBeEqualForSameValue() {
      EventLocation loc1 = EventLocation.of("Room 101");
      EventLocation loc2 = EventLocation.of("Room 101");

      assertThat(loc1).isEqualTo(loc2);
      assertThat(loc1.hashCode()).isEqualTo(loc2.hashCode());
    }

    @Test
    @DisplayName("다른 값을 가진 EventLocation은 동등하지 않다")
    void shouldNotBeEqualForDifferentValue() {
      EventLocation loc1 = EventLocation.of("Room 101");
      EventLocation loc2 = EventLocation.of("Room 102");

      assertThat(loc1).isNotEqualTo(loc2);
    }

    @Test
    @DisplayName("null 값을 가진 EventLocation끼리 동등하다")
    void shouldBeEqualForNullValues() {
      EventLocation loc1 = EventLocation.of(null);
      EventLocation loc2 = EventLocation.of("");

      assertThat(loc1).isEqualTo(loc2);
    }
  }

  @Nested
  @DisplayName("toString")
  class ToStringTest {

    @Test
    @DisplayName("toString은 값을 반환한다")
    void shouldReturnValue() {
      EventLocation location = EventLocation.of("Room 101");

      assertThat(location.toString()).isEqualTo("Room 101");
    }

    @Test
    @DisplayName("null 값이면 toString은 빈 문자열을 반환한다")
    void shouldReturnEmptyStringForNull() {
      EventLocation location = EventLocation.of(null);

      assertThat(location.toString()).isEqualTo("");
    }
  }
}
