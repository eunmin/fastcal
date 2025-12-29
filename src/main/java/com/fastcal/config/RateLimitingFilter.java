package com.fastcal.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
@Order(1)
public class RateLimitingFilter implements WebFilter {

  private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
      .maximumSize(100_000)
      .expireAfterAccess(10, TimeUnit.MINUTES)
      .build();

  private static final int AUTH_REQUESTS_PER_MINUTE = 10;
  private static final int API_REQUESTS_PER_MINUTE = 100;
  private static final int CALDAV_REQUESTS_PER_MINUTE = 200;

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    String path = exchange.getRequest().getPath().value();
    String clientIp = getClientIp(exchange);

    Bucket bucket = resolveBucket(clientIp, path);

    if (bucket.tryConsume(1)) {
      return chain.filter(exchange);
    } else {
      exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
      exchange.getResponse().getHeaders().add("Retry-After", "60");
      return exchange.getResponse().setComplete();
    }
  }

  private Bucket resolveBucket(String clientIp, String path) {
    String bucketKey = clientIp + ":" + getBucketType(path);
    return buckets.get(bucketKey, k -> createBucket(path));
  }

  private String getBucketType(String path) {
    if (path.startsWith("/api/auth") || path.startsWith("/api/users")) {
      return "auth";
    } else if (path.startsWith("/calendars") || path.startsWith("/principals")) {
      return "caldav";
    } else {
      return "api";
    }
  }

  private Bucket createBucket(String path) {
    Bandwidth limit;

    if (path.startsWith("/api/auth") || path.startsWith("/api/users")) {
      limit = Bandwidth.builder()
          .capacity(AUTH_REQUESTS_PER_MINUTE)
          .refillGreedy(AUTH_REQUESTS_PER_MINUTE, Duration.ofMinutes(1))
          .build();
    } else if (path.startsWith("/calendars") || path.startsWith("/principals")) {
      limit = Bandwidth.builder()
          .capacity(CALDAV_REQUESTS_PER_MINUTE)
          .refillGreedy(CALDAV_REQUESTS_PER_MINUTE, Duration.ofMinutes(1))
          .build();
    } else {
      limit = Bandwidth.builder()
          .capacity(API_REQUESTS_PER_MINUTE)
          .refillGreedy(API_REQUESTS_PER_MINUTE, Duration.ofMinutes(1))
          .build();
    }

    return Bucket.builder().addLimit(limit).build();
  }

  private String getClientIp(ServerWebExchange exchange) {
    String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
      return xForwardedFor.split(",")[0].trim();
    }

    String xRealIp = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
    if (xRealIp != null && !xRealIp.isEmpty()) {
      return xRealIp;
    }

    var remoteAddress = exchange.getRequest().getRemoteAddress();
    if (remoteAddress != null) {
      return remoteAddress.getAddress().getHostAddress();
    }

    return "unknown";
  }
}
