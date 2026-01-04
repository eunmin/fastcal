package com.fastcal.service.caldav;

import com.fastcal.domain.model.Calendar;
import com.fastcal.domain.model.CalendarEvent;
import com.fastcal.domain.model.vo.CalDavCapabilities;
import com.fastcal.domain.model.vo.CalDavDepth;
import com.fastcal.domain.model.vo.CalDavPath;
import com.fastcal.domain.model.vo.CalendarId;
import com.fastcal.domain.model.vo.ETag;
import com.fastcal.domain.model.vo.UserId;
import com.fastcal.exception.CalendarNotFoundException;
import com.fastcal.xml.DavResponse;
import com.fastcal.xml.PropValue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PropFindService {

  private final CalendarService calendarService;
  private final EventService eventService;

  public Mono<DavResponse> getPrincipalProperties(UserId userId, List<String> requestedProps) {
    String href = CalDavPath.principal(userId);
    Map<String, PropValue> props = new HashMap<>();

    for (String prop : requestedProps) {
      switch (prop) {
        case "displayname" -> props.put(prop, new PropValue.Text(userId.getValue()));
        case "resourcetype" -> props.put(prop, new PropValue.ResourceType(List.of("principal")));
        case "current-user-principal" -> props.put(prop, new PropValue.Href(CalDavPath.principal(userId)));
        case "principal-URL" -> props.put(prop, new PropValue.Href(CalDavPath.principal(userId)));
        case "calendar-home-set" -> props.put(prop, new PropValue.Href(CalDavPath.calendarHome(userId)));
        case "schedule-inbox-URL" -> props.put(prop, new PropValue.Href(CalDavPath.scheduleInbox(userId)));
        case "schedule-outbox-URL" -> props.put(prop, new PropValue.Href(CalDavPath.scheduleOutbox(userId)));
        case "supported-report-set" -> props.put(prop, new PropValue.SupportedReports(CalDavCapabilities.SUPPORTED_REPORTS));
      }
    }

    return Mono.just(new DavResponse(href, props, Map.of(), null));
  }

  public Mono<List<DavResponse>> getCalendarHomeProperties(UserId userId, CalDavDepth depth,
      List<String> requestedProps) {
    List<DavResponse> responses = new ArrayList<>();

    String homeHref = CalDavPath.calendarHome(userId);
    Map<String, PropValue> homeProps = new HashMap<>();

    for (String prop : requestedProps) {
      switch (prop) {
        case "displayname" -> homeProps.put(prop, new PropValue.Text("Calendar Home"));
        case "resourcetype" -> homeProps.put(prop, new PropValue.ResourceType(List.of("collection")));
        case "current-user-principal" -> homeProps.put(prop, new PropValue.Href(CalDavPath.principal(userId)));
        case "owner" -> homeProps.put(prop, new PropValue.Href(CalDavPath.principal(userId)));
        case "supported-report-set" -> homeProps.put(prop, new PropValue.SupportedReports(CalDavCapabilities.CALENDAR_HOME_REPORTS));
      }
    }

    responses.add(new DavResponse(homeHref, homeProps, Map.of(), null));

    if (depth.includesChildren()) {
      return calendarService.getCalendarsByUser(userId)
          .map(calendar -> buildCalendarResponse(calendar, requestedProps, false))
          .collectList()
          .map(calendarResponses -> {
            responses.addAll(calendarResponses);
            return responses;
          });
    }

    return Mono.just(responses);
  }

  public Mono<List<DavResponse>> getCalendarProperties(UserId userId, CalendarId calendarId, CalDavDepth depth,
      List<String> requestedProps) {
    return calendarService.getCalendar(userId, calendarId)
        .switchIfEmpty(Mono.error(new CalendarNotFoundException("Calendar " + calendarId + " not found")))
        .flatMap(calendar -> {
          DavResponse calendarResponse = buildCalendarResponse(calendar, requestedProps, true);

          if (depth.includesChildren()) {
            return eventService.getEventsByCalendar(userId, calendarId)
                .map(event -> buildEventResponse(userId, calendarId, event, requestedProps))
                .buffer(100)
                .reduce(new ArrayList<DavResponse>(), (list, batch) -> {
                  if (list.isEmpty()) {
                    list.add(calendarResponse);
                  }
                  list.addAll(batch);
                  return list;
                })
                .defaultIfEmpty(new ArrayList<>(List.of(calendarResponse)));
          }

          return Mono.just(new ArrayList<>(List.of(calendarResponse)));
        });
  }

  private DavResponse buildCalendarResponse(Calendar calendar, List<String> requestedProps,
      boolean includeSyncToken) {
    UserId userId = calendar.getUserId();
    String href = CalDavPath.calendar(userId, calendar.getCalendarId());
    Map<String, PropValue> props = new HashMap<>();

    for (String prop : requestedProps) {
      switch (prop) {
        case "displayname" -> props.put(prop, new PropValue.Text(
            calendar.getDisplayName() != null ? calendar.getDisplayName().getValue() : null));
        case "resourcetype" -> props.put(prop,
            new PropValue.ResourceType(List.of("collection", "calendar")));
        case "getctag" -> props.put(prop, new PropValue.Text(
            calendar.getCtag() != null ? calendar.getCtag().getValue() : null));
        case "sync-token" -> {
          if (includeSyncToken)
            props.put(prop, new PropValue.Text(
                calendar.getSyncToken() != null ? calendar.getSyncToken().getValue() : null));
        }
        case "calendar-description" -> {
          if (calendar.getDescription() != null)
            props.put(prop, new PropValue.Text(calendar.getDescription().getValue()));
        }
        case "calendar-color" -> {
          if (calendar.getColor() != null)
            props.put(prop, new PropValue.Text(calendar.getColor().getValue()));
        }
        case "calendar-timezone" -> {
          if (calendar.getTimezone() != null)
            props.put(prop, new PropValue.CalendarData(calendar.getTimezone().getValue()));
        }
        case "supported-calendar-component-set" -> props.put(prop,
            new PropValue.ComponentSet(CalDavCapabilities.SUPPORTED_CALENDAR_COMPONENTS));
        case "supported-report-set" -> props.put(prop,
            new PropValue.SupportedReports(CalDavCapabilities.SUPPORTED_REPORTS));
        case "current-user-principal" -> props.put(prop,
            new PropValue.Href(CalDavPath.principal(userId)));
        case "owner" -> props.put(prop,
            new PropValue.Href(CalDavPath.principal(userId)));
        case "current-user-privilege-set" -> props.put(prop,
            new PropValue.PrivilegeSet(CalDavCapabilities.PRIVILEGES));
      }
    }

    return new DavResponse(href, props, Map.of(), null);
  }

  private DavResponse buildEventResponse(UserId userId, CalendarId calendarId, CalendarEvent event,
      List<String> requestedProps) {
    String uidValue = event.getUid() != null ? event.getUid().getValue() : "";
    String eventHref = CalDavPath.event(userId, calendarId, uidValue);
    Map<String, PropValue> eventProps = new HashMap<>();

    for (String prop : requestedProps) {
      switch (prop) {
        case "getetag" -> eventProps.put(prop, new PropValue.Text(event.getEtag() != null ? event.getEtag().toHttpFormat() : ETag.EMPTY_HTTP_FORMAT));
        case "getcontenttype" -> eventProps.put(prop, new PropValue.Text(CalDavCapabilities.CALENDAR_CONTENT_TYPE));
        case "resourcetype" -> eventProps.put(prop, new PropValue.ResourceType(List.of()));
        case "calendar-data" -> eventProps.put(prop, new PropValue.CalendarData(event.getIcalData() != null ? event.getIcalData().getValue() : null));
      }
    }

    return new DavResponse(eventHref, eventProps, Map.of(), null);
  }
}
