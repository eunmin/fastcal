package com.fastcal.xml;

import org.springframework.stereotype.Component;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;

@Component
public class MkCalendarParser {

  private static final int MAX_ELEMENTS = 1000;
  private static final int MAX_DEPTH = 20;

  private final XMLInputFactory xmlInputFactory;

  public MkCalendarParser() {
    this.xmlInputFactory = XMLInputFactory.newInstance();
    xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
    xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
  }

  public ParsedCalendarProperties parse(String xml) {
    if (xml == null || xml.isEmpty() || xml.isBlank()) {
      return new ParsedCalendarProperties(null, null, null, null);
    }

    String displayName = null;
    String description = null;
    String color = null;
    String timezone = null;
    int elementCount = 0;
    int currentDepth = 0;

    XMLStreamReader reader = null;
    try {
      reader = xmlInputFactory.createXMLStreamReader(new StringReader(xml));

      while (reader.hasNext()) {
        int event = reader.next();
        if (event == XMLStreamConstants.START_ELEMENT) {
          elementCount++;
          currentDepth++;

          if (elementCount > MAX_ELEMENTS) {
            throw new IllegalArgumentException("XML exceeds maximum element count");
          }
          if (currentDepth > MAX_DEPTH) {
            throw new IllegalArgumentException("XML exceeds maximum depth");
          }

          String localName = reader.getLocalName();

          switch (localName) {
            case "displayname" -> displayName = readElementText(reader);
            case "calendar-description" -> description = readElementText(reader);
            case "calendar-color" -> color = readElementText(reader);
            case "calendar-timezone" -> timezone = readElementText(reader);
          }
        } else if (event == XMLStreamConstants.END_ELEMENT) {
          currentDepth--;
        }
      }
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (Exception ignored) {
        }
      }
    }

    return new ParsedCalendarProperties(displayName, description, color, timezone);
  }

  private String readElementText(XMLStreamReader reader) throws Exception {
    StringBuilder text = new StringBuilder();
    while (reader.hasNext()) {
      int event = reader.next();
      if (event == XMLStreamConstants.CHARACTERS || event == XMLStreamConstants.CDATA) {
        text.append(reader.getText());
      } else if (event == XMLStreamConstants.END_ELEMENT) {
        break;
      }
    }
    String result = text.toString().trim();
    return result.isEmpty() ? null : result;
  }

  public record ParsedCalendarProperties(
      String displayName,
      String description,
      String color,
      String timezone) {
  }
}
