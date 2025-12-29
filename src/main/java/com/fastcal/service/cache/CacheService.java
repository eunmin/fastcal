package com.fastcal.service.cache;

import com.fastcal.domain.model.Calendar;
import com.fastcal.domain.model.CalendarEvent;
import com.fastcal.domain.model.vo.CalendarId;
import com.fastcal.domain.model.vo.EventUid;
import com.fastcal.domain.model.vo.UserId;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
public class CacheService {

  private static final Logger log = LoggerFactory.getLogger(CacheService.class);
  private static final String CIRCUIT_BREAKER_NAME = "redis";
  private static final String CALENDAR_PREFIX = "calendar:";
  private static final String EVENT_PREFIX = "event:";
  private static final Duration REDIS_TTL = Duration.ofMinutes(30);

  private final ReactiveRedisTemplate<String, String> redisTemplate;

  private final Cache<String, Calendar> localCalendarCache;
  private final Cache<String, CalendarEvent> localEventCache;

  public CacheService(ReactiveRedisTemplate<String, String> redisTemplate) {
    this.redisTemplate = redisTemplate;

    this.localCalendarCache = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build();

    this.localEventCache = Caffeine.newBuilder()
        .maximumSize(50_000)
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build();
  }

  public Calendar getCalendar(UserId userId, CalendarId calendarId) {
    String key = calendarKey(userId, calendarId);
    return localCalendarCache.getIfPresent(key);
  }

  public void cacheCalendar(Calendar calendar) {
    String key = calendarKey(calendar.getUserId(), calendar.getCalendarId());
    localCalendarCache.put(key, calendar);
  }

  public void invalidateCalendar(UserId userId, CalendarId calendarId) {
    String key = calendarKey(userId, calendarId);
    localCalendarCache.invalidate(key);
  }

  public CalendarEvent getEvent(UserId userId, CalendarId calendarId, String eventUid) {
    String key = eventKey(userId, calendarId, eventUid);
    return localEventCache.getIfPresent(key);
  }

  public void cacheEvent(CalendarEvent event) {
    EventUid uid = event.getUid();
    String key = eventKey(event.getUserId(), event.getCalendarId(), uid != null ? uid.getValue() : null);
    localEventCache.put(key, event);
  }

  public void invalidateEvent(UserId userId, CalendarId calendarId, String eventUid) {
    String key = eventKey(userId, calendarId, eventUid);
    localEventCache.invalidate(key);
  }

  @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getETagFallback")
  public Mono<String> getETag(UserId userId, CalendarId calendarId, String eventUid) {
    String key = EVENT_PREFIX + userId.getValue() + ":" + calendarId.getValue() + ":" + eventUid + ":etag";
    return redisTemplate.opsForValue().get(key);
  }

  @SuppressWarnings("unused")
  private Mono<String> getETagFallback(UserId userId, CalendarId calendarId, String eventUid, Throwable t) {
    log.warn("Redis circuit breaker open for getETag, returning empty: {}", t.getMessage());
    return Mono.empty();
  }

  @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "cacheETagFallback")
  public Mono<Void> cacheETag(UserId userId, CalendarId calendarId, String eventUid, String etag) {
    String key = EVENT_PREFIX + userId.getValue() + ":" + calendarId.getValue() + ":" + eventUid + ":etag";
    return redisTemplate.opsForValue().set(key, etag, REDIS_TTL).then();
  }

  @SuppressWarnings("unused")
  private Mono<Void> cacheETagFallback(UserId userId, CalendarId calendarId, String eventUid, String etag, Throwable t) {
    log.warn("Redis circuit breaker open for cacheETag: {}", t.getMessage());
    return Mono.empty();
  }

  @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "invalidateETagFallback")
  public Mono<Boolean> invalidateETag(UserId userId, CalendarId calendarId, String eventUid) {
    String key = EVENT_PREFIX + userId.getValue() + ":" + calendarId.getValue() + ":" + eventUid + ":etag";
    return redisTemplate.delete(key).map(count -> count > 0);
  }

  @SuppressWarnings("unused")
  private Mono<Boolean> invalidateETagFallback(UserId userId, CalendarId calendarId, String eventUid, Throwable t) {
    log.warn("Redis circuit breaker open for invalidateETag: {}", t.getMessage());
    return Mono.just(false);
  }

  @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getCTagFallback")
  public Mono<String> getCTag(UserId userId, CalendarId calendarId) {
    String key = CALENDAR_PREFIX + userId.getValue() + ":" + calendarId.getValue() + ":ctag";
    return redisTemplate.opsForValue().get(key);
  }

  @SuppressWarnings("unused")
  private Mono<String> getCTagFallback(UserId userId, CalendarId calendarId, Throwable t) {
    log.warn("Redis circuit breaker open for getCTag, returning empty: {}", t.getMessage());
    return Mono.empty();
  }

  @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "cacheCTagFallback")
  public Mono<Boolean> cacheCTag(UserId userId, CalendarId calendarId, String ctag) {
    String key = CALENDAR_PREFIX + userId.getValue() + ":" + calendarId.getValue() + ":ctag";
    return redisTemplate.opsForValue().set(key, ctag, REDIS_TTL);
  }

  @SuppressWarnings("unused")
  private Mono<Boolean> cacheCTagFallback(UserId userId, CalendarId calendarId, String ctag, Throwable t) {
    log.warn("Redis circuit breaker open for cacheCTag: {}", t.getMessage());
    return Mono.just(false);
  }

  @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "invalidateCTagFallback")
  public Mono<Boolean> invalidateCTag(UserId userId, CalendarId calendarId) {
    String key = CALENDAR_PREFIX + userId.getValue() + ":" + calendarId.getValue() + ":ctag";
    return redisTemplate.delete(key).map(count -> count > 0);
  }

  @SuppressWarnings("unused")
  private Mono<Boolean> invalidateCTagFallback(UserId userId, CalendarId calendarId, Throwable t) {
    log.warn("Redis circuit breaker open for invalidateCTag: {}", t.getMessage());
    return Mono.just(false);
  }

  private String calendarKey(UserId userId, CalendarId calendarId) {
    return CALENDAR_PREFIX + userId.getValue() + ":" + calendarId.getValue();
  }

  private String eventKey(UserId userId, CalendarId calendarId, String eventUid) {
    return EVENT_PREFIX + userId.getValue() + ":" + calendarId.getValue() + ":" + eventUid;
  }
}
