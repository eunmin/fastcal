package com.fastcal.config;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter implements WebFilter {

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    exchange.getResponse().beforeCommit(() -> {
      var headers = exchange.getResponse().getHeaders();
      if (!headers.containsKey("X-Content-Type-Options")) {
        headers.add("X-Content-Type-Options", "nosniff");
      }
      if (!headers.containsKey("X-Frame-Options")) {
        headers.add("X-Frame-Options", "DENY");
      }
      if (!headers.containsKey("X-XSS-Protection")) {
        headers.add("X-XSS-Protection", "1; mode=block");
      }
      if (!headers.containsKey("Referrer-Policy")) {
        headers.add("Referrer-Policy", "strict-origin-when-cross-origin");
      }
      if (!headers.containsKey("Cache-Control")) {
        headers.add("Cache-Control", "no-store, no-cache, must-revalidate");
      }
      if (!headers.containsKey("Content-Security-Policy")) {
        headers.add("Content-Security-Policy",
            "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; frame-ancestors 'none'");
      }
      return Mono.empty();
    });

    return chain.filter(exchange);
  }
}
