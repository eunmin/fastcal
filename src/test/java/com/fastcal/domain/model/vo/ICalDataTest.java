package com.fastcal.domain.model.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ICalData")
class ICalDataTest {

  private static final String VALID_ICAL_DATA = """
      BEGIN:VCALENDAR
      VERSION:2.0
      PRODID:-//Test//Test//EN
      BEGIN:VEVENT
      UID:test-event-123
      DTSTART:20241225T100000Z
      DTEND:20241225T110000Z
      SUMMARY:Test Event
      END:VEVENT
      END:VCALENDAR
      """;

  private static final String VALID_VTODO_DATA = """
      BEGIN:VCALENDAR
      VERSION:2.0
      PRODID:-//Test//Test//EN
      BEGIN:VTODO
      UID:test-todo-123
      SUMMARY:Test Todo
      END:VTODO
      END:VCALENDAR
      """;

  @Nested
  @DisplayName("생성")
  class Creation {

    @Test
    @DisplayName("유효한 iCalendar 데이터로 생성할 수 있다")
    void shouldCreateWithValidICalData() {
      ICalData icalData = ICalData.of(VALID_ICAL_DATA);

      assertThat(icalData.getValue()).contains("BEGIN:VCALENDAR");
      assertThat(icalData.getValue()).contains("END:VCALENDAR");
    }

    @Test
    @DisplayName("VTODO 컴포넌트를 포함한 데이터로 생성할 수 있다")
    void shouldCreateWithVTodoComponent() {
      ICalData icalData = ICalData.of(VALID_VTODO_DATA);

      assertThat(icalData.hasVTodo()).isTrue();
      assertThat(icalData.hasVEvent()).isFalse();
    }

    @Test
    @DisplayName("of 메서드로 생성할 수 있다")
    void shouldCreateWithOfMethod() {
      ICalData icalData = ICalData.of(VALID_ICAL_DATA);

      assertThat(icalData).isNotNull();
    }

    @Test
    @DisplayName("ofNullable은 null 입력시 null을 반환한다")
    void shouldReturnNullForNullInput() {
      ICalData icalData = ICalData.ofNullable(null);

      assertThat(icalData).isNull();
    }

    @Test
    @DisplayName("ofNullable은 빈 문자열 입력시 null을 반환한다")
    void shouldReturnNullForBlankInput() {
      ICalData icalData = ICalData.ofNullable("   ");

      assertThat(icalData).isNull();
    }

    @Test
    @DisplayName("앞뒤 공백이 제거된다")
    void shouldTrimWhitespace() {
      ICalData icalData = ICalData.of("  " + VALID_ICAL_DATA + "  ");

      assertThat(icalData.getValue()).doesNotStartWith(" ");
      assertThat(icalData.getValue()).doesNotEndWith(" ");
    }
  }

  @Nested
  @DisplayName("유효성 검증")
  class Validation {

    @Test
    @DisplayName("null 값은 허용되지 않는다")
    void shouldRejectNullValue() {
      assertThatThrownBy(() -> ICalData.of(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("cannot be null or blank");
    }

    @Test
    @DisplayName("빈 문자열은 허용되지 않는다")
    void shouldRejectEmptyValue() {
      assertThatThrownBy(() -> ICalData.of(""))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("cannot be null or blank");
    }

    @Test
    @DisplayName("BEGIN:VCALENDAR가 없으면 예외가 발생한다")
    void shouldRejectMissingVCalendarBegin() {
      String invalidData = """
          VERSION:2.0
          BEGIN:VEVENT
          UID:test
          END:VEVENT
          END:VCALENDAR
          """;

      assertThatThrownBy(() -> ICalData.of(invalidData))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("must contain BEGIN:VCALENDAR");
    }

    @Test
    @DisplayName("END:VCALENDAR가 없으면 예외가 발생한다")
    void shouldRejectMissingVCalendarEnd() {
      String invalidData = """
          BEGIN:VCALENDAR
          VERSION:2.0
          BEGIN:VEVENT
          UID:test
          END:VEVENT
          """;

      assertThatThrownBy(() -> ICalData.of(invalidData))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("must contain END:VCALENDAR");
    }

    @Test
    @DisplayName("METHOD 속성이 있으면 예외가 발생한다 (RFC 4791)")
    void shouldRejectMethodProperty() {
      String dataWithMethod = """
          BEGIN:VCALENDAR
          VERSION:2.0
          METHOD:REQUEST
          BEGIN:VEVENT
          UID:test
          END:VEVENT
          END:VCALENDAR
          """;

      assertThatThrownBy(() -> ICalData.of(dataWithMethod))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("must not contain METHOD property");
    }

    @Test
    @DisplayName("유효한 컴포넌트가 없으면 예외가 발생한다")
    void shouldRejectMissingValidComponent() {
      String noComponent = """
          BEGIN:VCALENDAR
          VERSION:2.0
          END:VCALENDAR
          """;

      assertThatThrownBy(() -> ICalData.of(noComponent))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("must contain at least one valid component");
    }

    @Test
    @DisplayName("1MB를 초과하면 예외가 발생한다")
    void shouldRejectExceedingMaxSize() {
      String header = """
          BEGIN:VCALENDAR
          VERSION:2.0
          BEGIN:VEVENT
          UID:test
          DESCRIPTION:""";
      String footer = """

          END:VEVENT
          END:VCALENDAR
          """;
      String largeContent = "a".repeat(1024 * 1024 + 1);

      assertThatThrownBy(() -> ICalData.of(header + largeContent + footer))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("cannot exceed 1MB");
    }
  }

  @Nested
  @DisplayName("컴포넌트 확인")
  class ComponentCheck {

    @Test
    @DisplayName("VEVENT 컴포넌트가 있는지 확인할 수 있다")
    void shouldCheckHasVEvent() {
      ICalData icalData = ICalData.of(VALID_ICAL_DATA);

      assertThat(icalData.hasVEvent()).isTrue();
    }

    @Test
    @DisplayName("VTODO 컴포넌트가 있는지 확인할 수 있다")
    void shouldCheckHasVTodo() {
      ICalData icalData = ICalData.of(VALID_VTODO_DATA);

      assertThat(icalData.hasVTodo()).isTrue();
    }

    @Test
    @DisplayName("VEVENT가 없으면 hasVEvent는 false를 반환한다")
    void shouldReturnFalseWhenNoVEvent() {
      ICalData icalData = ICalData.of(VALID_VTODO_DATA);

      assertThat(icalData.hasVEvent()).isFalse();
    }
  }

  @Nested
  @DisplayName("동등성")
  class Equality {

    @Test
    @DisplayName("같은 데이터를 가진 ICalData는 동등하다")
    void shouldBeEqualForSameData() {
      ICalData data1 = ICalData.of(VALID_ICAL_DATA);
      ICalData data2 = ICalData.of(VALID_ICAL_DATA);

      assertThat(data1).isEqualTo(data2);
      assertThat(data1.hashCode()).isEqualTo(data2.hashCode());
    }

    @Test
    @DisplayName("다른 데이터를 가진 ICalData는 동등하지 않다")
    void shouldNotBeEqualForDifferentData() {
      ICalData data1 = ICalData.of(VALID_ICAL_DATA);
      ICalData data2 = ICalData.of(VALID_VTODO_DATA);

      assertThat(data1).isNotEqualTo(data2);
    }
  }

  @Nested
  @DisplayName("toString")
  class ToStringTest {

    @Test
    @DisplayName("toString은 값을 반환한다")
    void shouldReturnValue() {
      ICalData icalData = ICalData.of(VALID_ICAL_DATA);

      assertThat(icalData.toString()).contains("BEGIN:VCALENDAR");
    }
  }
}
