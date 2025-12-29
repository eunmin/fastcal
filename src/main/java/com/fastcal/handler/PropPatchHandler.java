package com.fastcal.handler;

import com.fastcal.domain.model.vo.CalendarId;
import com.fastcal.domain.model.vo.UserId;
import com.fastcal.service.caldav.CalendarService;
import com.fastcal.util.InputValidator;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class PropPatchHandler {

  private static final MediaType MULTISTATUS_CONTENT_TYPE = MediaType.parseMediaType("application/xml; charset=utf-8");

  private final CalendarService calendarService;

  public PropPatchHandler(CalendarService calendarService) {
    this.calendarService = calendarService;
  }

  public Mono<ServerResponse> handleCalendar(ServerRequest request) {
    String userIdStr = request.pathVariable("userId");
    if (!InputValidator.isValidUserId(userIdStr)) {
      return ServerResponse.badRequest().bodyValue("Invalid user ID format");
    }
    UserId userId = UserId.of(userIdStr);
    CalendarId calendarId = CalendarId.of(request.pathVariable("calendarId"));

    return request.bodyToMono(String.class)
        .defaultIfEmpty("")
        .flatMap(body -> calendarService.updateCalendarProperties(userId, calendarId, body))
        .flatMap(updated -> {
          String xml = buildPropPatchResponse(request.path());
          return ServerResponse.status(HttpStatus.MULTI_STATUS)
              .contentType(MULTISTATUS_CONTENT_TYPE)
              .bodyValue(xml);
        })
        .onErrorResume(e -> {
          String xml = buildPropPatchResponse(request.path());
          return ServerResponse.status(HttpStatus.MULTI_STATUS)
              .contentType(MULTISTATUS_CONTENT_TYPE)
              .bodyValue(xml);
        });
  }

  private String buildPropPatchResponse(String href) {
    return """
        <?xml version="1.0" encoding="UTF-8"?>
        <D:multistatus xmlns:D="DAV:" xmlns:C="urn:ietf:params:xml:ns:caldav" xmlns:CS="http://calendarserver.org/ns/">
          <D:response>
            <D:href>%s</D:href>
            <D:propstat>
              <D:prop/>
              <D:status>HTTP/1.1 200 OK</D:status>
            </D:propstat>
          </D:response>
        </D:multistatus>""".formatted(href);
  }
}
