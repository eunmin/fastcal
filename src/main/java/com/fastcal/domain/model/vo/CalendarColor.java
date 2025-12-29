package com.fastcal.domain.model.vo;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Value object representing a calendar color.
 *
 * Supports two formats based on CalDAV standards:
 * 1. Hex color format (Apple CalDAV extension): #RRGGBB or #RRGGBBAA
 * 2. CSS3 named colors (RFC 7986): case-insensitive color names
 *
 * @see <a href="https://icalendar.org/New-Properties-for-iCalendar-RFC-7986/5-9-color-property.html">RFC 7986 COLOR Property</a>
 * @see <a href="https://www.w3.org/wiki/CSS3/Color/Basic_color_keywords">CSS3 Color Keywords</a>
 */
public record CalendarColor(String value) {

  private static final Pattern HEX_COLOR_PATTERN =
      Pattern.compile("^#([A-Fa-f0-9]{3}|[A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})$");

  private static final Set<String> CSS3_COLOR_NAMES = Set.of(
      "aqua", "black", "blue", "fuchsia", "gray", "green", "lime", "maroon",
      "navy", "olive", "purple", "red", "silver", "teal", "white", "yellow",
      "aliceblue", "antiquewhite", "aquamarine", "azure", "beige", "bisque",
      "blanchedalmond", "blueviolet", "brown", "burlywood", "cadetblue",
      "chartreuse", "chocolate", "coral", "cornflowerblue", "cornsilk",
      "crimson", "cyan", "darkblue", "darkcyan", "darkgoldenrod", "darkgray",
      "darkgreen", "darkgrey", "darkkhaki", "darkmagenta", "darkolivegreen",
      "darkorange", "darkorchid", "darkred", "darksalmon", "darkseagreen",
      "darkslateblue", "darkslategray", "darkslategrey", "darkturquoise",
      "darkviolet", "deeppink", "deepskyblue", "dimgray", "dimgrey",
      "dodgerblue", "firebrick", "floralwhite", "forestgreen", "gainsboro",
      "ghostwhite", "gold", "goldenrod", "greenyellow", "grey", "honeydew",
      "hotpink", "indianred", "indigo", "ivory", "khaki", "lavender",
      "lavenderblush", "lawngreen", "lemonchiffon", "lightblue", "lightcoral",
      "lightcyan", "lightgoldenrodyellow", "lightgray", "lightgreen", "lightgrey",
      "lightpink", "lightsalmon", "lightseagreen", "lightskyblue", "lightslategray",
      "lightslategrey", "lightsteelblue", "lightyellow", "limegreen", "linen",
      "magenta", "mediumaquamarine", "mediumblue", "mediumorchid", "mediumpurple",
      "mediumseagreen", "mediumslateblue", "mediumspringgreen", "mediumturquoise",
      "mediumvioletred", "midnightblue", "mintcream", "mistyrose", "moccasin",
      "navajowhite", "oldlace", "olivedrab", "orange", "orangered", "orchid",
      "palegoldenrod", "palegreen", "paleturquoise", "palevioletred", "papayawhip",
      "peachpuff", "peru", "pink", "plum", "powderblue", "rosybrown", "royalblue",
      "saddlebrown", "salmon", "sandybrown", "seagreen", "seashell", "sienna",
      "skyblue", "slateblue", "slategray", "slategrey", "snow", "springgreen",
      "steelblue", "tan", "thistle", "tomato", "turquoise", "violet", "wheat",
      "whitesmoke", "yellowgreen",
      "rebeccapurple"
  );

  public CalendarColor {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("CalendarColor cannot be null or blank");
    }

    String trimmed = value.trim();

    if (!isValidHexColor(trimmed) && !isValidCss3ColorName(trimmed)) {
      throw new IllegalArgumentException(
          "CalendarColor must be a valid hex color (#RGB, #RRGGBB, #RRGGBBAA) or CSS3 color name");
    }

    value = normalizeColor(trimmed);
  }

  public static CalendarColor of(String value) {
    return new CalendarColor(value);
  }

  public static CalendarColor ofNullable(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return new CalendarColor(value);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  public String getValue() {
    return value;
  }

  public boolean isHexColor() {
    return value.startsWith("#");
  }

  public boolean isNamedColor() {
    return !value.startsWith("#");
  }

  public String toHexColor() {
    if (isHexColor()) {
      return expandShortHex(value);
    }
    return namedColorToHex(value);
  }

  private static boolean isValidHexColor(String color) {
    return HEX_COLOR_PATTERN.matcher(color).matches();
  }

  private static boolean isValidCss3ColorName(String color) {
    return CSS3_COLOR_NAMES.contains(color.toLowerCase());
  }

  private static String normalizeColor(String color) {
    if (color.startsWith("#")) {
      return color.toUpperCase();
    }
    return color.toLowerCase();
  }

  private String expandShortHex(String hex) {
    if (hex.length() == 4) {
      char r = hex.charAt(1);
      char g = hex.charAt(2);
      char b = hex.charAt(3);
      return String.format("#%c%c%c%c%c%c", r, r, g, g, b, b);
    }
    return hex;
  }

  private String namedColorToHex(String name) {
    return switch (name.toLowerCase()) {
      case "black" -> "#000000";
      case "white" -> "#FFFFFF";
      case "red" -> "#FF0000";
      case "green" -> "#008000";
      case "blue" -> "#0000FF";
      case "yellow" -> "#FFFF00";
      case "cyan", "aqua" -> "#00FFFF";
      case "magenta", "fuchsia" -> "#FF00FF";
      case "gray", "grey" -> "#808080";
      case "silver" -> "#C0C0C0";
      case "maroon" -> "#800000";
      case "olive" -> "#808000";
      case "lime" -> "#00FF00";
      case "teal" -> "#008080";
      case "navy" -> "#000080";
      case "purple" -> "#800080";
      case "orange" -> "#FFA500";
      case "pink" -> "#FFC0CB";
      case "turquoise" -> "#40E0D0";
      case "coral" -> "#FF7F50";
      case "salmon" -> "#FA8072";
      case "gold" -> "#FFD700";
      case "indigo" -> "#4B0082";
      case "violet" -> "#EE82EE";
      case "brown" -> "#A52A2A";
      case "crimson" -> "#DC143C";
      case "rebeccapurple" -> "#663399";
      default -> "#808080";
    };
  }

  @Override
  public String toString() {
    return value;
  }
}
