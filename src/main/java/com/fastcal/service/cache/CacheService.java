package com.fastcal.service.cache;

import com.fastcal.domain.model.Calendar;
import com.fastcal.domain.model.CalendarEvent;
import com.fastcal.domain.model.vo.CalendarId;
import com.fastcal.domain.model.vo.EventUid;
import com.fastcal.domain.model.vo.UserId;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
public class CacheService {

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

  public Mono<String> getETag(UserId userId, CalendarId calendarId, String eventUid) {
    String key = EVENT_PREFIX + userId.getValue() + ":" + calendarId.getValue() + ":" + eventUid + ":etag";
    return redisTemplate.opsForValue().get(key);
  }

  public Mono<Void> cacheETag(UserId userId, CalendarId calendarId, String eventUid, String etag) {
    String key = EVENT_PREFIX + userId.getValue() + ":" + calendarId.getValue() + ":" + eventUid + ":etag";
    return redisTemplate.opsForValue().set(key, etag, REDIS_TTL).then();
  }

  public Mono<Boolean> invalidateETag(UserId userId, CalendarId calendarId, String eventUid) {
    String key = EVENT_PREFIX + userId.getValue() + ":" + calendarId.getValue() + ":" + eventUid + ":etag";
    return redisTemplate.delete(key).map(count -> count > 0);
  }

  public Mono<String> getCTag(UserId userId, CalendarId calendarId) {
    String key = CALENDAR_PREFIX + userId.getValue() + ":" + calendarId.getValue() + ":ctag";
    return redisTemplate.opsForValue().get(key);
  }

  public Mono<Boolean> cacheCTag(UserId userId, CalendarId calendarId, String ctag) {
    String key = CALENDAR_PREFIX + userId.getValue() + ":" + calendarId.getValue() + ":ctag";
    return redisTemplate.opsForValue().set(key, ctag, REDIS_TTL);
  }

  public Mono<Boolean> invalidateCTag(UserId userId, CalendarId calendarId) {
    String key = CALENDAR_PREFIX + userId.getValue() + ":" + calendarId.getValue() + ":ctag";
    return redisTemplate.delete(key).map(count -> count > 0);
  }

  private String calendarKey(UserId userId, CalendarId calendarId) {
    return CALENDAR_PREFIX + userId.getValue() + ":" + calendarId.getValue();
  }

  private String eventKey(UserId userId, CalendarId calendarId, String eventUid) {
    return EVENT_PREFIX + userId.getValue() + ":" + calendarId.getValue() + ":" + eventUid;
  }
}
