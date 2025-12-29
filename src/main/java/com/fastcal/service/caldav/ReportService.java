package com.fastcal.service.caldav;

import com.fastcal.domain.model.CalendarEvent;
import com.fastcal.domain.model.vo.CalendarId;
import com.fastcal.domain.model.vo.UserId;
import com.fastcal.exception.CalendarNotFoundException;
import com.fastcal.exception.EventNotFoundException;
import com.fastcal.handler.ReportRequest;
import com.fastcal.xml.DavResponse;
import com.fastcal.xml.PropValue;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReportService {

  private final EventService eventService;
  private final CalendarService calendarService;

  @Value("${caldav.server-info.product-id}")
  private String productId;

  public Mono<List<DavResponse>> calendarQuery(UserId userId, CalendarId calendarId, ReportRequest request) {
    Flux<CalendarEvent> events;

    if (request.filter() != null && request.filter().timeRange() != null) {
      LocalDateTime start = parseDateTime(request.filter().timeRange().start());
      LocalDateTime end = parseDateTime(request.filter().timeRange().end());
      events = eventService.getEventsByTimeRange(userId, calendarId, start, end);
    } else {
      events = eventService.getEventsByCalendar(userId, calendarId);
    }

    return events.map(event -> buildEventResponse(userId, calendarId, event, request.requestedProps()))
        .collectList();
  }

  public Mono<List<DavResponse>> calendarMultiget(UserId userId, CalendarId calendarId, ReportRequest request) {
    return Flux.fromIterable(request.hrefs())
        .flatMap(href -> {
          String eventUid = extractEventUid(href);
          return eventService.getEvent(userId, calendarId, eventUid)
              .map(event -> buildEventResponse(userId, calendarId, event, request.requestedProps()))
              .onErrorResume(EventNotFoundException.class,
                  e -> Mono.just(new DavResponse(href, Map.of(), Map.of(), 404)));
        })
        .collectList();
  }

  public Mono<List<DavResponse>> syncCollection(UserId userId, CalendarId calendarId, ReportRequest request) {
    return calendarService.getCalendar(userId, calendarId)
        .switchIfEmpty(Mono.error(new CalendarNotFoundException("Calendar " + calendarId + " not found")))
        .flatMap(calendar -> eventService.getEventsByCalendar(userId, calendarId)
            .map(event -> buildEventResponse(userId, calendarId, event, request.requestedProps()))
            .collectList());
  }

  public Mono<List<DavResponse>> freeBusyQuery(UserId userId, CalendarId calendarId, ReportRequest request) {
    if (request.filter() == null || request.filter().timeRange() == null) {
      return Mono.error(new IllegalArgumentException("time-range required for free-busy query"));
    }

    LocalDateTime start = parseDateTime(request.filter().timeRange().start());
    LocalDateTime end = parseDateTime(request.filter().timeRange().end());

    return eventService.getEventsByTimeRange(userId, calendarId, start, end)
        .collectList()
        .map(events -> {
          String freeBusyIcal = buildFreeBusyResponse(events, start, end);
          Map<String, PropValue> props = Map.of("calendar-data",
              new PropValue.CalendarData(freeBusyIcal));
          return List.of(new DavResponse("/calendars/" + userId.getValue() + "/" + calendarId.getValue() + "/",
              props, Map.of(), null));
        });
  }

  private DavResponse buildEventResponse(UserId userId, CalendarId calendarId, CalendarEvent event,
      List<String> requestedProps) {
    String uidValue = event.getUid() != null ? event.getUid().getValue() : "";
    String href = "/calendars/" + userId.getValue() + "/" + calendarId.getValue() + "/" + uidValue + ".ics";
    Map<String, PropValue> props = new HashMap<>();

    for (String prop : requestedProps) {
      switch (prop) {
        case "getetag" -> props.put(prop, new PropValue.Text("\"" + (event.getEtag() != null ? event.getEtag().getValue() : "") + "\""));
        case "getcontenttype" -> props.put(prop, new PropValue.Text("text/calendar; charset=utf-8"));
        case "calendar-data" -> props.put(prop, new PropValue.CalendarData(event.getIcalData() != null ? event.getIcalData().getValue() : null));
        case "resourcetype" -> props.put(prop, new PropValue.ResourceType(List.of()));
      }
    }

    return new DavResponse(href, props, Map.of(), null);
  }

  private String extractEventUid(String href) {
    String[] parts = href.split("/");
    String filename = parts[parts.length - 1];
    return filename.replace(".ics", "");
  }

  private LocalDateTime parseDateTime(String isoString) {
    if (isoString == null) {
      return LocalDateTime.MIN;
    }
    try {
      if (isoString.contains("T") && !isoString.contains("-")) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
        return LocalDateTime.parse(isoString, formatter);
      }
      return LocalDateTime.parse(isoString);
    } catch (java.time.format.DateTimeParseException e) {
      throw new IllegalArgumentException("Invalid date-time format: " + isoString);
    }
  }

  private String buildFreeBusyResponse(List<CalendarEvent> events, LocalDateTime start, LocalDateTime end) {
    StringBuilder busyPeriods = new StringBuilder();
    for (CalendarEvent event : events) {
      if (event.getDtstart() != null && event.getDtend() != null) {
        busyPeriods.append("FREEBUSY:")
            .append(formatDateTime(event.getDtstart()))
            .append("/")
            .append(formatDateTime(event.getDtend()))
            .append("\n");
      }
    }

    return """
        BEGIN:VCALENDAR
        VERSION:2.0
        PRODID:%s
        METHOD:REPLY
        BEGIN:VFREEBUSY
        DTSTART:%s
        DTEND:%s
        %sEND:VFREEBUSY
        END:VCALENDAR
        """.formatted(productId, formatDateTime(start), formatDateTime(end), busyPeriods);
  }

  private String formatDateTime(LocalDateTime dt) {
    return dt.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"));
  }
}
