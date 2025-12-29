package com.fastcal.xml;

import org.springframework.stereotype.Component;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

@Component
public class PropFindParser {

  private static final int MAX_ELEMENTS = 1000;
  private static final int MAX_DEPTH = 20;

  private final XMLInputFactory xmlInputFactory;

  public PropFindParser() {
    this.xmlInputFactory = XMLInputFactory.newInstance();
    xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
    xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
  }

  public List<String> parse(String xml) {
    if (xml == null || xml.isBlank()) {
      return List.of("allprop");
    }

    List<String> requestedProps = new ArrayList<>();
    boolean inProp = false;
    PropFindType propType = PropFindType.PROP;
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
            case "allprop" -> propType = PropFindType.ALLPROP;
            case "propname" -> propType = PropFindType.PROPNAME;
            case "prop" -> inProp = true;
            default -> {
              if (inProp) {
                requestedProps.add(localName);
              }
            }
          }
        } else if (event == XMLStreamConstants.END_ELEMENT) {
          currentDepth--;
          if ("prop".equals(reader.getLocalName())) {
            inProp = false;
          }
        }
      }
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
      return getDefaultProps();
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (Exception ignored) {
        }
      }
    }

    return switch (propType) {
      case ALLPROP -> getAllProps();
      case PROPNAME -> List.of("propname");
      case PROP -> requestedProps.isEmpty() ? getDefaultProps() : requestedProps;
    };
  }

  private List<String> getAllProps() {
    return List.of(
        "displayname",
        "resourcetype",
        "getcontenttype",
        "getetag",
        "getctag",
        "sync-token",
        "current-user-principal",
        "owner",
        "supported-report-set");
  }

  private List<String> getDefaultProps() {
    return List.of("displayname", "resourcetype");
  }

  private enum PropFindType {
    PROP,
    ALLPROP,
    PROPNAME
  }
}
