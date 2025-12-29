package com.fastcal.xml;

import org.springframework.stereotype.Component;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

@Component
public class DavXmlBuilder {

  private static final String DAV_NS = "DAV:";
  private static final String CALDAV_NS = "urn:ietf:params:xml:ns:caldav";
  private static final String APPLE_NS = "http://apple.com/ns/ical/";
  private static final String CS_NS = "http://calendarserver.org/ns/";

  private final XMLOutputFactory xmlOutputFactory;

  public DavXmlBuilder() {
    this.xmlOutputFactory = XMLOutputFactory.newInstance();
  }

  public String buildMultiStatus(List<DavResponse> responses) {
    StringWriter writer = new StringWriter();

    try {
      XMLStreamWriter xml = xmlOutputFactory.createXMLStreamWriter(writer);

      xml.writeStartDocument("UTF-8", "1.0");
      xml.writeStartElement("D", "multistatus", DAV_NS);
      xml.writeNamespace("D", DAV_NS);
      xml.writeNamespace("C", CALDAV_NS);
      xml.writeNamespace("A", APPLE_NS);
      xml.writeNamespace("CS", CS_NS);

      for (DavResponse response : responses) {
        writeResponse(xml, response);
      }

      xml.writeEndElement();
      xml.writeEndDocument();
      xml.close();

    } catch (XMLStreamException e) {
      throw new RuntimeException("Failed to build XML response", e);
    }

    return writer.toString();
  }

  private void writeResponse(XMLStreamWriter xml, DavResponse response) throws XMLStreamException {
    xml.writeStartElement(DAV_NS, "response");

    xml.writeStartElement(DAV_NS, "href");
    xml.writeCharacters(response.href());
    xml.writeEndElement();

    if (!response.props().isEmpty()) {
      xml.writeStartElement(DAV_NS, "propstat");

      xml.writeStartElement(DAV_NS, "prop");
      for (Map.Entry<String, PropValue> entry : response.props().entrySet()) {
        writePropValue(xml, entry.getKey(), entry.getValue());
      }
      xml.writeEndElement();

      xml.writeStartElement(DAV_NS, "status");
      int status = response.status() != null ? response.status() : 200;
      xml.writeCharacters("HTTP/1.1 " + status + " OK");
      xml.writeEndElement();

      xml.writeEndElement();
    }

    if (!response.notFoundProps().isEmpty()) {
      xml.writeStartElement(DAV_NS, "propstat");

      xml.writeStartElement(DAV_NS, "prop");
      for (String name : response.notFoundProps().keySet()) {
        xml.writeEmptyElement(DAV_NS, name);
      }
      xml.writeEndElement();

      xml.writeStartElement(DAV_NS, "status");
      xml.writeCharacters("HTTP/1.1 404 Not Found");
      xml.writeEndElement();

      xml.writeEndElement();
    }

    xml.writeEndElement();
  }

  private void writePropValue(XMLStreamWriter xml, String name, PropValue value) throws XMLStreamException {
    String ns = getNamespace(name);

    if (value instanceof PropValue.Text text) {
      xml.writeStartElement(ns, name);
      xml.writeCharacters(text.text());
      xml.writeEndElement();
    } else if (value instanceof PropValue.Href href) {
      xml.writeStartElement(ns, name);
      xml.writeStartElement(DAV_NS, "href");
      xml.writeCharacters(href.href());
      xml.writeEndElement();
      xml.writeEndElement();
    } else if (value instanceof PropValue.HrefList hrefList) {
      xml.writeStartElement(ns, name);
      for (String h : hrefList.hrefs()) {
        xml.writeStartElement(DAV_NS, "href");
        xml.writeCharacters(h);
        xml.writeEndElement();
      }
      xml.writeEndElement();
    } else if (value instanceof PropValue.ResourceType resourceType) {
      xml.writeStartElement(ns, name);
      for (String type : resourceType.types()) {
        if ("collection".equals(type)) {
          xml.writeEmptyElement(DAV_NS, "collection");
        } else if ("calendar".equals(type)) {
          xml.writeEmptyElement(CALDAV_NS, "calendar");
        } else if ("principal".equals(type)) {
          xml.writeEmptyElement(DAV_NS, "principal");
        }
      }
      xml.writeEndElement();
    } else if (value instanceof PropValue.SupportedReports reports) {
      xml.writeStartElement(DAV_NS, "supported-report-set");
      for (String report : reports.reports()) {
        xml.writeStartElement(DAV_NS, "supported-report");
        xml.writeStartElement(DAV_NS, "report");
        if ("calendar-query".equals(report)) {
          xml.writeEmptyElement(CALDAV_NS, "calendar-query");
        } else if ("calendar-multiget".equals(report)) {
          xml.writeEmptyElement(CALDAV_NS, "calendar-multiget");
        } else if ("sync-collection".equals(report)) {
          xml.writeEmptyElement(DAV_NS, "sync-collection");
        } else if ("free-busy-query".equals(report)) {
          xml.writeEmptyElement(CALDAV_NS, "free-busy-query");
        }
        xml.writeEndElement();
        xml.writeEndElement();
      }
      xml.writeEndElement();
    } else if (value instanceof PropValue.ComponentSet componentSet) {
      xml.writeStartElement(CALDAV_NS, "supported-calendar-component-set");
      for (String comp : componentSet.components()) {
        xml.writeEmptyElement(CALDAV_NS, "comp");
        xml.writeAttribute("name", comp);
      }
      xml.writeEndElement();
    } else if (value instanceof PropValue.PrivilegeSet privilegeSet) {
      xml.writeStartElement(DAV_NS, "current-user-privilege-set");
      for (String priv : privilegeSet.privileges()) {
        xml.writeStartElement(DAV_NS, "privilege");
        xml.writeEmptyElement(DAV_NS, priv);
        xml.writeEndElement();
      }
      xml.writeEndElement();
    } else if (value instanceof PropValue.CalendarData calendarData) {
      xml.writeStartElement(CALDAV_NS, "calendar-data");
      xml.writeCData(calendarData.icalData());
      xml.writeEndElement();
    }
  }

  private String getNamespace(String propName) {
    return switch (propName) {
      case "calendar-home-set", "calendar-user-address-set", "schedule-inbox-URL",
          "schedule-outbox-URL", "calendar-description", "calendar-timezone",
          "supported-calendar-component-set", "calendar-data" ->
        CALDAV_NS;
      case "calendar-color" -> APPLE_NS;
      case "getctag" -> CS_NS;
      default -> DAV_NS;
    };
  }
}
