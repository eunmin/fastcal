package com.fastcal.domain.model.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("EventUid")
class EventUidTest {

  @Nested
  @DisplayName("생성")
  class Creation {

    @Test
    @DisplayName("유효한 UID로 생성할 수 있다")
    void shouldCreateWithValidUid() {
      EventUid uid = EventUid.of("event-123");

      assertThat(uid.getValue()).isEqualTo("event-123");
    }

    @Test
    @DisplayName("UUID 형식의 UID로 생성할 수 있다")
    void shouldCreateWithUuidFormat() {
      EventUid uid = EventUid.of("550e8400-e29b-41d4-a716-446655440000");

      assertThat(uid.getValue()).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
    }

    @Test
    @DisplayName("이메일 형식의 UID로 생성할 수 있다 (레거시 지원)")
    void shouldCreateWithEmailStyleUid() {
      EventUid uid = EventUid.of("event123@example.com");

      assertThat(uid.getValue()).isEqualTo("event123@example.com");
    }

    @Test
    @DisplayName("of 메서드로 생성할 수 있다")
    void shouldCreateWithOfMethod() {
      EventUid uid = EventUid.of("test-uid");

      assertThat(uid).isNotNull();
      assertThat(uid.getValue()).isEqualTo("test-uid");
    }

    @Test
    @DisplayName("ofNullable은 null 입력시 null을 반환한다")
    void shouldReturnNullForNullInput() {
      EventUid uid = EventUid.ofNullable(null);

      assertThat(uid).isNull();
    }

    @Test
    @DisplayName("ofNullable은 빈 문자열 입력시 null을 반환한다")
    void shouldReturnNullForBlankInput() {
      EventUid uid = EventUid.ofNullable("   ");

      assertThat(uid).isNull();
    }

    @Test
    @DisplayName("generate 메서드로 UUID 기반 UID를 생성할 수 있다")
    void shouldGenerateUuidBasedUid() {
      EventUid uid1 = EventUid.generate();
      EventUid uid2 = EventUid.generate();

      assertThat(uid1.getValue()).isNotBlank();
      assertThat(uid2.getValue()).isNotBlank();
      assertThat(uid1).isNotEqualTo(uid2);
    }

    @Test
    @DisplayName("앞뒤 공백이 제거된다")
    void shouldTrimWhitespace() {
      EventUid uid = EventUid.of("  event-123  ");

      assertThat(uid.getValue()).isEqualTo("event-123");
    }
  }

  @Nested
  @DisplayName("유효성 검증")
  class Validation {

    @Test
    @DisplayName("null 값은 허용되지 않는다")
    void shouldRejectNullValue() {
      assertThatThrownBy(() -> EventUid.of(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("cannot be null or blank");
    }

    @Test
    @DisplayName("빈 문자열은 허용되지 않는다")
    void shouldRejectEmptyValue() {
      assertThatThrownBy(() -> EventUid.of(""))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("cannot be null or blank");
    }

    @Test
    @DisplayName("공백만 있는 문자열은 허용되지 않는다")
    void shouldRejectBlankValue() {
      assertThatThrownBy(() -> EventUid.of("   "))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("cannot be null or blank");
    }

    @Test
    @DisplayName("255자를 초과하면 예외가 발생한다 (RFC 5545)")
    void shouldRejectExceedingMaxLength() {
      String longUid = "a".repeat(256);

      assertThatThrownBy(() -> EventUid.of(longUid))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("cannot exceed 255 octets");
    }

    @Test
    @DisplayName("255자는 허용된다")
    void shouldAcceptMaxLength() {
      String maxLengthUid = "a".repeat(255);

      EventUid uid = EventUid.of(maxLengthUid);

      assertThat(uid.getValue()).hasSize(255);
    }

    @Test
    @DisplayName("특수문자가 포함되면 예외가 발생한다")
    void shouldRejectSpecialCharacters() {
      assertThatThrownBy(() -> EventUid.of("event<>123"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("invalid characters");
    }

    @Test
    @DisplayName("공백이 포함되면 예외가 발생한다")
    void shouldRejectSpacesInValue() {
      assertThatThrownBy(() -> EventUid.of("event 123"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("invalid characters");
    }

    @Test
    @DisplayName("허용된 문자만 포함된 UID는 유효하다")
    void shouldAcceptAllowedCharacters() {
      EventUid uid = EventUid.of("event-123_test.uid@example.com");

      assertThat(uid.getValue()).isEqualTo("event-123_test.uid@example.com");
    }
  }

  @Nested
  @DisplayName("동등성")
  class Equality {

    @Test
    @DisplayName("같은 값을 가진 EventUid는 동등하다")
    void shouldBeEqualForSameValue() {
      EventUid uid1 = EventUid.of("event-123");
      EventUid uid2 = EventUid.of("event-123");

      assertThat(uid1).isEqualTo(uid2);
      assertThat(uid1.hashCode()).isEqualTo(uid2.hashCode());
    }

    @Test
    @DisplayName("다른 값을 가진 EventUid는 동등하지 않다")
    void shouldNotBeEqualForDifferentValue() {
      EventUid uid1 = EventUid.of("event-123");
      EventUid uid2 = EventUid.of("event-456");

      assertThat(uid1).isNotEqualTo(uid2);
    }
  }

  @Nested
  @DisplayName("toString")
  class ToStringTest {

    @Test
    @DisplayName("toString은 값을 반환한다")
    void shouldReturnValue() {
      EventUid uid = EventUid.of("event-123");

      assertThat(uid.toString()).isEqualTo("event-123");
    }
  }
}
