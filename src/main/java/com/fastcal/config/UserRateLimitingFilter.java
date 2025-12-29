package com.fastcal.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class UserRateLimitingFilter implements WebFilter {

  private final Cache<String, Bucket> userBuckets = Caffeine.newBuilder()
      .maximumSize(50_000)
      .expireAfterAccess(10, TimeUnit.MINUTES)
      .build();

  private static final int USER_REQUESTS_PER_MINUTE = 300;

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    String path = exchange.getRequest().getPath().value();

    if (isPublicPath(path)) {
      return chain.filter(exchange);
    }

    return exchange.getPrincipal()
        .map(Principal::getName)
        .flatMap(userId -> {
          Bucket bucket = resolveUserBucket(userId);

          if (bucket.tryConsume(1)) {
            return chain.filter(exchange);
          } else {
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders().add("Retry-After", "60");
            exchange.getResponse().getHeaders().add("X-RateLimit-Limit", String.valueOf(USER_REQUESTS_PER_MINUTE));
            exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", "0");
            return exchange.getResponse().setComplete();
          }
        })
        .switchIfEmpty(chain.filter(exchange));
  }

  private boolean isPublicPath(String path) {
    return path.equals("/") ||
        path.endsWith(".html") ||
        path.equals("/favicon.ico") ||
        path.startsWith("/api/auth") ||
        path.startsWith("/api/users") ||
        path.startsWith("/internal/actuator");
  }

  private Bucket resolveUserBucket(String userId) {
    return userBuckets.get(userId, k -> createUserBucket());
  }

  private Bucket createUserBucket() {
    Bandwidth limit = Bandwidth.builder()
        .capacity(USER_REQUESTS_PER_MINUTE)
        .refillGreedy(USER_REQUESTS_PER_MINUTE, Duration.ofMinutes(1))
        .build();

    return Bucket.builder().addLimit(limit).build();
  }
}
