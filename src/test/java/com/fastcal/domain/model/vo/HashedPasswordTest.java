package com.fastcal.domain.model.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("HashedPassword")
class HashedPasswordTest {

  private static final String BCRYPT_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqBuBhsxLcZPjVpzQ3S.Rk9Y3L0Wy";

  @Nested
  @DisplayName("of()")
  class OfTests {

    @Test
    @DisplayName("should create HashedPassword with valid value")
    void shouldCreateWithValidValue() {
      HashedPassword password = HashedPassword.of(BCRYPT_HASH);
      assertThat(password.getValue()).isEqualTo(BCRYPT_HASH);
    }

    @Test
    @DisplayName("should trim whitespace")
    void shouldTrimWhitespace() {
      HashedPassword password = HashedPassword.of("  " + BCRYPT_HASH + "  ");
      assertThat(password.getValue()).isEqualTo(BCRYPT_HASH);
    }

    @Test
    @DisplayName("should throw on null value")
    void shouldThrowOnNull() {
      assertThatThrownBy(() -> HashedPassword.of(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("cannot be null or blank");
    }

    @Test
    @DisplayName("should throw on blank value")
    void shouldThrowOnBlank() {
      assertThatThrownBy(() -> HashedPassword.of("   "))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("cannot be null or blank");
    }
  }

  @Nested
  @DisplayName("ofNullable()")
  class OfNullableTests {

    @Test
    @DisplayName("should return null for null input")
    void shouldReturnNullForNull() {
      assertThat(HashedPassword.ofNullable(null)).isNull();
    }

    @Test
    @DisplayName("should return null for blank input")
    void shouldReturnNullForBlank() {
      assertThat(HashedPassword.ofNullable("   ")).isNull();
    }

    @Test
    @DisplayName("should create HashedPassword for valid input")
    void shouldCreateForValidInput() {
      HashedPassword password = HashedPassword.ofNullable(BCRYPT_HASH);
      assertThat(password).isNotNull();
      assertThat(password.getValue()).isEqualTo(BCRYPT_HASH);
    }
  }

  @Nested
  @DisplayName("equality")
  class EqualityTests {

    @Test
    @DisplayName("should be equal for same hash")
    void shouldBeEqualForSameHash() {
      HashedPassword password1 = HashedPassword.of(BCRYPT_HASH);
      HashedPassword password2 = HashedPassword.of(BCRYPT_HASH);
      assertThat(password1).isEqualTo(password2);
    }

    @Test
    @DisplayName("should not be equal for different hash")
    void shouldNotBeEqualForDifferentHash() {
      HashedPassword password1 = HashedPassword.of(BCRYPT_HASH);
      HashedPassword password2 = HashedPassword.of("$2a$10$different");
      assertThat(password1).isNotEqualTo(password2);
    }
  }

  @Nested
  @DisplayName("toString()")
  class ToStringTests {

    @Test
    @DisplayName("should return protected message instead of actual value")
    void shouldReturnProtectedMessage() {
      HashedPassword password = HashedPassword.of(BCRYPT_HASH);
      assertThat(password.toString()).isEqualTo("[PROTECTED]");
      assertThat(password.toString()).doesNotContain(BCRYPT_HASH);
    }
  }
}
