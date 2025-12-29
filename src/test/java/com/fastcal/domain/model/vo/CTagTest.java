package com.fastcal.domain.model.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CTag")
class CTagTest {

  @Nested
  @DisplayName("of()")
  class OfTests {

    @Test
    @DisplayName("should create with valid value")
    void shouldCreateWithValidValue() {
      CTag ctag = CTag.of("abc123-def456");
      assertThat(ctag.getValue()).isEqualTo("abc123-def456");
    }

    @Test
    @DisplayName("should trim whitespace")
    void shouldTrimWhitespace() {
      CTag ctag = CTag.of("  abc123  ");
      assertThat(ctag.getValue()).isEqualTo("abc123");
    }

    @Test
    @DisplayName("should handle null")
    void shouldHandleNull() {
      CTag ctag = CTag.of(null);
      assertThat(ctag.getValue()).isNull();
    }

    @Test
    @DisplayName("should convert blank to null")
    void shouldConvertBlankToNull() {
      CTag ctag = CTag.of("   ");
      assertThat(ctag.getValue()).isNull();
    }

    @Test
    @DisplayName("should accept UUID format")
    void shouldAcceptUuid() {
      String uuid = "550e8400-e29b-41d4-a716-446655440000";
      CTag ctag = CTag.of(uuid);
      assertThat(ctag.getValue()).isEqualTo(uuid);
    }
  }

  @Nested
  @DisplayName("ofNullable()")
  class OfNullableTests {

    @Test
    @DisplayName("should return null for null input")
    void shouldReturnNullForNull() {
      assertThat(CTag.ofNullable(null)).isNull();
    }

    @Test
    @DisplayName("should return null for blank input")
    void shouldReturnNullForBlank() {
      assertThat(CTag.ofNullable("   ")).isNull();
    }

    @Test
    @DisplayName("should create for valid input")
    void shouldCreateForValid() {
      CTag ctag = CTag.ofNullable("abc123");
      assertThat(ctag).isNotNull();
      assertThat(ctag.getValue()).isEqualTo("abc123");
    }
  }

  @Nested
  @DisplayName("equality")
  class EqualityTests {

    @Test
    @DisplayName("should be equal for same value")
    void shouldBeEqualForSameValue() {
      CTag ctag1 = CTag.of("abc123");
      CTag ctag2 = CTag.of("abc123");
      assertThat(ctag1).isEqualTo(ctag2);
    }

    @Test
    @DisplayName("should not be equal for different values")
    void shouldNotBeEqualForDifferentValues() {
      CTag ctag1 = CTag.of("abc123");
      CTag ctag2 = CTag.of("def456");
      assertThat(ctag1).isNotEqualTo(ctag2);
    }
  }
}
