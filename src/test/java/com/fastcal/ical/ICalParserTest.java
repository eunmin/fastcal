package com.fastcal.ical;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ICalParserTest {

  private ICalParser parser;

  @BeforeEach
  void setUp() {
    parser = new ICalParser();
  }

  @Nested
  @DisplayName("parse()")
  class ParseTests {

    @Test
    @DisplayName("should parse basic event with all properties")
    void shouldParseBasicEvent() {
      String icalData = """
          BEGIN:VCALENDAR
          VERSION:2.0
          PRODID:-//Test//Test//EN
          BEGIN:VEVENT
          UID:test-event-123
          DTSTART:20241225T100000Z
          DTEND:20241225T110000Z
          SUMMARY:Christmas Meeting
          DESCRIPTION:Annual holiday meeting
          LOCATION:Conference Room A
          END:VEVENT
          END:VCALENDAR
          """;

      ParsedEvent result = parser.parse(icalData);

      assertThat(result.uid()).isEqualTo("test-event-123");
      assertThat(result.summary()).isEqualTo("Christmas Meeting");
      assertThat(result.description()).isEqualTo("Annual holiday meeting");
      assertThat(result.location()).isEqualTo("Conference Room A");
      assertThat(result.dtstart()).isNotNull();
      assertThat(result.dtend()).isNotNull();
      assertThat(result.allDay()).isFalse();
    }

    @Test
    @DisplayName("should parse all-day event")
    void shouldParseAllDayEvent() {
      String icalData = """
          BEGIN:VCALENDAR
          VERSION:2.0
          PRODID:-//Test//Test//EN
          BEGIN:VEVENT
          UID:all-day-event
          DTSTART;VALUE=DATE:20241225
          DTEND;VALUE=DATE:20241226
          SUMMARY:Christmas Day
          END:VEVENT
          END:VCALENDAR
          """;

      ParsedEvent result = parser.parse(icalData);

      assertThat(result.uid()).isEqualTo("all-day-event");
      assertThat(result.summary()).isEqualTo("Christmas Day");
      assertThat(result.allDay()).isTrue();
      assertThat(result.dtstart()).isNotNull();
      assertThat(result.dtstart().getHour()).isEqualTo(0);
      assertThat(result.dtstart().getMinute()).isEqualTo(0);
    }

    @Test
    @DisplayName("should parse recurring event with RRULE")
    void shouldParseRecurringEvent() {
      String icalData = """
          BEGIN:VCALENDAR
          VERSION:2.0
          PRODID:-//Test//Test//EN
          BEGIN:VEVENT
          UID:recurring-event
          DTSTART:20241225T090000Z
          DTEND:20241225T100000Z
          SUMMARY:Weekly Meeting
          RRULE:FREQ=WEEKLY;BYDAY=MO,WE,FR;COUNT=10
          END:VEVENT
          END:VCALENDAR
          """;

      ParsedEvent result = parser.parse(icalData);

      assertThat(result.uid()).isEqualTo("recurring-event");
      assertThat(result.rrule()).contains("FREQ=WEEKLY");
      assertThat(result.rrule()).contains("BYDAY=MO,WE,FR");
      assertThat(result.rrule()).contains("COUNT=10");
    }

    @Test
    @DisplayName("should parse event with minimal properties")
    void shouldParseMinimalEvent() {
      String icalData = """
          BEGIN:VCALENDAR
          VERSION:2.0
          PRODID:-//Test//Test//EN
          BEGIN:VEVENT
          UID:minimal-event
          DTSTART:20241225T100000Z
          END:VEVENT
          END:VCALENDAR
          """;

      ParsedEvent result = parser.parse(icalData);

      assertThat(result.uid()).isEqualTo("minimal-event");
      assertThat(result.summary()).isNull();
      assertThat(result.description()).isNull();
      assertThat(result.location()).isNull();
      assertThat(result.dtend()).isNull();
      assertThat(result.rrule()).isNull();
    }

    @Test
    @DisplayName("should throw exception when no VEVENT found")
    void shouldThrowExceptionWhenNoVEvent() {
      String icalData = """
          BEGIN:VCALENDAR
          VERSION:2.0
          PRODID:-//Test//Test//EN
          END:VCALENDAR
          """;

      assertThatThrownBy(() -> parser.parse(icalData))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("No VEVENT found");
    }

    @Test
    @DisplayName("should throw exception for invalid iCal data")
    void shouldThrowExceptionForInvalidData() {
      String invalidData = "This is not valid iCalendar data";

      assertThatThrownBy(() -> parser.parse(invalidData))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Failed to parse");
    }

    @Test
    @DisplayName("should parse event with timezone")
    void shouldParseEventWithTimezone() {
      String icalData = """
          BEGIN:VCALENDAR
          VERSION:2.0
          PRODID:-//Test//Test//EN
          BEGIN:VEVENT
          UID:tz-event
          DTSTART;TZID=Asia/Seoul:20241225T190000
          DTEND;TZID=Asia/Seoul:20241225T200000
          SUMMARY:Seoul Meeting
          END:VEVENT
          END:VCALENDAR
          """;

      ParsedEvent result = parser.parse(icalData);

      assertThat(result.uid()).isEqualTo("tz-event");
      assertThat(result.summary()).isEqualTo("Seoul Meeting");
      assertThat(result.dtstart()).isNotNull();
    }

    @Test
    @DisplayName("should parse event with special characters in summary")
    void shouldParseEventWithSpecialCharacters() {
      String icalData = """
          BEGIN:VCALENDAR
          VERSION:2.0
          PRODID:-//Test//Test//EN
          BEGIN:VEVENT
          UID:special-chars
          DTSTART:20241225T100000Z
          SUMMARY:Meeting: Team & Partners (Q4)
          DESCRIPTION:Discussion about "Q4 Goals" & Budget
          END:VEVENT
          END:VCALENDAR
          """;

      ParsedEvent result = parser.parse(icalData);

      assertThat(result.summary()).isEqualTo("Meeting: Team & Partners (Q4)");
      assertThat(result.description()).contains("Q4 Goals");
    }

    @Test
    @DisplayName("should throw exception for empty string")
    void shouldThrowExceptionForEmptyString() {
      assertThatThrownBy(() -> parser.parse(""))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("should throw exception for null input")
    void shouldThrowExceptionForNullInput() {
      assertThatThrownBy(() -> parser.parse(null))
          .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("should parse only first VEVENT when multiple exist")
    void shouldParseFirstVEventWhenMultipleExist() {
      String icalData = """
          BEGIN:VCALENDAR
          VERSION:2.0
          PRODID:-//Test//Test//EN
          BEGIN:VEVENT
          UID:first-event
          DTSTART:20241225T100000Z
          SUMMARY:First Event
          END:VEVENT
          BEGIN:VEVENT
          UID:second-event
          DTSTART:20241226T100000Z
          SUMMARY:Second Event
          END:VEVENT
          END:VCALENDAR
          """;

      ParsedEvent result = parser.parse(icalData);

      assertThat(result.uid()).isEqualTo("first-event");
      assertThat(result.summary()).isEqualTo("First Event");
    }

    @Test
    @DisplayName("should parse event with unicode characters")
    void shouldParseEventWithUnicode() {
      String icalData = """
          BEGIN:VCALENDAR
          VERSION:2.0
          PRODID:-//Test//Test//EN
          BEGIN:VEVENT
          UID:unicode-event
          DTSTART:20241225T100000Z
          SUMMARY:회의 - 한글 테스트
          DESCRIPTION:日本語テスト 🎉
          LOCATION:서울 강남구
          END:VEVENT
          END:VCALENDAR
          """;

      ParsedEvent result = parser.parse(icalData);

      assertThat(result.uid()).isEqualTo("unicode-event");
      assertThat(result.summary()).isEqualTo("회의 - 한글 테스트");
      assertThat(result.description()).contains("日本語テスト");
      assertThat(result.location()).isEqualTo("서울 강남구");
    }

    @Test
    @DisplayName("should parse event with very long description")
    void shouldParseEventWithLongDescription() {
      String longDescription = "A".repeat(10000);
      String icalData = """
          BEGIN:VCALENDAR
          VERSION:2.0
          PRODID:-//Test//Test//EN
          BEGIN:VEVENT
          UID:long-desc-event
          DTSTART:20241225T100000Z
          SUMMARY:Long Description Event
          DESCRIPTION:%s
          END:VEVENT
          END:VCALENDAR
          """.formatted(longDescription);

      ParsedEvent result = parser.parse(icalData);

      assertThat(result.description()).hasSize(10000);
    }

    @Test
    @DisplayName("should parse event with newlines in description")
    void shouldParseEventWithNewlinesInDescription() {
      String icalData = """
          BEGIN:VCALENDAR
          VERSION:2.0
          PRODID:-//Test//Test//EN
          BEGIN:VEVENT
          UID:newline-event
          DTSTART:20241225T100000Z
          SUMMARY:Multiline Event
          DESCRIPTION:Line 1\\nLine 2\\nLine 3
          END:VEVENT
          END:VCALENDAR
          """;

      ParsedEvent result = parser.parse(icalData);

      assertThat(result.description()).contains("Line 1");
    }
  }

  @Nested
  @DisplayName("validate()")
  class ValidateTests {

    @Test
    @DisplayName("should return valid for correct iCal data")
    void shouldReturnValidForCorrectData() {
      String icalData = """
          BEGIN:VCALENDAR
          VERSION:2.0
          PRODID:-//Test//Test//EN
          BEGIN:VEVENT
          UID:valid-event
          DTSTART:20241225T100000Z
          SUMMARY:Valid Event
          END:VEVENT
          END:VCALENDAR
          """;

      ValidationResult result = parser.validate(icalData);

      assertThat(result.valid()).isTrue();
      assertThat(result.error()).isNull();
    }

    @Test
    @DisplayName("should return invalid when no VEVENT")
    void shouldReturnInvalidWhenNoVEvent() {
      String icalData = """
          BEGIN:VCALENDAR
          VERSION:2.0
          PRODID:-//Test//Test//EN
          END:VCALENDAR
          """;

      ValidationResult result = parser.validate(icalData);

      assertThat(result.valid()).isFalse();
      assertThat(result.error()).contains("No VEVENT");
    }

    @Test
    @DisplayName("should return invalid when missing UID")
    void shouldReturnInvalidWhenMissingUid() {
      String icalData = """
          BEGIN:VCALENDAR
          VERSION:2.0
          PRODID:-//Test//Test//EN
          BEGIN:VEVENT
          DTSTART:20241225T100000Z
          SUMMARY:No UID Event
          END:VEVENT
          END:VCALENDAR
          """;

      ValidationResult result = parser.validate(icalData);

      assertThat(result.valid()).isFalse();
      assertThat(result.error()).contains("UID");
    }

    @Test
    @DisplayName("should return invalid when missing DTSTART")
    void shouldReturnInvalidWhenMissingDtstart() {
      String icalData = """
          BEGIN:VCALENDAR
          VERSION:2.0
          PRODID:-//Test//Test//EN
          BEGIN:VEVENT
          UID:no-dtstart
          SUMMARY:No DTSTART Event
          END:VEVENT
          END:VCALENDAR
          """;

      ValidationResult result = parser.validate(icalData);

      assertThat(result.valid()).isFalse();
      assertThat(result.error()).contains("DTSTART");
    }

    @Test
    @DisplayName("should return invalid for malformed iCal data")
    void shouldReturnInvalidForMalformedData() {
      String invalidData = "not valid icalendar";

      ValidationResult result = parser.validate(invalidData);

      assertThat(result.valid()).isFalse();
      assertThat(result.error()).contains("Failed to parse");
    }

    @Test
    @DisplayName("should return invalid for empty string")
    void shouldReturnInvalidForEmptyString() {
      ValidationResult result = parser.validate("");

      assertThat(result.valid()).isFalse();
    }

    @Test
    @DisplayName("should return invalid for null input")
    void shouldReturnInvalidForNullInput() {
      ValidationResult result = parser.validate(null);

      assertThat(result.valid()).isFalse();
    }
  }
}
