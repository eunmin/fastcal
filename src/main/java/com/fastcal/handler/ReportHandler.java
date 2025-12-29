package com.fastcal.handler;

import com.fastcal.domain.model.vo.CalendarId;
import com.fastcal.domain.model.vo.UserId;
import com.fastcal.service.caldav.ReportService;
import com.fastcal.util.InputValidator;
import com.fastcal.xml.DavXmlBuilder;
import com.fastcal.xml.ReportParser;
import com.fastcal.xml.DavResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ReportHandler {

  private static final MediaType MULTISTATUS_CONTENT_TYPE = MediaType.parseMediaType("application/xml; charset=utf-8");

  private final ReportService reportService;
  private final ReportParser reportParser;
  private final DavXmlBuilder davXmlBuilder;

  public Mono<ServerResponse> handle(ServerRequest request) {
    String userIdStr = request.pathVariable("userId");
    if (!InputValidator.isValidUserId(userIdStr)) {
      return ServerResponse.badRequest().bodyValue("Invalid user ID format");
    }

    String calendarIdStr = request.pathVariable("calendarId");
    if (!InputValidator.isValidCalendarId(calendarIdStr)) {
      return ServerResponse.badRequest().bodyValue("Invalid calendar ID format");
    }

    return request.principal()
        .map(Principal::getName)
        .switchIfEmpty(Mono.error(new AccessDeniedException("Authentication required")))
        .filter(authenticatedUserId -> authenticatedUserId.equals(userIdStr))
        .switchIfEmpty(Mono.error(new AccessDeniedException("Access denied to this resource")))
        .flatMap(authenticatedUserId -> request.bodyToMono(String.class)
            .defaultIfEmpty("")
            .flatMap(body -> {
              UserId userId = UserId.of(userIdStr);
              CalendarId calendarId = CalendarId.of(calendarIdStr);
              ReportRequest reportRequest = reportParser.parse(body);

              Mono<List<DavResponse>> responsesMono = switch (reportRequest.type()) {
                case CALENDAR_QUERY -> reportService.calendarQuery(userId, calendarId, reportRequest);
                case CALENDAR_MULTIGET -> reportService.calendarMultiget(userId, calendarId, reportRequest);
                case SYNC_COLLECTION -> reportService.syncCollection(userId, calendarId, reportRequest);
                case FREE_BUSY_QUERY -> reportService.freeBusyQuery(userId, calendarId, reportRequest);
              };

              return responsesMono;
            })
            .flatMap(responses -> {
              String xml = davXmlBuilder.buildMultiStatus(responses);
              return ServerResponse.status(HttpStatus.MULTI_STATUS)
                  .contentType(MULTISTATUS_CONTENT_TYPE)
                  .bodyValue(xml);
            })
            .onErrorResume(IllegalArgumentException.class,
                e -> ServerResponse.badRequest().bodyValue(e.getMessage())));
  }
}
