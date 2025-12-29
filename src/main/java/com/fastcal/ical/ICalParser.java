package com.fastcal.ical;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.DtEnd;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.RRule;
import org.springframework.stereotype.Component;

import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class ICalParser {

  private final CalendarBuilder calendarBuilder = new CalendarBuilder();

  public ParsedEvent parse(String icalData) {
    try {
      Calendar calendar = calendarBuilder.build(new StringReader(icalData));
      VEvent vEvent = calendar.getComponent(net.fortuna.ical4j.model.Component.VEVENT);

      if (vEvent == null) {
        throw new IllegalArgumentException("No VEVENT found in calendar data");
      }

      DtStart dtStart = vEvent.getProperty(Property.DTSTART);
      DtEnd dtEnd = vEvent.getProperty(Property.DTEND);

      LocalDateTime startDateTime = null;
      boolean allDay = false;
      if (dtStart != null) {
        var result = parseDateTimeProperty(dtStart);
        startDateTime = result.dateTime();
        allDay = result.allDay();
      }

      LocalDateTime endDateTime = null;
      if (dtEnd != null) {
        endDateTime = parseDateTimeProperty(dtEnd).dateTime();
      }

      String uid = vEvent.getUid() != null ? vEvent.getUid().getValue() : null;
      String summary = vEvent.getSummary() != null ? vEvent.getSummary().getValue() : null;
      String description = vEvent.getDescription() != null ? vEvent.getDescription().getValue() : null;
      String location = vEvent.getLocation() != null ? vEvent.getLocation().getValue() : null;

      RRule rrule = vEvent.getProperty(Property.RRULE);
      String rruleValue = rrule != null ? rrule.getValue() : null;

      return new ParsedEvent(uid, summary, description, location,
          startDateTime, endDateTime, allDay, rruleValue);

    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to parse iCalendar data: " + e.getMessage(), e);
    }
  }

  public ValidationResult validate(String icalData) {
    try {
      Calendar calendar = calendarBuilder.build(new StringReader(icalData));

      VEvent vEvent = calendar.getComponent(net.fortuna.ical4j.model.Component.VEVENT);
      if (vEvent == null) {
        return new ValidationResult(false, "No VEVENT component found");
      }

      if (vEvent.getUid() == null) {
        return new ValidationResult(false, "UID property is required");
      }

      if (vEvent.getProperty(Property.DTSTART) == null) {
        return new ValidationResult(false, "DTSTART property is required");
      }

      return new ValidationResult(true, null);

    } catch (Exception e) {
      return new ValidationResult(false, "Failed to parse iCalendar data: " + e.getMessage());
    }
  }

  private DateTimeResult parseDateTimeProperty(net.fortuna.ical4j.model.Property property) {
    if (property instanceof DtStart dtStart) {
      var date = dtStart.getDate();
      boolean isDateOnly = dtStart.getValue() != null && dtStart.getValue().length() == 8;

      if (isDateOnly) {
        LocalDateTime localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().atStartOfDay();
        return new DateTimeResult(localDate, true);
      } else {
        return new DateTimeResult(date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(), false);
      }
    } else if (property instanceof DtEnd dtEnd) {
      var date = dtEnd.getDate();
      return new DateTimeResult(date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(), false);
    }

    return new DateTimeResult(null, false);
  }

  private record DateTimeResult(LocalDateTime dateTime, boolean allDay) {
  }
}
