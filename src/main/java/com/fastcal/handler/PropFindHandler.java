package com.fastcal.handler;

import com.fastcal.domain.model.vo.CalendarId;
import com.fastcal.domain.model.vo.UserId;
import com.fastcal.service.caldav.PropFindService;
import com.fastcal.util.InputValidator;
import com.fastcal.xml.DavXmlBuilder;
import com.fastcal.xml.PropFindParser;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.List;

@Component
public class PropFindHandler {

  private static final MediaType MULTISTATUS_CONTENT_TYPE = MediaType.parseMediaType("application/xml; charset=utf-8");

  private final PropFindService propFindService;
  private final PropFindParser propFindParser;
  private final DavXmlBuilder davXmlBuilder;

  public PropFindHandler(PropFindService propFindService, PropFindParser propFindParser, DavXmlBuilder davXmlBuilder) {
    this.propFindService = propFindService;
    this.propFindParser = propFindParser;
    this.davXmlBuilder = davXmlBuilder;
  }

  public Mono<ServerResponse> handleCalendarRoot(ServerRequest request) {
    String depth = request.headers().firstHeader("Depth");
    if (!InputValidator.isValidDepth(depth)) {
      return ServerResponse.badRequest().bodyValue("Invalid Depth header value");
    }
    final String finalDepth = InputValidator.sanitizeDepth(depth);

    return request.principal()
        .map(Principal::getName)
        .switchIfEmpty(Mono.error(new org.springframework.security.access.AccessDeniedException("Authentication required")))
        .flatMap(userIdStr -> request.bodyToMono(String.class)
            .defaultIfEmpty("")
            .flatMap(body -> {
              UserId userId = UserId.of(userIdStr);
              List<String> requestedProps = propFindParser.parse(body);
              return propFindService.getCalendarHomeProperties(userId, finalDepth, requestedProps);
            })
            .flatMap(responses -> {
              String xml = davXmlBuilder.buildMultiStatus(responses);
              return ServerResponse.status(HttpStatus.MULTI_STATUS)
                  .contentType(MULTISTATUS_CONTENT_TYPE)
                  .bodyValue(xml);
            }));
  }

  public Mono<ServerResponse> handlePrincipal(ServerRequest request) {
    String userIdStr = request.pathVariable("userId");
    if (!InputValidator.isValidUserId(userIdStr)) {
      return ServerResponse.badRequest().bodyValue("Invalid user ID format");
    }

    String depth = request.headers().firstHeader("Depth");
    if (!InputValidator.isValidDepth(depth)) {
      return ServerResponse.badRequest().bodyValue("Invalid Depth header value");
    }

    return request.principal()
        .map(Principal::getName)
        .switchIfEmpty(Mono.error(new org.springframework.security.access.AccessDeniedException("Authentication required")))
        .filter(authenticatedUserId -> authenticatedUserId.equals(userIdStr))
        .switchIfEmpty(Mono.error(new org.springframework.security.access.AccessDeniedException("Access denied to this resource")))
        .flatMap(authenticatedUserId -> request.bodyToMono(String.class)
            .defaultIfEmpty("")
            .flatMap(body -> {
              UserId userId = UserId.of(userIdStr);
              List<String> requestedProps = propFindParser.parse(body);
              return propFindService.getPrincipalProperties(userId, requestedProps);
            })
            .flatMap(response -> {
              String xml = davXmlBuilder.buildMultiStatus(List.of(response));
              return ServerResponse.status(HttpStatus.MULTI_STATUS)
                  .contentType(MULTISTATUS_CONTENT_TYPE)
                  .bodyValue(xml);
            }));
  }

  public Mono<ServerResponse> handleCalendarHome(ServerRequest request) {
    String userIdStr = request.pathVariable("userId");
    if (!InputValidator.isValidUserId(userIdStr)) {
      return ServerResponse.badRequest().bodyValue("Invalid user ID format");
    }

    String depth = request.headers().firstHeader("Depth");
    if (!InputValidator.isValidDepth(depth)) {
      return ServerResponse.badRequest().bodyValue("Invalid Depth header value");
    }
    final String finalDepth = InputValidator.sanitizeDepth(depth);

    return request.principal()
        .map(Principal::getName)
        .switchIfEmpty(Mono.error(new org.springframework.security.access.AccessDeniedException("Authentication required")))
        .filter(authenticatedUserId -> authenticatedUserId.equals(userIdStr))
        .switchIfEmpty(Mono.error(new org.springframework.security.access.AccessDeniedException("Access denied to this resource")))
        .flatMap(authenticatedUserId -> request.bodyToMono(String.class)
            .defaultIfEmpty("")
            .flatMap(body -> {
              UserId userId = UserId.of(userIdStr);
              List<String> requestedProps = propFindParser.parse(body);
              return propFindService.getCalendarHomeProperties(userId, finalDepth, requestedProps);
            })
            .flatMap(responses -> {
              String xml = davXmlBuilder.buildMultiStatus(responses);
              return ServerResponse.status(HttpStatus.MULTI_STATUS)
                  .contentType(MULTISTATUS_CONTENT_TYPE)
                  .bodyValue(xml);
            }));
  }

  public Mono<ServerResponse> handleCalendar(ServerRequest request) {
    String userIdStr = request.pathVariable("userId");
    if (!InputValidator.isValidUserId(userIdStr)) {
      return ServerResponse.badRequest().bodyValue("Invalid user ID format");
    }

    String calendarIdStr = request.pathVariable("calendarId");
    if (!InputValidator.isValidCalendarId(calendarIdStr)) {
      return ServerResponse.badRequest().bodyValue("Invalid calendar ID format");
    }
    CalendarId calendarId = CalendarId.of(calendarIdStr);

    String depth = request.headers().firstHeader("Depth");
    if (!InputValidator.isValidDepth(depth)) {
      return ServerResponse.badRequest().bodyValue("Invalid Depth header value");
    }
    final String finalDepth = InputValidator.sanitizeDepth(depth);

    return request.principal()
        .map(Principal::getName)
        .switchIfEmpty(Mono.error(new org.springframework.security.access.AccessDeniedException("Authentication required")))
        .filter(authenticatedUserId -> authenticatedUserId.equals(userIdStr))
        .switchIfEmpty(Mono.error(new org.springframework.security.access.AccessDeniedException("Access denied to this resource")))
        .flatMap(authenticatedUserId -> request.bodyToMono(String.class)
            .defaultIfEmpty("")
            .flatMap(body -> {
              UserId userId = UserId.of(userIdStr);
              List<String> requestedProps = propFindParser.parse(body);
              return propFindService.getCalendarProperties(userId, calendarId, finalDepth, requestedProps);
            })
            .flatMap(responses -> {
              String xml = davXmlBuilder.buildMultiStatus(responses);
              return ServerResponse.status(HttpStatus.MULTI_STATUS)
                  .contentType(MULTISTATUS_CONTENT_TYPE)
                  .bodyValue(xml);
            }));
  }
}
