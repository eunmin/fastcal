package com.fastcal.domain.model.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CalendarColor")
class CalendarColorTest {

  @Nested
  @DisplayName("hex color format")
  class HexColorTests {

    @Test
    @DisplayName("should accept #RRGGBB format")
    void shouldAcceptRRGGBB() {
      CalendarColor color = CalendarColor.of("#FF0000");
      assertThat(color.getValue()).isEqualTo("#FF0000");
      assertThat(color.isHexColor()).isTrue();
    }

    @Test
    @DisplayName("should accept #RRGGBBAA format")
    void shouldAcceptRRGGBBAA() {
      CalendarColor color = CalendarColor.of("#FF0000FF");
      assertThat(color.getValue()).isEqualTo("#FF0000FF");
    }

    @Test
    @DisplayName("should accept #RGB short format")
    void shouldAcceptRGBShort() {
      CalendarColor color = CalendarColor.of("#F00");
      assertThat(color.getValue()).isEqualTo("#F00");
    }

    @Test
    @DisplayName("should normalize hex to uppercase")
    void shouldNormalizeToUppercase() {
      CalendarColor color = CalendarColor.of("#ff0000");
      assertThat(color.getValue()).isEqualTo("#FF0000");
    }

    @Test
    @DisplayName("should expand short hex to full format")
    void shouldExpandShortHex() {
      CalendarColor color = CalendarColor.of("#F00");
      assertThat(color.toHexColor()).isEqualTo("#FF0000");
    }
  }

  @Nested
  @DisplayName("CSS3 named colors")
  class NamedColorTests {

    @Test
    @DisplayName("should accept CSS3 basic colors")
    void shouldAcceptBasicColors() {
      CalendarColor color = CalendarColor.of("red");
      assertThat(color.getValue()).isEqualTo("red");
      assertThat(color.isNamedColor()).isTrue();
    }

    @Test
    @DisplayName("should normalize named color to lowercase")
    void shouldNormalizeToLowercase() {
      CalendarColor color = CalendarColor.of("RED");
      assertThat(color.getValue()).isEqualTo("red");
    }

    @Test
    @DisplayName("should convert named color to hex")
    void shouldConvertToHex() {
      CalendarColor color = CalendarColor.of("red");
      assertThat(color.toHexColor()).isEqualTo("#FF0000");
    }

    @Test
    @DisplayName("should accept extended CSS3 colors")
    void shouldAcceptExtendedColors() {
      CalendarColor color = CalendarColor.of("rebeccapurple");
      assertThat(color.getValue()).isEqualTo("rebeccapurple");
      assertThat(color.toHexColor()).isEqualTo("#663399");
    }
  }

  @Nested
  @DisplayName("validation")
  class ValidationTests {

    @Test
    @DisplayName("should throw on null")
    void shouldThrowOnNull() {
      assertThatThrownBy(() -> CalendarColor.of(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("cannot be null or blank");
    }

    @Test
    @DisplayName("should throw on blank")
    void shouldThrowOnBlank() {
      assertThatThrownBy(() -> CalendarColor.of("   "))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("cannot be null or blank");
    }

    @Test
    @DisplayName("should throw on invalid color")
    void shouldThrowOnInvalidColor() {
      assertThatThrownBy(() -> CalendarColor.of("notacolor"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("valid hex color");
    }

    @Test
    @DisplayName("should throw on invalid hex format")
    void shouldThrowOnInvalidHex() {
      assertThatThrownBy(() -> CalendarColor.of("#GGG"))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  @DisplayName("ofNullable()")
  class OfNullableTests {

    @Test
    @DisplayName("should return null for null input")
    void shouldReturnNullForNull() {
      assertThat(CalendarColor.ofNullable(null)).isNull();
    }

    @Test
    @DisplayName("should return null for invalid color")
    void shouldReturnNullForInvalid() {
      assertThat(CalendarColor.ofNullable("notacolor")).isNull();
    }

    @Test
    @DisplayName("should create for valid color")
    void shouldCreateForValid() {
      CalendarColor color = CalendarColor.ofNullable("#FF0000");
      assertThat(color).isNotNull();
      assertThat(color.getValue()).isEqualTo("#FF0000");
    }
  }
}
