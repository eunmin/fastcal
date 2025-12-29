package com.fastcal.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
@Order(-2)
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

  @Value("${app.error.include-details:false}")
  private boolean includeErrorDetails;

  @Override
  public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
    var response = exchange.getResponse();
    String path = exchange.getRequest().getPath().value();

    if (response.isCommitted()) {
      return Mono.empty();
    }

    if (ex instanceof CalDavException calDavEx) {
      response.setStatusCode(calDavEx.getHttpStatus());
      response.getHeaders().setContentType(MediaType.APPLICATION_XML);

      String errorXml = buildErrorXml(calDavEx);
      return writeResponse(exchange, errorXml);
    } else {
      log.error("Unexpected error on {}: {}", path, ex.getMessage(), ex);
      response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
      response.getHeaders().setContentType(MediaType.APPLICATION_XML);

      String errorXml = buildGenericErrorXml("Internal server error");
      return writeResponse(exchange, errorXml);
    }
  }

  private Mono<Void> writeResponse(ServerWebExchange exchange, String content) {
    var response = exchange.getResponse();
    DataBufferFactory bufferFactory = response.bufferFactory();
    DataBuffer dataBuffer = bufferFactory.wrap(content.getBytes(StandardCharsets.UTF_8));
    return response.writeWith(Mono.just(dataBuffer));
  }

  private String buildErrorXml(CalDavException ex) {
    String davError = ex.getDavErrorType() != null ? "<C:" + ex.getDavErrorType() + "/>" : "";
    String message = includeErrorDetails
        ? (ex.getMessage() != null ? ex.getMessage() : "")
        : getSafeErrorMessage(ex);
    return """
        <?xml version="1.0" encoding="UTF-8"?>
        <D:error xmlns:D="DAV:" xmlns:C="urn:ietf:params:xml:ns:caldav">
          %s
          <D:error-description>%s</D:error-description>
        </D:error>""".formatted(davError, escapeXml(message));
  }

  private String getSafeErrorMessage(CalDavException ex) {
    if (ex instanceof CalendarNotFoundException) {
      return "Calendar not found";
    } else if (ex instanceof EventNotFoundException) {
      return "Event not found";
    } else if (ex instanceof CalendarAlreadyExistsException) {
      return "Calendar already exists";
    } else if (ex instanceof PreconditionFailedException) {
      return "Precondition failed";
    } else if (ex instanceof InvalidCalendarDataException) {
      return "Invalid calendar data";
    }
    return "Request failed";
  }

  private String buildGenericErrorXml(String message) {
    return """
        <?xml version="1.0" encoding="UTF-8"?>
        <D:error xmlns:D="DAV:">
          <D:error-description>%s</D:error-description>
        </D:error>""".formatted(escapeXml(message));
  }

  private String escapeXml(String text) {
    return text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;");
  }
}
