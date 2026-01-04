package com.fastcal.service.caldav;

import com.fastcal.domain.model.CalendarEvent;
import com.fastcal.domain.model.vo.CalDavCapabilities;
import com.fastcal.domain.model.vo.CalDavPath;
import com.fastcal.domain.model.vo.CalendarId;
import com.fastcal.domain.model.vo.ETag;
import com.fastcal.domain.model.vo.FreeBusyResponse;
import com.fastcal.domain.model.vo.ICalDateTime;
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
      LocalDateTime start = ICalDateTime.parse(request.filter().timeRange().start()).getValue();
      LocalDateTime end = ICalDateTime.parse(request.filter().timeRange().end()).getValue();
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
          String eventUid = CalDavPath.extractEventUid(href);
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

    LocalDateTime start = ICalDateTime.parse(request.filter().timeRange().start()).getValue();
    LocalDateTime end = ICalDateTime.parse(request.filter().timeRange().end()).getValue();

    return eventService.getEventsByTimeRange(userId, calendarId, start, end)
        .collectList()
        .map(events -> {
          List<FreeBusyResponse.BusyPeriod> busyPeriods = events.stream()
              .filter(e -> e.getDtstart() != null && e.getDtend() != null)
              .map(e -> new FreeBusyResponse.BusyPeriod(e.getDtstart(), e.getDtend()))
              .toList();
          FreeBusyResponse freeBusy = FreeBusyResponse.of(productId, start, end, busyPeriods);
          Map<String, PropValue> props = Map.of("calendar-data",
              new PropValue.CalendarData(freeBusy.getValue()));
          return List.of(new DavResponse(CalDavPath.calendar(userId, calendarId),
              props, Map.of(), null));
        });
  }

  private DavResponse buildEventResponse(UserId userId, CalendarId calendarId, CalendarEvent event,
      List<String> requestedProps) {
    String uidValue = event.getUid() != null ? event.getUid().getValue() : "";
    String href = CalDavPath.event(userId, calendarId, uidValue);
    Map<String, PropValue> props = new HashMap<>();

    for (String prop : requestedProps) {
      switch (prop) {
        case "getetag" -> props.put(prop, new PropValue.Text(event.getEtag() != null ? event.getEtag().toHttpFormat() : ETag.EMPTY_HTTP_FORMAT));
        case "getcontenttype" -> props.put(prop, new PropValue.Text(CalDavCapabilities.CALENDAR_CONTENT_TYPE));
        case "calendar-data" -> props.put(prop, new PropValue.CalendarData(event.getIcalData() != null ? event.getIcalData().getValue() : null));
        case "resourcetype" -> props.put(prop, new PropValue.ResourceType(List.of()));
      }
    }

    return new DavResponse(href, props, Map.of(), null);
  }

}
