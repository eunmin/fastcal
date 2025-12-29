package com.fastcal.handler;

import com.fastcal.domain.model.vo.CalendarId;
import com.fastcal.domain.model.vo.UserId;
import com.fastcal.exception.PreconditionFailedException;
import com.fastcal.service.caldav.CalendarService;
import com.fastcal.service.caldav.EventService;
import com.fastcal.util.InputValidator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.List;

@Component
public class CalDavHandler {

  private static final List<String> DAV_CAPABILITIES = List.of(
      "1", "2", "3",
      "calendar-access",
      "calendar-schedule",
      "calendar-auto-schedule",
      "extended-mkcol",
      "access-control");
  private static final String CALDAV_CONTENT_TYPE = "text/calendar; charset=utf-8";

  private final CalendarService calendarService;
  private final EventService eventService;

  public CalDavHandler(CalendarService calendarService, EventService eventService) {
    this.calendarService = calendarService;
    this.eventService = eventService;
  }

  public Mono<ServerResponse> wellKnown(ServerRequest request) {
    return ServerResponse.status(HttpStatus.MOVED_PERMANENTLY)
        .header(HttpHeaders.LOCATION, "/calendars/")
        .build();
  }

  public Mono<ServerResponse> options(ServerRequest request) {
    return ServerResponse.ok()
        .header("DAV", String.join(", ", DAV_CAPABILITIES))
        .header("Allow", "OPTIONS, GET, HEAD, PUT, DELETE, PROPFIND, PROPPATCH, REPORT, MKCALENDAR, MKCOL")
        .header(HttpHeaders.CONTENT_LENGTH, "0")
        .build();
  }

  public Mono<ServerResponse> getCalendarHome(ServerRequest request) {
    return ServerResponse.ok()
        .header("DAV", String.join(", ", DAV_CAPABILITIES))
        .contentType(MediaType.TEXT_PLAIN)
        .bodyValue("FastCal CalDAV Server");
  }

  public Mono<ServerResponse> createCalendar(ServerRequest request) {
    String userIdStr = request.pathVariable("userId");
    if (!InputValidator.isValidUserId(userIdStr)) {
      return ServerResponse.badRequest().bodyValue("Invalid user ID format");
    }
    UserId userId = UserId.of(userIdStr);

    String calendarIdStr = request.pathVariable("calendarId");
    if (!InputValidator.isValidCalendarId(calendarIdStr)) {
      return ServerResponse.badRequest().bodyValue("Invalid calendar ID format");
    }
    CalendarId calendarId = CalendarId.of(calendarIdStr);

    return request.principal()
        .map(Principal::getName)
        .filter(authenticatedUserId -> authenticatedUserId.equals(userIdStr))
        .switchIfEmpty(Mono.defer(() -> ServerResponse.status(HttpStatus.FORBIDDEN).build().then(Mono.empty())))
        .flatMap(authenticatedUserId -> request.bodyToMono(String.class)
            .defaultIfEmpty("")
            .flatMap(body -> calendarService.createCalendar(userId, calendarId, body)
                .then(ServerResponse.status(HttpStatus.CREATED)
                    .header(HttpHeaders.LOCATION, "/calendars/" + userIdStr + "/" + calendarIdStr + "/")
                    .build()))
            .onErrorResume(e -> ServerResponse.status(HttpStatus.CONFLICT)
                .contentType(MediaType.APPLICATION_XML)
                .bodyValue(errorResponse("calendar-collection-location-ok",
                    e.getMessage() != null ? e.getMessage() : "Calendar creation failed"))));
  }

  public Mono<ServerResponse> deleteCalendar(ServerRequest request) {
    String userIdStr = request.pathVariable("userId");
    if (!InputValidator.isValidUserId(userIdStr)) {
      return ServerResponse.badRequest().bodyValue("Invalid user ID format");
    }
    UserId userId = UserId.of(userIdStr);

    String calendarIdStr = request.pathVariable("calendarId");
    if (!InputValidator.isValidCalendarId(calendarIdStr)) {
      return ServerResponse.badRequest().bodyValue("Invalid calendar ID format");
    }
    CalendarId calendarId = CalendarId.of(calendarIdStr);

    return request.principal()
        .map(Principal::getName)
        .filter(authenticatedUserId -> authenticatedUserId.equals(userIdStr))
        .switchIfEmpty(Mono.defer(() -> ServerResponse.status(HttpStatus.FORBIDDEN).build().then(Mono.empty())))
        .flatMap(authenticatedUserId -> calendarService.deleteCalendar(userId, calendarId)
            .then(ServerResponse.noContent().build())
            .onErrorResume(e -> ServerResponse.status(HttpStatus.NOT_FOUND).build()));
  }

  public Mono<ServerResponse> getEvent(ServerRequest request) {
    String userIdStr = request.pathVariable("userId");
    if (!InputValidator.isValidUserId(userIdStr)) {
      return ServerResponse.badRequest().bodyValue("Invalid user ID format");
    }
    UserId userId = UserId.of(userIdStr);

    String calendarIdStr = request.pathVariable("calendarId");
    if (!InputValidator.isValidCalendarId(calendarIdStr)) {
      return ServerResponse.badRequest().bodyValue("Invalid calendar ID format");
    }
    CalendarId calendarId = CalendarId.of(calendarIdStr);

    String eventUid = request.pathVariable("eventUid");
    if (!InputValidator.isValidEventUid(eventUid)) {
      return ServerResponse.badRequest().bodyValue("Invalid event UID format");
    }

    return request.principal()
        .map(Principal::getName)
        .filter(authenticatedUserId -> authenticatedUserId.equals(userIdStr))
        .switchIfEmpty(Mono.defer(() -> ServerResponse.status(HttpStatus.FORBIDDEN).build().then(Mono.empty())))
        .flatMap(authenticatedUserId -> eventService.getEvent(userId, calendarId, eventUid)
            .flatMap(event -> ServerResponse.ok()
                .contentType(MediaType.parseMediaType(CALDAV_CONTENT_TYPE))
                .header("ETag", "\"" + event.getEtag().getValue() + "\"")
                .bodyValue(event.getIcalData().getValue()))
            .onErrorResume(e -> ServerResponse.notFound().build()));
  }

  public Mono<ServerResponse> putEvent(ServerRequest request) {
    String userIdStr = request.pathVariable("userId");
    if (!InputValidator.isValidUserId(userIdStr)) {
      return ServerResponse.badRequest().bodyValue("Invalid user ID format");
    }
    UserId userId = UserId.of(userIdStr);

    String calendarIdStr = request.pathVariable("calendarId");
    if (!InputValidator.isValidCalendarId(calendarIdStr)) {
      return ServerResponse.badRequest().bodyValue("Invalid calendar ID format");
    }
    CalendarId calendarId = CalendarId.of(calendarIdStr);

    String eventUid = request.pathVariable("eventUid");
    if (!InputValidator.isValidEventUid(eventUid)) {
      return ServerResponse.badRequest().bodyValue("Invalid event UID format");
    }

    String ifMatch = request.headers().firstHeader("If-Match");
    String ifNoneMatch = request.headers().firstHeader("If-None-Match");

    return request.principal()
        .map(Principal::getName)
        .filter(authenticatedUserId -> authenticatedUserId.equals(userIdStr))
        .switchIfEmpty(Mono.defer(() -> ServerResponse.status(HttpStatus.FORBIDDEN).build().then(Mono.empty())))
        .flatMap(authenticatedUserId -> request.bodyToMono(String.class)
            .defaultIfEmpty("")
            .flatMap(body -> eventService.saveEvent(userId, calendarId, eventUid, body, ifMatch, ifNoneMatch))
            .flatMap(result -> {
              if (result.isCreated()) {
                return ServerResponse.status(HttpStatus.CREATED)
                    .header("ETag", "\"" + result.etag() + "\"")
                    .header(HttpHeaders.LOCATION,
                        "/calendars/" + userIdStr + "/" + calendarIdStr + "/" + eventUid + ".ics")
                    .build();
              } else {
                return ServerResponse.noContent()
                    .header("ETag", "\"" + result.etag() + "\"")
                    .build();
              }
            })
            .onErrorResume(PreconditionFailedException.class,
                e -> ServerResponse.status(HttpStatus.PRECONDITION_FAILED).build())
            .onErrorResume(e -> ServerResponse.status(HttpStatus.CONFLICT)
                .contentType(MediaType.APPLICATION_XML)
                .bodyValue(errorResponse("valid-calendar-data",
                    e.getMessage() != null ? e.getMessage() : "Invalid calendar data"))));
  }

  public Mono<ServerResponse> deleteEvent(ServerRequest request) {
    String userIdStr = request.pathVariable("userId");
    if (!InputValidator.isValidUserId(userIdStr)) {
      return ServerResponse.badRequest().bodyValue("Invalid user ID format");
    }
    UserId userId = UserId.of(userIdStr);

    String calendarIdStr = request.pathVariable("calendarId");
    if (!InputValidator.isValidCalendarId(calendarIdStr)) {
      return ServerResponse.badRequest().bodyValue("Invalid calendar ID format");
    }
    CalendarId calendarId = CalendarId.of(calendarIdStr);

    String eventUid = request.pathVariable("eventUid");
    if (!InputValidator.isValidEventUid(eventUid)) {
      return ServerResponse.badRequest().bodyValue("Invalid event UID format");
    }

    String ifMatch = request.headers().firstHeader("If-Match");

    return request.principal()
        .map(Principal::getName)
        .filter(authenticatedUserId -> authenticatedUserId.equals(userIdStr))
        .switchIfEmpty(Mono.defer(() -> ServerResponse.status(HttpStatus.FORBIDDEN).build().then(Mono.empty())))
        .flatMap(authenticatedUserId -> eventService.deleteEvent(userId, calendarId, eventUid, ifMatch)
            .then(ServerResponse.noContent().build())
            .onErrorResume(PreconditionFailedException.class,
                e -> ServerResponse.status(HttpStatus.PRECONDITION_FAILED).build())
            .onErrorResume(e -> ServerResponse.notFound().build()));
  }

  private String errorResponse(String errorType, String message) {
    return """
        <?xml version="1.0" encoding="UTF-8"?>
        <D:error xmlns:D="DAV:" xmlns:C="urn:ietf:params:xml:ns:caldav">
          <C:%s/>
          <D:error-description>%s</D:error-description>
        </D:error>""".formatted(errorType, message);
  }
}
