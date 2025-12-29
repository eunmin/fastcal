package com.fastcal.domain.model.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ETag")
class ETagTest {

  @Nested
  @DisplayName("생성")
  class Creation {

    @Test
    @DisplayName("유효한 ETag로 생성할 수 있다")
    void shouldCreateWithValidETag() {
      ETag etag = ETag.of("abc123def456");

      assertThat(etag.getValue()).isEqualTo("abc123def456");
    }

    @Test
    @DisplayName("따옴표로 감싸진 ETag는 따옴표가 제거된다")
    void shouldRemoveSurroundingQuotes() {
      ETag etag = ETag.of("\"abc123\"");

      assertThat(etag.getValue()).isEqualTo("abc123");
    }

    @Test
    @DisplayName("of 메서드로 생성할 수 있다")
    void shouldCreateWithOfMethod() {
      ETag etag = ETag.of("test-etag");

      assertThat(etag).isNotNull();
      assertThat(etag.getValue()).isEqualTo("test-etag");
    }

    @Test
    @DisplayName("ofNullable은 null 입력시 null을 반환한다")
    void shouldReturnNullForNullInput() {
      ETag etag = ETag.ofNullable(null);

      assertThat(etag).isNull();
    }

    @Test
    @DisplayName("ofNullable은 빈 문자열 입력시 null을 반환한다")
    void shouldReturnNullForBlankInput() {
      ETag etag = ETag.ofNullable("   ");

      assertThat(etag).isNull();
    }

    @Test
    @DisplayName("앞뒤 공백이 제거된다")
    void shouldTrimWhitespace() {
      ETag etag = ETag.of("  abc123  ");

      assertThat(etag.getValue()).isEqualTo("abc123");
    }
  }

  @Nested
  @DisplayName("fromContent")
  class FromContent {

    @Test
    @DisplayName("콘텐츠로부터 ETag를 생성할 수 있다")
    void shouldGenerateFromContent() {
      ETag etag = ETag.fromContent("test content");

      assertThat(etag.getValue()).isNotBlank();
      assertThat(etag.getValue()).hasSize(32); // MD5 hex length
    }

    @Test
    @DisplayName("같은 콘텐츠는 같은 ETag를 생성한다")
    void shouldGenerateSameETagForSameContent() {
      ETag etag1 = ETag.fromContent("test content");
      ETag etag2 = ETag.fromContent("test content");

      assertThat(etag1).isEqualTo(etag2);
    }

    @Test
    @DisplayName("다른 콘텐츠는 다른 ETag를 생성한다")
    void shouldGenerateDifferentETagForDifferentContent() {
      ETag etag1 = ETag.fromContent("content 1");
      ETag etag2 = ETag.fromContent("content 2");

      assertThat(etag1).isNotEqualTo(etag2);
    }

    @Test
    @DisplayName("null 콘텐츠는 예외를 발생시킨다")
    void shouldRejectNullContent() {
      assertThatThrownBy(() -> ETag.fromContent(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Content cannot be null");
    }
  }

  @Nested
  @DisplayName("유효성 검증")
  class Validation {

    @Test
    @DisplayName("null 값은 허용되지 않는다")
    void shouldRejectNullValue() {
      assertThatThrownBy(() -> ETag.of(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("cannot be null or blank");
    }

    @Test
    @DisplayName("빈 문자열은 허용되지 않는다")
    void shouldRejectEmptyValue() {
      assertThatThrownBy(() -> ETag.of(""))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("cannot be null or blank");
    }

    @Test
    @DisplayName("128자를 초과하면 예외가 발생한다")
    void shouldRejectExceedingMaxLength() {
      String longETag = "a".repeat(129);

      assertThatThrownBy(() -> ETag.of(longETag))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("cannot exceed 128 characters");
    }

    @Test
    @DisplayName("128자는 허용된다")
    void shouldAcceptMaxLength() {
      String maxLengthETag = "a".repeat(128);

      ETag etag = ETag.of(maxLengthETag);

      assertThat(etag.getValue()).hasSize(128);
    }

    @Test
    @DisplayName("값 내부에 따옴표가 있으면 예외가 발생한다")
    void shouldRejectInternalQuotes() {
      assertThatThrownBy(() -> ETag.of("abc\"123"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("cannot contain double quotes");
    }

    @Test
    @DisplayName("따옴표만 있으면 예외가 발생한다")
    void shouldRejectOnlyQuotes() {
      assertThatThrownBy(() -> ETag.of("\"\""))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("cannot be empty after trimming");
    }
  }

  @Nested
  @DisplayName("toHttpFormat")
  class ToHttpFormat {

    @Test
    @DisplayName("HTTP 헤더 형식으로 변환한다")
    void shouldConvertToHttpFormat() {
      ETag etag = ETag.of("abc123");

      assertThat(etag.toHttpFormat()).isEqualTo("\"abc123\"");
    }

    @Test
    @DisplayName("이미 따옴표가 제거된 ETag도 올바르게 변환한다")
    void shouldConvertEvenAfterQuotesRemoved() {
      ETag etag = ETag.of("\"abc123\"");

      assertThat(etag.toHttpFormat()).isEqualTo("\"abc123\"");
    }
  }

  @Nested
  @DisplayName("동등성")
  class Equality {

    @Test
    @DisplayName("같은 값을 가진 ETag는 동등하다")
    void shouldBeEqualForSameValue() {
      ETag etag1 = ETag.of("abc123");
      ETag etag2 = ETag.of("abc123");

      assertThat(etag1).isEqualTo(etag2);
      assertThat(etag1.hashCode()).isEqualTo(etag2.hashCode());
    }

    @Test
    @DisplayName("다른 값을 가진 ETag는 동등하지 않다")
    void shouldNotBeEqualForDifferentValue() {
      ETag etag1 = ETag.of("abc123");
      ETag etag2 = ETag.of("def456");

      assertThat(etag1).isNotEqualTo(etag2);
    }

    @Test
    @DisplayName("따옴표 유무와 관계없이 같은 값은 동등하다")
    void shouldBeEqualRegardlessOfQuotes() {
      ETag etag1 = ETag.of("abc123");
      ETag etag2 = ETag.of("\"abc123\"");

      assertThat(etag1).isEqualTo(etag2);
    }
  }

  @Nested
  @DisplayName("toString")
  class ToStringTest {

    @Test
    @DisplayName("toString은 값을 반환한다")
    void shouldReturnValue() {
      ETag etag = ETag.of("abc123");

      assertThat(etag.toString()).isEqualTo("abc123");
    }
  }
}
