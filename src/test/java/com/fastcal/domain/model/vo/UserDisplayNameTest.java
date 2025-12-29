package com.fastcal.domain.model.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UserDisplayName")
class UserDisplayNameTest {

  @Nested
  @DisplayName("of()")
  class OfTests {

    @Test
    @DisplayName("should create UserDisplayName with valid value")
    void shouldCreateWithValidValue() {
      UserDisplayName displayName = UserDisplayName.of("John Doe");
      assertThat(displayName.getValue()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("should trim whitespace")
    void shouldTrimWhitespace() {
      UserDisplayName displayName = UserDisplayName.of("  John Doe  ");
      assertThat(displayName.getValue()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("should allow null value")
    void shouldAllowNull() {
      UserDisplayName displayName = UserDisplayName.of(null);
      assertThat(displayName.getValue()).isNull();
    }

    @Test
    @DisplayName("should convert blank to null")
    void shouldConvertBlankToNull() {
      UserDisplayName displayName = UserDisplayName.of("   ");
      assertThat(displayName.getValue()).isNull();
    }

    @Test
    @DisplayName("should convert empty to null")
    void shouldConvertEmptyToNull() {
      UserDisplayName displayName = UserDisplayName.of("");
      assertThat(displayName.getValue()).isNull();
    }

    @Test
    @DisplayName("should throw on exceeding max length")
    void shouldThrowOnExceedingMaxLength() {
      String longName = "a".repeat(101);
      assertThatThrownBy(() -> UserDisplayName.of(longName))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("100");
    }

    @Test
    @DisplayName("should allow max length")
    void shouldAllowMaxLength() {
      String maxName = "a".repeat(100);
      UserDisplayName displayName = UserDisplayName.of(maxName);
      assertThat(displayName.getValue()).isEqualTo(maxName);
    }
  }

  @Nested
  @DisplayName("ofNullable()")
  class OfNullableTests {

    @Test
    @DisplayName("should return null for null input")
    void shouldReturnNullForNull() {
      assertThat(UserDisplayName.ofNullable(null)).isNull();
    }

    @Test
    @DisplayName("should return null for blank input")
    void shouldReturnNullForBlank() {
      assertThat(UserDisplayName.ofNullable("   ")).isNull();
    }

    @Test
    @DisplayName("should create UserDisplayName for valid input")
    void shouldCreateForValidInput() {
      UserDisplayName displayName = UserDisplayName.ofNullable("John Doe");
      assertThat(displayName).isNotNull();
      assertThat(displayName.getValue()).isEqualTo("John Doe");
    }
  }

  @Nested
  @DisplayName("equality")
  class EqualityTests {

    @Test
    @DisplayName("should be equal for same name")
    void shouldBeEqualForSameName() {
      UserDisplayName name1 = UserDisplayName.of("John Doe");
      UserDisplayName name2 = UserDisplayName.of("John Doe");
      assertThat(name1).isEqualTo(name2);
    }

    @Test
    @DisplayName("should not be equal for different names")
    void shouldNotBeEqualForDifferentNames() {
      UserDisplayName name1 = UserDisplayName.of("John Doe");
      UserDisplayName name2 = UserDisplayName.of("Jane Doe");
      assertThat(name1).isNotEqualTo(name2);
    }

    @Test
    @DisplayName("should be equal for null values")
    void shouldBeEqualForNullValues() {
      UserDisplayName name1 = UserDisplayName.of(null);
      UserDisplayName name2 = UserDisplayName.of(null);
      assertThat(name1).isEqualTo(name2);
    }
  }

  @Nested
  @DisplayName("toString()")
  class ToStringTests {

    @Test
    @DisplayName("should return display name value")
    void shouldReturnDisplayNameValue() {
      UserDisplayName displayName = UserDisplayName.of("John Doe");
      assertThat(displayName.toString()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("should return null for null value")
    void shouldReturnNullForNullValue() {
      UserDisplayName displayName = UserDisplayName.of(null);
      assertThat(displayName.toString()).isNull();
    }
  }
}
