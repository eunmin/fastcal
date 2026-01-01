package com.fastcal.domain.model.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UserId")
class UserIdTest {

  @Nested
  @DisplayName("of()")
  class OfTests {

    @Test
    @DisplayName("should create UserId with valid username")
    void shouldCreateWithValidUsername() {
      UserId userId = UserId.of("eunmin");
      assertThat(userId.getValue()).isEqualTo("eunmin");
    }

    @Test
    @DisplayName("should create UserId with valid email")
    void shouldCreateWithValidEmail() {
      UserId userId = UserId.of("user@example.com");
      assertThat(userId.getValue()).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("should accept mixed case")
    void shouldAcceptMixedCase() {
      UserId userId = UserId.of("UserName123");
      assertThat(userId.getValue()).isEqualTo("UserName123");
    }

    @Test
    @DisplayName("should throw on null value")
    void shouldThrowOnNull() {
      assertThatThrownBy(() -> UserId.of(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("cannot be null or blank");
    }

    @Test
    @DisplayName("should throw on blank value")
    void shouldThrowOnBlank() {
      assertThatThrownBy(() -> UserId.of("   "))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("cannot be null or blank");
    }

    @Test
    @DisplayName("should throw on invalid characters")
    void shouldThrowOnInvalidCharacters() {
      assertThatThrownBy(() -> UserId.of("user/name"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("invalid characters");
    }

    @Test
    @DisplayName("should throw on exceeding max length")
    void shouldThrowOnExceedingMaxLength() {
      String longId = "a".repeat(256);
      assertThatThrownBy(() -> UserId.of(longId))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("255");
    }
  }

  @Nested
  @DisplayName("equality")
  class EqualityTests {

    @Test
    @DisplayName("should be equal for same userId")
    void shouldBeEqualForSameUserId() {
      UserId userId1 = UserId.of("eunmin");
      UserId userId2 = UserId.of("eunmin");
      assertThat(userId1).isEqualTo(userId2);
    }

    @Test
    @DisplayName("should not be equal for different case")
    void shouldNotBeEqualForDifferentCase() {
      UserId userId1 = UserId.of("eunmin");
      UserId userId2 = UserId.of("EUNMIN");
      assertThat(userId1).isNotEqualTo(userId2);
    }
  }
}
