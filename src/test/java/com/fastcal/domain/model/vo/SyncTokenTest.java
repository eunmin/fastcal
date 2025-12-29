package com.fastcal.domain.model.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SyncToken")
class SyncTokenTest {

  @Nested
  @DisplayName("of()")
  class OfTests {

    @Test
    @DisplayName("should create with valid URI value")
    void shouldCreateWithValidUri() {
      SyncToken token = SyncToken.of("http://fastcal.com/ns/sync/123456");
      assertThat(token.getValue()).isEqualTo("http://fastcal.com/ns/sync/123456");
    }

    @Test
    @DisplayName("should trim whitespace")
    void shouldTrimWhitespace() {
      SyncToken token = SyncToken.of("  http://example.com/sync/123  ");
      assertThat(token.getValue()).isEqualTo("http://example.com/sync/123");
    }

    @Test
    @DisplayName("should handle null")
    void shouldHandleNull() {
      SyncToken token = SyncToken.of(null);
      assertThat(token.getValue()).isNull();
    }

    @Test
    @DisplayName("should convert blank to null")
    void shouldConvertBlankToNull() {
      SyncToken token = SyncToken.of("   ");
      assertThat(token.getValue()).isNull();
    }
  }

  @Nested
  @DisplayName("generate()")
  class GenerateTests {

    @Test
    @DisplayName("should generate token with prefix")
    void shouldGenerateWithPrefix() {
      SyncToken token = SyncToken.generate();
      assertThat(token.getValue()).startsWith("http://fastcal.com/ns/sync/");
    }

    @Test
    @DisplayName("should generate unique tokens")
    void shouldGenerateUniqueTokens() {
      SyncToken token1 = SyncToken.generate();
      SyncToken token2 = SyncToken.generate();
      assertThat(token1.getValue()).isNotEqualTo(token2.getValue());
    }
  }

  @Nested
  @DisplayName("ofNullable()")
  class OfNullableTests {

    @Test
    @DisplayName("should return null for null input")
    void shouldReturnNullForNull() {
      assertThat(SyncToken.ofNullable(null)).isNull();
    }

    @Test
    @DisplayName("should return null for blank input")
    void shouldReturnNullForBlank() {
      assertThat(SyncToken.ofNullable("   ")).isNull();
    }

    @Test
    @DisplayName("should create for valid input")
    void shouldCreateForValid() {
      SyncToken token = SyncToken.ofNullable("http://example.com/sync/123");
      assertThat(token).isNotNull();
      assertThat(token.getValue()).isEqualTo("http://example.com/sync/123");
    }
  }

  @Nested
  @DisplayName("equality")
  class EqualityTests {

    @Test
    @DisplayName("should be equal for same value")
    void shouldBeEqualForSameValue() {
      SyncToken token1 = SyncToken.of("http://example.com/sync/123");
      SyncToken token2 = SyncToken.of("http://example.com/sync/123");
      assertThat(token1).isEqualTo(token2);
    }
  }
}
