package com.fastcal.config;

import com.fastcal.handler.CalDavHandler;
import com.fastcal.handler.PropFindHandler;
import com.fastcal.handler.PropPatchHandler;
import com.fastcal.handler.ReportHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;

@Configuration
public class WebFluxConfig {

  private final CalDavHandler calDavHandler;
  private final PropFindHandler propFindHandler;
  private final PropPatchHandler propPatchHandler;
  private final ReportHandler reportHandler;

  public WebFluxConfig(CalDavHandler calDavHandler, PropFindHandler propFindHandler,
      PropPatchHandler propPatchHandler, ReportHandler reportHandler) {
    this.calDavHandler = calDavHandler;
    this.propFindHandler = propFindHandler;
    this.propPatchHandler = propPatchHandler;
    this.reportHandler = reportHandler;
  }

  @Bean
  public RouterFunction<ServerResponse> calDavRoutes() {
    return RouterFunctions.route()
        .route(method(HttpMethod.valueOf("PROPFIND")).and(path("/")), propFindHandler::handleCalendarRoot)
        .route(GET("/.well-known/caldav"), calDavHandler::wellKnown)
        .route(GET("/.well-known/caldav/"), calDavHandler::wellKnown)
        .route(method(HttpMethod.valueOf("PROPFIND")).and(path("/.well-known/caldav")), calDavHandler::wellKnown)
        .route(method(HttpMethod.valueOf("PROPFIND")).and(path("/.well-known/caldav/")), calDavHandler::wellKnown)
        .route(method(HttpMethod.valueOf("PROPFIND")).and(path("/principals/{userId}")), propFindHandler::handlePrincipal)
        .route(method(HttpMethod.valueOf("PROPFIND")).and(path("/principals/{userId}/")), propFindHandler::handlePrincipal)
        .route(method(HttpMethod.valueOf("PROPFIND")).and(path("/calendars")), propFindHandler::handleCalendarRoot)
        .route(method(HttpMethod.valueOf("PROPFIND")).and(path("/calendars/")), propFindHandler::handleCalendarRoot)
        .route(GET("/calendars"), calDavHandler::getCalendarHome)
        .route(GET("/calendars/"), calDavHandler::getCalendarHome)
        .route(method(HttpMethod.valueOf("PROPFIND")).and(path("/calendars/{userId}")), propFindHandler::handleCalendarHome)
        .route(method(HttpMethod.valueOf("PROPFIND")).and(path("/calendars/{userId}/")), propFindHandler::handleCalendarHome)
        .route(GET("/calendars/{userId}"), calDavHandler::getCalendarHome)
        .route(GET("/calendars/{userId}/"), calDavHandler::getCalendarHome)
        .route(method(HttpMethod.valueOf("PROPFIND")).and(path("/calendars/{userId}/{calendarId}")), propFindHandler::handleCalendar)
        .route(method(HttpMethod.valueOf("PROPFIND")).and(path("/calendars/{userId}/{calendarId}/")), propFindHandler::handleCalendar)
        .route(method(HttpMethod.valueOf("PROPPATCH")).and(path("/calendars/{userId}/{calendarId}")), propPatchHandler::handleCalendar)
        .route(method(HttpMethod.valueOf("PROPPATCH")).and(path("/calendars/{userId}/{calendarId}/")), propPatchHandler::handleCalendar)
        .route(method(HttpMethod.valueOf("REPORT")).and(path("/calendars/{userId}/{calendarId}")), reportHandler::handle)
        .route(method(HttpMethod.valueOf("REPORT")).and(path("/calendars/{userId}/{calendarId}/")), reportHandler::handle)
        .route(method(HttpMethod.valueOf("MKCALENDAR")).and(path("/calendars/{userId}/{calendarId}")), calDavHandler::createCalendar)
        .route(method(HttpMethod.valueOf("MKCALENDAR")).and(path("/calendars/{userId}/{calendarId}/")), calDavHandler::createCalendar)
        .route(DELETE("/calendars/{userId}/{calendarId}"), calDavHandler::deleteCalendar)
        .route(DELETE("/calendars/{userId}/{calendarId}/"), calDavHandler::deleteCalendar)
        .route(GET("/calendars/{userId}/{calendarId}/{eventUid}.ics"), calDavHandler::getEvent)
        .route(PUT("/calendars/{userId}/{calendarId}/{eventUid}.ics"), calDavHandler::putEvent)
        .route(DELETE("/calendars/{userId}/{calendarId}/{eventUid}.ics"), calDavHandler::deleteEvent)
        .route(OPTIONS("/"), calDavHandler::options)
        .route(OPTIONS("/.well-known/caldav"), calDavHandler::options)
        .route(OPTIONS("/.well-known/caldav/"), calDavHandler::options)
        .route(OPTIONS("/principals/{userId}"), calDavHandler::options)
        .route(OPTIONS("/principals/{userId}/"), calDavHandler::options)
        .route(OPTIONS("/calendars"), calDavHandler::options)
        .route(OPTIONS("/calendars/"), calDavHandler::options)
        .route(OPTIONS("/calendars/{userId}"), calDavHandler::options)
        .route(OPTIONS("/calendars/{userId}/"), calDavHandler::options)
        .route(OPTIONS("/calendars/{userId}/{calendarId}"), calDavHandler::options)
        .route(OPTIONS("/calendars/{userId}/{calendarId}/"), calDavHandler::options)
        .route(OPTIONS("/calendars/{userId}/{calendarId}/{eventUid}.ics"), calDavHandler::options)
        .build();
  }
}
