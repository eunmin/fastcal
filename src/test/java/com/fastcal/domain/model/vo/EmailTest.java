package com.fastcal.domain.model.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Email")
class EmailTest {

  @Nested
  @DisplayName("of()")
  class OfTests {

    @Test
    @DisplayName("should create Email with valid value")
    void shouldCreateWithValidEmail() {
      Email email = Email.of("user@example.com");
      assertThat(email.getValue()).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("should normalize to lowercase")
    void shouldNormalizeToLowercase() {
      Email email = Email.of("User@Example.COM");
      assertThat(email.getValue()).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("should trim whitespace")
    void shouldTrimWhitespace() {
      Email email = Email.of("  user@example.com  ");
      assertThat(email.getValue()).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("should throw on null value")
    void shouldThrowOnNull() {
      assertThatThrownBy(() -> Email.of(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("cannot be null or blank");
    }

    @Test
    @DisplayName("should throw on blank value")
    void shouldThrowOnBlank() {
      assertThatThrownBy(() -> Email.of("   "))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("cannot be null or blank");
    }

    @Test
    @DisplayName("should throw on invalid email format")
    void shouldThrowOnInvalidEmail() {
      assertThatThrownBy(() -> Email.of("invalid-email"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("valid email");
    }

    @Test
    @DisplayName("should throw on email without domain")
    void shouldThrowOnEmailWithoutDomain() {
      assertThatThrownBy(() -> Email.of("user@"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("valid email");
    }

    @Test
    @DisplayName("should throw on email exceeding max length")
    void shouldThrowOnExceedingMaxLength() {
      String longEmail = "a".repeat(250) + "@example.com";
      assertThatThrownBy(() -> Email.of(longEmail))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("255");
    }
  }

  @Nested
  @DisplayName("ofNullable()")
  class OfNullableTests {

    @Test
    @DisplayName("should return null for null input")
    void shouldReturnNullForNull() {
      assertThat(Email.ofNullable(null)).isNull();
    }

    @Test
    @DisplayName("should return null for blank input")
    void shouldReturnNullForBlank() {
      assertThat(Email.ofNullable("   ")).isNull();
    }

    @Test
    @DisplayName("should create Email for valid input")
    void shouldCreateForValidInput() {
      Email email = Email.ofNullable("user@example.com");
      assertThat(email).isNotNull();
      assertThat(email.getValue()).isEqualTo("user@example.com");
    }
  }

  @Nested
  @DisplayName("equality")
  class EqualityTests {

    @Test
    @DisplayName("should be equal for same email")
    void shouldBeEqualForSameEmail() {
      Email email1 = Email.of("user@example.com");
      Email email2 = Email.of("user@example.com");
      assertThat(email1).isEqualTo(email2);
    }

    @Test
    @DisplayName("should be equal for different case (normalized)")
    void shouldBeEqualForDifferentCase() {
      Email email1 = Email.of("user@example.com");
      Email email2 = Email.of("USER@EXAMPLE.COM");
      assertThat(email1).isEqualTo(email2);
    }
  }

  @Nested
  @DisplayName("toString()")
  class ToStringTests {

    @Test
    @DisplayName("should return email value")
    void shouldReturnEmailValue() {
      Email email = Email.of("user@example.com");
      assertThat(email.toString()).isEqualTo("user@example.com");
    }
  }
}
