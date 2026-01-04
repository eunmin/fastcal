package com.fastcal.service.caldav;

import com.fastcal.domain.model.Calendar;
import com.fastcal.domain.model.vo.CalendarColor;
import com.fastcal.domain.model.vo.CalendarDescription;
import com.fastcal.domain.model.vo.CalendarDisplayName;
import com.fastcal.domain.model.vo.CalendarId;
import com.fastcal.domain.model.vo.CalendarTimezone;
import com.fastcal.domain.model.vo.CTag;
import com.fastcal.domain.model.vo.SyncToken;
import com.fastcal.domain.model.vo.UserId;
import com.fastcal.domain.repository.CalendarRepository;
import com.fastcal.exception.CalendarAlreadyExistsException;
import com.fastcal.exception.CalendarNotFoundException;
import com.fastcal.service.cache.CacheService;
import com.fastcal.xml.MkCalendarParser;
import com.fastcal.xml.MkCalendarParser.ParsedCalendarProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CalendarService {

  private final CalendarRepository calendarRepository;
  private final CacheService cacheService;
  private final MkCalendarParser mkCalendarParser;

  @Value("${caldav.sync.token-prefix}")
  private String syncTokenPrefix;

  public Mono<Calendar> getCalendar(UserId userId, CalendarId calendarId) {
    Calendar cached = cacheService.getCalendar(userId, calendarId);
    if (cached != null) {
      return Mono.just(cached);
    }

    return calendarRepository.findByUserIdAndCalendarId(userId, calendarId)
        .doOnNext(cacheService::cacheCalendar);
  }

  public Flux<Calendar> getCalendarsByUser(UserId userId) {
    return calendarRepository.findByUserId(userId);
  }

  public Mono<Calendar> createCalendar(UserId userId, CalendarId calendarId, String mkcalendarXml) {
    return doCreateCalendar(userId, calendarId, mkcalendarXml)
        .flatMap(saved -> {
          String ctag = saved.getCtag() != null ? saved.getCtag().getValue() : null;
          return cacheService.cacheCTag(userId, calendarId, ctag)
              .onErrorResume(e -> Mono.empty())
              .thenReturn(saved);
        });
  }

  @Transactional
  protected Mono<Calendar> doCreateCalendar(UserId userId, CalendarId calendarId, String mkcalendarXml) {
    return calendarRepository.findByUserIdAndCalendarId(userId, calendarId)
        .flatMap(existing -> Mono.<Calendar>error(
            new CalendarAlreadyExistsException("Calendar " + calendarId + " already exists")))
        .switchIfEmpty(Mono.defer(() -> {
          ParsedCalendarProperties props = mkCalendarParser.parse(mkcalendarXml);

          Calendar calendar = Calendar.of(userId, calendarId, CalendarDisplayName.ofNullable(props.displayName()));
          calendar.setDescription(CalendarDescription.ofNullable(props.description()));
          calendar.setColor(CalendarColor.ofNullable(props.color()));
          calendar.setTimezone(CalendarTimezone.ofNullable(props.timezone()));
          calendar.setCtag(CTag.of(generateCTag()));
          calendar.setSyncToken(SyncToken.of(generateSyncToken()));
          calendar.setCreatedAt(LocalDateTime.now());
          calendar.setUpdatedAt(LocalDateTime.now());

          return calendarRepository.save(calendar)
              .doOnNext(cacheService::cacheCalendar);
        }));
  }

  public Mono<Void> deleteCalendar(UserId userId, CalendarId calendarId) {
    return doDeleteCalendar(userId, calendarId)
        .then(Mono.fromRunnable(() -> cacheService.invalidateCalendar(userId, calendarId)))
        .then(Mono.defer(() -> cacheService.invalidateCTag(userId, calendarId)))
        .then();
  }

  @Transactional
  protected Mono<Void> doDeleteCalendar(UserId userId, CalendarId calendarId) {
    return calendarRepository.findByUserIdAndCalendarId(userId, calendarId)
        .switchIfEmpty(Mono.error(new CalendarNotFoundException("Calendar " + calendarId + " not found")))
        .flatMap(calendar -> calendarRepository.delete(calendar));
  }

  public Mono<String> updateCTag(UserId userId, CalendarId calendarId) {
    String newCTag = generateCTag();
    String newSyncToken = generateSyncToken();

    return doUpdateCTag(userId, calendarId, newCTag, newSyncToken)
        .then(Mono.fromRunnable(() -> cacheService.invalidateCalendar(userId, calendarId)))
        .then(Mono.defer(() -> cacheService.cacheCTag(userId, calendarId, newCTag)))
        .thenReturn(newCTag);
  }

  @Transactional
  protected Mono<Integer> doUpdateCTag(UserId userId, CalendarId calendarId, String newCTag, String newSyncToken) {
    return calendarRepository.updateCTagAndSyncToken(userId, calendarId, newCTag, newSyncToken);
  }

  public Mono<Calendar> updateCalendarProperties(UserId userId, CalendarId calendarId, String proppatchXml) {
    return doUpdateCalendarProperties(userId, calendarId, proppatchXml)
        .flatMap(saved -> Mono.defer(() -> cacheService.cacheCTag(userId, calendarId,
                saved.getCtag() != null ? saved.getCtag().getValue() : null))
            .thenReturn(saved));
  }

  @Transactional
  protected Mono<Calendar> doUpdateCalendarProperties(UserId userId, CalendarId calendarId, String proppatchXml) {
    return calendarRepository.findByUserIdAndCalendarId(userId, calendarId)
        .switchIfEmpty(Mono.error(new CalendarNotFoundException("Calendar " + calendarId + " not found")))
        .flatMap(calendar -> {
          ParsedCalendarProperties props = mkCalendarParser.parse(proppatchXml);

          if (props.displayName() != null)
            calendar.setDisplayName(CalendarDisplayName.of(props.displayName()));
          if (props.description() != null)
            calendar.setDescription(CalendarDescription.of(props.description()));
          if (props.color() != null)
            calendar.setColor(CalendarColor.ofNullable(props.color()));

          calendar.setUpdatedAt(LocalDateTime.now());
          CTag newCTag = CTag.of(generateCTag());
          calendar.setCtag(newCTag);

          return calendarRepository.save(calendar)
              .doOnNext(cacheService::cacheCalendar);
        });
  }

  private String generateCTag() {
    return UUID.randomUUID().toString();
  }

  private String generateSyncToken() {
    return syncTokenPrefix + System.currentTimeMillis() + "-" + UUID.randomUUID().toString();
  }
}
