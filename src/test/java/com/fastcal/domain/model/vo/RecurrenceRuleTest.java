package com.fastcal.domain.model.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RecurrenceRule")
class RecurrenceRuleTest {

  @Nested
  @DisplayName("생성")
  class Creation {

    @Test
    @DisplayName("유효한 RRULE로 생성할 수 있다")
    void shouldCreateWithValidRRule() {
      RecurrenceRule rrule = RecurrenceRule.of("FREQ=DAILY");

      assertThat(rrule.getValue()).isEqualTo("FREQ=DAILY");
    }

    @Test
    @DisplayName("여러 파트를 가진 RRULE로 생성할 수 있다")
    void shouldCreateWithMultipleParts() {
      RecurrenceRule rrule = RecurrenceRule.of("FREQ=WEEKLY;BYDAY=MO,WE,FR;COUNT=10");

      assertThat(rrule.getValue()).isEqualTo("FREQ=WEEKLY;BYDAY=MO,WE,FR;COUNT=10");
    }

    @Test
    @DisplayName("RRULE: 접두사가 있으면 제거된다")
    void shouldRemoveRRulePrefix() {
      RecurrenceRule rrule = RecurrenceRule.of("RRULE:FREQ=DAILY");

      assertThat(rrule.getValue()).isEqualTo("FREQ=DAILY");
    }

    @Test
    @DisplayName("of 메서드로 생성할 수 있다")
    void shouldCreateWithOfMethod() {
      RecurrenceRule rrule = RecurrenceRule.of("FREQ=MONTHLY");

      assertThat(rrule).isNotNull();
      assertThat(rrule.getValue()).isEqualTo("FREQ=MONTHLY");
    }

    @Test
    @DisplayName("ofNullable은 null 입력시 null을 반환한다")
    void shouldReturnNullForNullInput() {
      RecurrenceRule rrule = RecurrenceRule.ofNullable(null);

      assertThat(rrule).isNull();
    }

    @Test
    @DisplayName("ofNullable은 빈 문자열 입력시 null을 반환한다")
    void shouldReturnNullForBlankInput() {
      RecurrenceRule rrule = RecurrenceRule.ofNullable("   ");

      assertThat(rrule).isNull();
    }

    @Test
    @DisplayName("앞뒤 공백이 제거된다")
    void shouldTrimWhitespace() {
      RecurrenceRule rrule = RecurrenceRule.of("  FREQ=DAILY  ");

      assertThat(rrule.getValue()).isEqualTo("FREQ=DAILY");
    }

    @Test
    @DisplayName("null 값으로 생성하면 value는 null이다")
    void shouldAllowNullValue() {
      RecurrenceRule rrule = RecurrenceRule.of(null);

      assertThat(rrule.getValue()).isNull();
    }

    @Test
    @DisplayName("빈 문자열로 생성하면 value는 null이다")
    void shouldConvertEmptyToNull() {
      RecurrenceRule rrule = RecurrenceRule.of("");

      assertThat(rrule.getValue()).isNull();
    }
  }

  @Nested
  @DisplayName("FREQ 유효성 검증")
  class FreqValidation {

    @Test
    @DisplayName("FREQ가 없으면 예외가 발생한다")
    void shouldRejectMissingFreq() {
      assertThatThrownBy(() -> RecurrenceRule.of("BYDAY=MO"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("must contain FREQ rule part");
    }

    @Test
    @DisplayName("FREQ가 두 번 있으면 예외가 발생한다")
    void shouldRejectDuplicateFreq() {
      assertThatThrownBy(() -> RecurrenceRule.of("FREQ=DAILY;FREQ=WEEKLY"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("must not occur more than once");
    }

    @Test
    @DisplayName("유효하지 않은 FREQ 값은 예외가 발생한다")
    void shouldRejectInvalidFreqValue() {
      assertThatThrownBy(() -> RecurrenceRule.of("FREQ=INVALID"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Invalid FREQ value");
    }

    @Test
    @DisplayName("모든 유효한 FREQ 값을 허용한다")
    void shouldAcceptAllValidFreqValues() {
      assertThat(RecurrenceRule.of("FREQ=SECONDLY")).isNotNull();
      assertThat(RecurrenceRule.of("FREQ=MINUTELY")).isNotNull();
      assertThat(RecurrenceRule.of("FREQ=HOURLY")).isNotNull();
      assertThat(RecurrenceRule.of("FREQ=DAILY")).isNotNull();
      assertThat(RecurrenceRule.of("FREQ=WEEKLY")).isNotNull();
      assertThat(RecurrenceRule.of("FREQ=MONTHLY")).isNotNull();
      assertThat(RecurrenceRule.of("FREQ=YEARLY")).isNotNull();
    }
  }

  @Nested
  @DisplayName("UNTIL과 COUNT 유효성 검증")
  class UntilCountValidation {

    @Test
    @DisplayName("UNTIL과 COUNT가 동시에 있으면 예외가 발생한다")
    void shouldRejectBothUntilAndCount() {
      assertThatThrownBy(() -> RecurrenceRule.of("FREQ=DAILY;UNTIL=20241231;COUNT=10"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("cannot contain both UNTIL and COUNT");
    }

    @Test
    @DisplayName("COUNT가 음수이면 예외가 발생한다")
    void shouldRejectNegativeCount() {
      assertThatThrownBy(() -> RecurrenceRule.of("FREQ=DAILY;COUNT=-1"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("must be a positive integer");
    }

    @Test
    @DisplayName("COUNT가 0이면 예외가 발생한다")
    void shouldRejectZeroCount() {
      assertThatThrownBy(() -> RecurrenceRule.of("FREQ=DAILY;COUNT=0"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("must be a positive integer");
    }

    @Test
    @DisplayName("유효한 COUNT를 허용한다")
    void shouldAcceptValidCount() {
      RecurrenceRule rrule = RecurrenceRule.of("FREQ=DAILY;COUNT=10");

      assertThat(rrule.getValue()).contains("COUNT=10");
    }

    @Test
    @DisplayName("유효한 UNTIL을 허용한다")
    void shouldAcceptValidUntil() {
      RecurrenceRule rrule = RecurrenceRule.of("FREQ=DAILY;UNTIL=20241231T235959Z");

      assertThat(rrule.getValue()).contains("UNTIL=20241231T235959Z");
    }
  }

  @Nested
  @DisplayName("INTERVAL 유효성 검증")
  class IntervalValidation {

    @Test
    @DisplayName("INTERVAL이 음수이면 예외가 발생한다")
    void shouldRejectNegativeInterval() {
      assertThatThrownBy(() -> RecurrenceRule.of("FREQ=DAILY;INTERVAL=-1"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("must be a positive integer");
    }

    @Test
    @DisplayName("INTERVAL이 0이면 예외가 발생한다")
    void shouldRejectZeroInterval() {
      assertThatThrownBy(() -> RecurrenceRule.of("FREQ=DAILY;INTERVAL=0"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("must be a positive integer");
    }

    @Test
    @DisplayName("유효한 INTERVAL을 허용한다")
    void shouldAcceptValidInterval() {
      RecurrenceRule rrule = RecurrenceRule.of("FREQ=WEEKLY;INTERVAL=2");

      assertThat(rrule.getValue()).contains("INTERVAL=2");
    }
  }

  @Nested
  @DisplayName("BYDAY 유효성 검증")
  class ByDayValidation {

    @Test
    @DisplayName("유효한 BYDAY 값을 허용한다")
    void shouldAcceptValidByDay() {
      RecurrenceRule rrule = RecurrenceRule.of("FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR");

      assertThat(rrule.getValue()).contains("BYDAY=MO,TU,WE,TH,FR");
    }

    @Test
    @DisplayName("nth 형식의 BYDAY를 허용한다")
    void shouldAcceptNthByDay() {
      RecurrenceRule rrule = RecurrenceRule.of("FREQ=MONTHLY;BYDAY=2MO");

      assertThat(rrule.getValue()).contains("BYDAY=2MO");
    }

    @Test
    @DisplayName("음수 nth 형식의 BYDAY를 허용한다")
    void shouldAcceptNegativeNthByDay() {
      RecurrenceRule rrule = RecurrenceRule.of("FREQ=MONTHLY;BYDAY=-1FR");

      assertThat(rrule.getValue()).contains("BYDAY=-1FR");
    }

    @Test
    @DisplayName("유효하지 않은 요일은 예외가 발생한다")
    void shouldRejectInvalidWeekday() {
      assertThatThrownBy(() -> RecurrenceRule.of("FREQ=WEEKLY;BYDAY=XX"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Invalid BYDAY value");
    }
  }

  @Nested
  @DisplayName("BY* 범위 유효성 검증")
  class ByRangeValidation {

    @Test
    @DisplayName("BYSECOND 범위를 검증한다 (0-60)")
    void shouldValidateBySecondRange() {
      assertThat(RecurrenceRule.of("FREQ=MINUTELY;BYSECOND=0,30,60")).isNotNull();

      assertThatThrownBy(() -> RecurrenceRule.of("FREQ=MINUTELY;BYSECOND=61"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("out of valid range");
    }

    @Test
    @DisplayName("BYMINUTE 범위를 검증한다 (0-59)")
    void shouldValidateByMinuteRange() {
      assertThat(RecurrenceRule.of("FREQ=HOURLY;BYMINUTE=0,30,59")).isNotNull();

      assertThatThrownBy(() -> RecurrenceRule.of("FREQ=HOURLY;BYMINUTE=60"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("out of valid range");
    }

    @Test
    @DisplayName("BYHOUR 범위를 검증한다 (0-23)")
    void shouldValidateByHourRange() {
      assertThat(RecurrenceRule.of("FREQ=DAILY;BYHOUR=0,12,23")).isNotNull();

      assertThatThrownBy(() -> RecurrenceRule.of("FREQ=DAILY;BYHOUR=24"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("out of valid range");
    }

    @Test
    @DisplayName("BYMONTH 범위를 검증한다 (1-12)")
    void shouldValidateByMonthRange() {
      assertThat(RecurrenceRule.of("FREQ=YEARLY;BYMONTH=1,6,12")).isNotNull();

      assertThatThrownBy(() -> RecurrenceRule.of("FREQ=YEARLY;BYMONTH=0"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("out of valid range");

      assertThatThrownBy(() -> RecurrenceRule.of("FREQ=YEARLY;BYMONTH=13"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("out of valid range");
    }

    @Test
    @DisplayName("BYMONTHDAY 범위를 검증한다 (-31 to 31, 0 제외)")
    void shouldValidateByMonthDayRange() {
      assertThat(RecurrenceRule.of("FREQ=MONTHLY;BYMONTHDAY=1,15,-1")).isNotNull();

      assertThatThrownBy(() -> RecurrenceRule.of("FREQ=MONTHLY;BYMONTHDAY=32"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("out of valid range");
    }
  }

  @Nested
  @DisplayName("WKST 유효성 검증")
  class WkstValidation {

    @Test
    @DisplayName("유효한 WKST 값을 허용한다")
    void shouldAcceptValidWkst() {
      assertThat(RecurrenceRule.of("FREQ=WEEKLY;WKST=MO")).isNotNull();
      assertThat(RecurrenceRule.of("FREQ=WEEKLY;WKST=SU")).isNotNull();
    }

    @Test
    @DisplayName("유효하지 않은 WKST 값은 예외가 발생한다")
    void shouldRejectInvalidWkst() {
      assertThatThrownBy(() -> RecurrenceRule.of("FREQ=WEEKLY;WKST=XX"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Invalid WKST value");
    }
  }

  @Nested
  @DisplayName("getFrequency")
  class GetFrequency {

    @Test
    @DisplayName("RRULE에서 빈도를 추출한다")
    void shouldExtractFrequency() {
      RecurrenceRule rrule = RecurrenceRule.of("FREQ=WEEKLY;BYDAY=MO");

      assertThat(rrule.getFrequency()).isEqualTo("WEEKLY");
    }

    @Test
    @DisplayName("null RRULE에서는 null을 반환한다")
    void shouldReturnNullForNullRRule() {
      RecurrenceRule rrule = RecurrenceRule.of(null);

      assertThat(rrule.getFrequency()).isNull();
    }
  }

  @Nested
  @DisplayName("toICalFormat")
  class ToICalFormat {

    @Test
    @DisplayName("RRULE: 접두사를 포함한 형식으로 반환한다")
    void shouldReturnWithRRulePrefix() {
      RecurrenceRule rrule = RecurrenceRule.of("FREQ=DAILY");

      assertThat(rrule.toICalFormat()).isEqualTo("RRULE:FREQ=DAILY");
    }

    @Test
    @DisplayName("null 값이면 빈 문자열을 반환한다")
    void shouldReturnEmptyForNull() {
      RecurrenceRule rrule = RecurrenceRule.of(null);

      assertThat(rrule.toICalFormat()).isEqualTo("");
    }
  }

  @Nested
  @DisplayName("isEmpty")
  class IsEmpty {

    @Test
    @DisplayName("null 값이면 isEmpty는 true를 반환한다")
    void shouldReturnTrueForNullValue() {
      RecurrenceRule rrule = RecurrenceRule.of(null);

      assertThat(rrule.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("값이 있으면 isEmpty는 false를 반환한다")
    void shouldReturnFalseForNonEmptyValue() {
      RecurrenceRule rrule = RecurrenceRule.of("FREQ=DAILY");

      assertThat(rrule.isEmpty()).isFalse();
    }
  }

  @Nested
  @DisplayName("동등성")
  class Equality {

    @Test
    @DisplayName("같은 값을 가진 RecurrenceRule은 동등하다")
    void shouldBeEqualForSameValue() {
      RecurrenceRule rrule1 = RecurrenceRule.of("FREQ=DAILY;COUNT=10");
      RecurrenceRule rrule2 = RecurrenceRule.of("FREQ=DAILY;COUNT=10");

      assertThat(rrule1).isEqualTo(rrule2);
      assertThat(rrule1.hashCode()).isEqualTo(rrule2.hashCode());
    }

    @Test
    @DisplayName("다른 값을 가진 RecurrenceRule은 동등하지 않다")
    void shouldNotBeEqualForDifferentValue() {
      RecurrenceRule rrule1 = RecurrenceRule.of("FREQ=DAILY");
      RecurrenceRule rrule2 = RecurrenceRule.of("FREQ=WEEKLY");

      assertThat(rrule1).isNotEqualTo(rrule2);
    }
  }

  @Nested
  @DisplayName("toString")
  class ToStringTest {

    @Test
    @DisplayName("toString은 값을 반환한다")
    void shouldReturnValue() {
      RecurrenceRule rrule = RecurrenceRule.of("FREQ=DAILY");

      assertThat(rrule.toString()).isEqualTo("FREQ=DAILY");
    }

    @Test
    @DisplayName("null 값이면 toString은 빈 문자열을 반환한다")
    void shouldReturnEmptyStringForNull() {
      RecurrenceRule rrule = RecurrenceRule.of(null);

      assertThat(rrule.toString()).isEqualTo("");
    }
  }

  @Nested
  @DisplayName("복잡한 RRULE")
  class ComplexRules {

    @Test
    @DisplayName("복잡한 주간 반복 규칙을 처리한다")
    void shouldHandleComplexWeeklyRule() {
      RecurrenceRule rrule = RecurrenceRule.of(
          "FREQ=WEEKLY;INTERVAL=2;BYDAY=MO,WE,FR;UNTIL=20251231T235959Z");

      assertThat(rrule.getValue()).contains("FREQ=WEEKLY");
      assertThat(rrule.getValue()).contains("INTERVAL=2");
      assertThat(rrule.getValue()).contains("BYDAY=MO,WE,FR");
      assertThat(rrule.getFrequency()).isEqualTo("WEEKLY");
    }

    @Test
    @DisplayName("복잡한 월간 반복 규칙을 처리한다")
    void shouldHandleComplexMonthlyRule() {
      RecurrenceRule rrule = RecurrenceRule.of(
          "FREQ=MONTHLY;BYDAY=2MO;COUNT=12");

      assertThat(rrule.getValue()).contains("FREQ=MONTHLY");
      assertThat(rrule.getValue()).contains("BYDAY=2MO");
      assertThat(rrule.getFrequency()).isEqualTo("MONTHLY");
    }

    @Test
    @DisplayName("복잡한 연간 반복 규칙을 처리한다")
    void shouldHandleComplexYearlyRule() {
      RecurrenceRule rrule = RecurrenceRule.of(
          "FREQ=YEARLY;BYMONTH=12;BYMONTHDAY=25");

      assertThat(rrule.getValue()).contains("FREQ=YEARLY");
      assertThat(rrule.getValue()).contains("BYMONTH=12");
      assertThat(rrule.getValue()).contains("BYMONTHDAY=25");
      assertThat(rrule.getFrequency()).isEqualTo("YEARLY");
    }
  }
}
