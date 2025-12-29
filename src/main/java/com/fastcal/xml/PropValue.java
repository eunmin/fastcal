package com.fastcal.xml;

import java.util.List;

public sealed interface PropValue {

  record Text(String text) implements PropValue {
  }

  record Href(String href) implements PropValue {
  }

  record HrefList(List<String> hrefs) implements PropValue {
  }

  record ResourceType(List<String> types) implements PropValue {
  }

  record SupportedReports(List<String> reports) implements PropValue {
  }

  record ComponentSet(List<String> components) implements PropValue {
  }

  record PrivilegeSet(List<String> privileges) implements PropValue {
  }

  record CalendarData(String icalData) implements PropValue {
  }
}
