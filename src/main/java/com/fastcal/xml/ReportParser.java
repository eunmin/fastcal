package com.fastcal.xml;

import com.fastcal.handler.*;
import org.springframework.stereotype.Component;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

@Component
public class ReportParser {

  private static final int MAX_ELEMENTS = 1000;
  private static final int MAX_DEPTH = 20;

  private final XMLInputFactory xmlInputFactory;

  public ReportParser() {
    this.xmlInputFactory = XMLInputFactory.newInstance();
    xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
    xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
  }

  public ReportRequest parse(String xml) {
    ReportType reportType = null;
    List<String> requestedProps = new ArrayList<>();
    List<String> hrefs = new ArrayList<>();
    String syncToken = null;
    boolean inProp = false;
    boolean inHref = false;
    String timeRangeStart = null;
    String timeRangeEnd = null;
    String componentType = "VCALENDAR";
    int elementCount = 0;
    int currentDepth = 0;

    try {
      XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(new StringReader(xml));
      StringBuilder hrefContent = new StringBuilder();

      while (reader.hasNext()) {
        int event = reader.next();

        if (event == XMLStreamConstants.START_ELEMENT) {
          elementCount++;
          currentDepth++;

          if (elementCount > MAX_ELEMENTS) {
            reader.close();
            throw new IllegalArgumentException("XML exceeds maximum element count");
          }
          if (currentDepth > MAX_DEPTH) {
            reader.close();
            throw new IllegalArgumentException("XML exceeds maximum depth");
          }

          String localName = reader.getLocalName();

          switch (localName) {
            case "calendar-query" -> reportType = ReportType.CALENDAR_QUERY;
            case "calendar-multiget" -> reportType = ReportType.CALENDAR_MULTIGET;
            case "sync-collection" -> reportType = ReportType.SYNC_COLLECTION;
            case "free-busy-query" -> reportType = ReportType.FREE_BUSY_QUERY;
            case "prop" -> inProp = true;
            case "href" -> {
              inHref = true;
              hrefContent.setLength(0);
            }
            case "comp-filter" -> {
              String name = reader.getAttributeValue(null, "name");
              if (name != null) {
                componentType = name;
              }
            }
            case "time-range" -> {
              timeRangeStart = reader.getAttributeValue(null, "start");
              timeRangeEnd = reader.getAttributeValue(null, "end");
            }
            default -> {
              if (inProp) {
                requestedProps.add(localName);
              }
            }
          }
        } else if (event == XMLStreamConstants.CHARACTERS) {
          if (inHref) {
            hrefContent.append(reader.getText().trim());
          }
        } else if (event == XMLStreamConstants.END_ELEMENT) {
          currentDepth--;
          String localName = reader.getLocalName();
          switch (localName) {
            case "prop" -> inProp = false;
            case "href" -> {
              if (!hrefContent.isEmpty()) {
                hrefs.add(hrefContent.toString());
              }
              inHref = false;
            }
          }
        }
      }

      reader.close();
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to parse REPORT request: " + e.getMessage());
    }

    CalendarFilter filter = null;
    if (timeRangeStart != null || timeRangeEnd != null) {
      filter = new CalendarFilter(
          componentType,
          new TimeRange(timeRangeStart, timeRangeEnd),
          List.of());
    }

    if (requestedProps.isEmpty()) {
      requestedProps = List.of("getetag", "calendar-data");
    }

    return new ReportRequest(
        reportType != null ? reportType : ReportType.CALENDAR_QUERY,
        requestedProps,
        hrefs,
        filter,
        syncToken);
  }
}
