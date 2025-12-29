package com.fastcal.domain.model.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CalendarTimezone")
class CalendarTimezoneTest {

  private static final String VTIMEZONE_EXAMPLE = """
      BEGIN:VTIMEZONE
      TZID:America/New_York
      BEGIN:STANDARD
      DTSTART:19701101T020000
      RRULE:FREQ=YEARLY;BYMONTH=11;BYDAY=1SU
      TZOFFSETFROM:-0400
      TZOFFSETTO:-0500
      TZNAME:EST
      END:STANDARD
      BEGIN:DAYLIGHT
      DTSTART:19700308T020000
      RRULE:FREQ=YEARLY;BYMONTH=3;BYDAY=2SU
      TZOFFSETFROM:-0500
      TZOFFSETTO:-0400
      TZNAME:EDT
      END:DAYLIGHT
      END:VTIMEZONE
      """;

  @Nested
  @DisplayName("IANA timezone ID")
  class IanaTimezoneTests {

    @Test
    @DisplayName("should accept valid IANA timezone")
    void shouldAcceptValidTimezone() {
      CalendarTimezone tz = CalendarTimezone.of("Asia/Seoul");
      assertThat(tz.getValue()).isEqualTo("Asia/Seoul");
      assertThat(tz.isIanaTimezone()).isTrue();
      assertThat(tz.isVTimezone()).isFalse();
    }

    @Test
    @DisplayName("should convert to ZoneId")
    void shouldConvertToZoneId() {
      CalendarTimezone tz = CalendarTimezone.of("America/New_York");
      assertThat(tz.toZoneId()).isEqualTo(ZoneId.of("America/New_York"));
    }

    @Test
    @DisplayName("should accept UTC")
    void shouldAcceptUtc() {
      CalendarTimezone tz = CalendarTimezone.of("UTC");
      assertThat(tz.getValue()).isEqualTo("UTC");
    }

    @Test
    @DisplayName("should trim whitespace")
    void shouldTrimWhitespace() {
      CalendarTimezone tz = CalendarTimezone.of("  Asia/Seoul  ");
      assertThat(tz.getValue()).isEqualTo("Asia/Seoul");
    }
  }

  @Nested
  @DisplayName("VTIMEZONE format")
  class VTimezoneTests {

    @Test
    @DisplayName("should accept VTIMEZONE component")
    void shouldAcceptVTimezone() {
      CalendarTimezone tz = CalendarTimezone.of(VTIMEZONE_EXAMPLE);
      assertThat(tz.isVTimezone()).isTrue();
      assertThat(tz.isIanaTimezone()).isFalse();
    }

    @Test
    @DisplayName("should return null for toZoneId() with VTIMEZONE")
    void shouldReturnNullZoneIdForVTimezone() {
      CalendarTimezone tz = CalendarTimezone.of(VTIMEZONE_EXAMPLE);
      assertThat(tz.toZoneId()).isNull();
    }

    @Test
    @DisplayName("should preserve VTIMEZONE content")
    void shouldPreserveContent() {
      CalendarTimezone tz = CalendarTimezone.of(VTIMEZONE_EXAMPLE);
      assertThat(tz.getValue()).contains("BEGIN:VTIMEZONE");
      assertThat(tz.getValue()).contains("END:VTIMEZONE");
    }
  }

  @Nested
  @DisplayName("validation")
  class ValidationTests {

    @Test
    @DisplayName("should handle null")
    void shouldHandleNull() {
      CalendarTimezone tz = CalendarTimezone.of(null);
      assertThat(tz.getValue()).isNull();
    }

    @Test
    @DisplayName("should convert blank to null")
    void shouldConvertBlankToNull() {
      CalendarTimezone tz = CalendarTimezone.of("   ");
      assertThat(tz.getValue()).isNull();
    }

    @Test
    @DisplayName("should throw on invalid timezone")
    void shouldThrowOnInvalidTimezone() {
      assertThatThrownBy(() -> CalendarTimezone.of("Invalid/Timezone"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Invalid timezone");
    }
  }

  @Nested
  @DisplayName("ofNullable()")
  class OfNullableTests {

    @Test
    @DisplayName("should return null for null input")
    void shouldReturnNullForNull() {
      assertThat(CalendarTimezone.ofNullable(null)).isNull();
    }

    @Test
    @DisplayName("should return null for invalid timezone")
    void shouldReturnNullForInvalid() {
      assertThat(CalendarTimezone.ofNullable("Invalid/Timezone")).isNull();
    }

    @Test
    @DisplayName("should create for valid timezone")
    void shouldCreateForValid() {
      CalendarTimezone tz = CalendarTimezone.ofNullable("Asia/Seoul");
      assertThat(tz).isNotNull();
      assertThat(tz.getValue()).isEqualTo("Asia/Seoul");
    }
  }

  @Nested
  @DisplayName("ofZoneId()")
  class OfZoneIdTests {

    @Test
    @DisplayName("should create from ZoneId")
    void shouldCreateFromZoneId() {
      CalendarTimezone tz = CalendarTimezone.ofZoneId(ZoneId.of("Europe/London"));
      assertThat(tz.getValue()).isEqualTo("Europe/London");
    }

    @Test
    @DisplayName("should return null for null ZoneId")
    void shouldReturnNullForNullZoneId() {
      assertThat(CalendarTimezone.ofZoneId(null)).isNull();
    }
  }
}
