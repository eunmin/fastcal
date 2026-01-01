package com.fastcal.service.caldav;

import com.fastcal.domain.model.Calendar;
import com.fastcal.domain.model.CalendarEvent;
import com.fastcal.domain.model.vo.CalendarId;
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
    String userIdValue = userId.getValue();
    String href = "/principals/" + userIdValue + "/";
    Map<String, PropValue> props = new HashMap<>();

    for (String prop : requestedProps) {
      switch (prop) {
        case "displayname" -> props.put(prop, new PropValue.Text(userIdValue));
        case "resourcetype" -> props.put(prop, new PropValue.ResourceType(List.of("principal")));
        case "current-user-principal" -> props.put(prop, new PropValue.Href("/principals/" + userIdValue + "/"));
        case "principal-URL" -> props.put(prop, new PropValue.Href("/principals/" + userIdValue + "/"));
        case "calendar-home-set" -> props.put(prop, new PropValue.Href("/calendars/" + userIdValue + "/"));
        case "calendar-user-address-set" -> {
          if (userIdValue.contains("@")) {
            props.put(prop, new PropValue.HrefList(List.of("mailto:" + userIdValue)));
          } else {
            props.put(prop, new PropValue.HrefList(List.of()));
          }
        }
        case "schedule-inbox-URL" -> props.put(prop, new PropValue.Href("/calendars/" + userIdValue + "/inbox/"));
        case "schedule-outbox-URL" -> props.put(prop, new PropValue.Href("/calendars/" + userIdValue + "/outbox/"));
        case "supported-report-set" -> props.put(prop, new PropValue.SupportedReports(
            List.of("calendar-query", "calendar-multiget", "sync-collection", "free-busy-query")));
      }
    }

    return Mono.just(new DavResponse(href, props, Map.of(), null));
  }

  public Mono<List<DavResponse>> getCalendarHomeProperties(UserId userId, String depth,
      List<String> requestedProps) {
    String userIdValue = userId.getValue();
    List<DavResponse> responses = new ArrayList<>();

    String homeHref = "/calendars/" + userIdValue + "/";
    Map<String, PropValue> homeProps = new HashMap<>();

    for (String prop : requestedProps) {
      switch (prop) {
        case "displayname" -> homeProps.put(prop, new PropValue.Text("Calendar Home"));
        case "resourcetype" -> homeProps.put(prop, new PropValue.ResourceType(List.of("collection")));
        case "current-user-principal" -> homeProps.put(prop, new PropValue.Href("/principals/" + userIdValue + "/"));
        case "owner" -> homeProps.put(prop, new PropValue.Href("/principals/" + userIdValue + "/"));
        case "supported-report-set" -> homeProps.put(prop, new PropValue.SupportedReports(
            List.of("calendar-query", "calendar-multiget", "sync-collection")));
      }
    }

    responses.add(new DavResponse(homeHref, homeProps, Map.of(), null));

    if ("1".equals(depth) || "infinity".equals(depth)) {
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

  public Mono<List<DavResponse>> getCalendarProperties(UserId userId, CalendarId calendarId, String depth,
      List<String> requestedProps) {
    return calendarService.getCalendar(userId, calendarId)
        .switchIfEmpty(Mono.error(new CalendarNotFoundException("Calendar " + calendarId + " not found")))
        .flatMap(calendar -> {
          DavResponse calendarResponse = buildCalendarResponse(calendar, requestedProps, true);

          if ("1".equals(depth) || "infinity".equals(depth)) {
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
    String userIdValue = calendar.getUserId().getValue();
    String href = "/calendars/" + userIdValue + "/" + calendar.getCalendarId() + "/";
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
            new PropValue.ComponentSet(List.of("VEVENT", "VTODO")));
        case "supported-report-set" -> props.put(prop, new PropValue.SupportedReports(
            List.of("calendar-query", "calendar-multiget", "sync-collection", "free-busy-query")));
        case "current-user-principal" -> props.put(prop,
            new PropValue.Href("/principals/" + userIdValue + "/"));
        case "owner" -> props.put(prop,
            new PropValue.Href("/principals/" + userIdValue + "/"));
        case "current-user-privilege-set" -> props.put(prop, new PropValue.PrivilegeSet(
            List.of("read", "write", "write-content", "write-properties", "bind", "unbind")));
      }
    }

    return new DavResponse(href, props, Map.of(), null);
  }

  private DavResponse buildEventResponse(UserId userId, CalendarId calendarId, CalendarEvent event,
      List<String> requestedProps) {
    String uidValue = event.getUid() != null ? event.getUid().getValue() : "";
    String eventHref = "/calendars/" + userId.getValue() + "/" + calendarId.getValue() + "/" + uidValue + ".ics";
    Map<String, PropValue> eventProps = new HashMap<>();

    for (String prop : requestedProps) {
      switch (prop) {
        case "getetag" -> eventProps.put(prop, new PropValue.Text("\"" + (event.getEtag() != null ? event.getEtag().getValue() : "") + "\""));
        case "getcontenttype" -> eventProps.put(prop, new PropValue.Text("text/calendar; charset=utf-8"));
        case "resourcetype" -> eventProps.put(prop, new PropValue.ResourceType(List.of()));
        case "calendar-data" -> eventProps.put(prop, new PropValue.CalendarData(event.getIcalData() != null ? event.getIcalData().getValue() : null));
      }
    }

    return new DavResponse(eventHref, eventProps, Map.of(), null);
  }
}
