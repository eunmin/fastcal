package com.fastcal.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ETagUtil {

  private ETagUtil() {
  }

  private static final Pattern ETAG_PATTERN = Pattern.compile("(W/)?\"([^\"]*)\"");

  public static String normalize(String etag) {
    if (etag == null || etag.isEmpty()) {
      return null;
    }

    String trimmed = etag.trim();

    if ("*".equals(trimmed)) {
      return "*";
    }

    Matcher matcher = ETAG_PATTERN.matcher(trimmed);
    if (matcher.matches()) {
      return matcher.group(2);
    }

    return trimmed.replace("\"", "");
  }

  private static List<String> parseMultiple(String etagHeader) {
    List<String> etags = new ArrayList<>();

    if (etagHeader == null || etagHeader.isEmpty()) {
      return etags;
    }

    if ("*".equals(etagHeader.trim())) {
      etags.add("*");
      return etags;
    }

    Matcher matcher = ETAG_PATTERN.matcher(etagHeader);
    while (matcher.find()) {
      etags.add(matcher.group(2));
    }

    return etags;
  }

  public static boolean matches(String ifMatchHeader, String currentEtag) {
    if (ifMatchHeader == null || ifMatchHeader.isEmpty()) {
      return true;
    }

    if ("*".equals(ifMatchHeader.trim())) {
      return currentEtag != null && !currentEtag.isEmpty();
    }

    List<String> etags = parseMultiple(ifMatchHeader);
    String normalizedCurrent = normalize(currentEtag);

    for (String etag : etags) {
      if (etag.equals(normalizedCurrent)) {
        return true;
      }
    }

    return false;
  }
}
