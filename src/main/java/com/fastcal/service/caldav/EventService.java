package com.fastcal.service.caldav;

import com.fastcal.domain.model.CalendarEvent;
import com.fastcal.domain.model.ChangeType;
import com.fastcal.domain.model.SyncChange;
import com.fastcal.domain.model.vo.CalendarId;
import com.fastcal.domain.model.vo.ETag;
import com.fastcal.domain.model.vo.EventDescription;
import com.fastcal.domain.model.vo.EventLocation;
import com.fastcal.domain.model.vo.EventSummary;
import com.fastcal.domain.model.vo.EventUid;
import com.fastcal.domain.model.vo.ICalData;
import com.fastcal.domain.model.vo.RecurrenceRule;
import com.fastcal.domain.model.vo.UserId;
import com.fastcal.domain.repository.CalendarEventRepository;
import com.fastcal.domain.repository.SyncChangeRepository;
import com.fastcal.exception.CalendarNotFoundException;
import com.fastcal.exception.EventNotFoundException;
import com.fastcal.exception.PreconditionFailedException;
import com.fastcal.ical.ICalParser;
import com.fastcal.ical.ParsedEvent;
import com.fastcal.service.cache.CacheService;
import com.fastcal.util.ETagUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class EventService {

  private final CalendarEventRepository eventRepository;
  private final SyncChangeRepository syncChangeRepository;
  private final CalendarService calendarService;
  private final CacheService cacheService;
  private final ICalParser iCalParser;

  @Value("${caldav.sync.token-prefix}")
  private String syncTokenPrefix;

  public Mono<CalendarEvent> getEvent(UserId userId, CalendarId calendarId, String eventUid) {
    CalendarEvent cached = cacheService.getEvent(userId, calendarId, eventUid);
    if (cached != null) {
      return Mono.just(cached);
    }

    return eventRepository.findByUserIdAndCalendarIdAndUid(userId, calendarId, eventUid)
        .doOnNext(cacheService::cacheEvent)
        .switchIfEmpty(Mono.error(new EventNotFoundException("Event " + eventUid + " not found")));
  }

  public Flux<CalendarEvent> getEventsByCalendar(UserId userId, CalendarId calendarId) {
    return eventRepository.findByUserIdAndCalendarId(userId, calendarId);
  }

  public Flux<CalendarEvent> getEventsByTimeRange(UserId userId, CalendarId calendarId,
      LocalDateTime start, LocalDateTime end) {
    return eventRepository.findByTimeRange(userId, calendarId, start, end);
  }

  public Mono<EventSaveResult> saveEvent(UserId userId, CalendarId calendarId, String eventUid,
      String icalData, String ifMatch, String ifNoneMatch) {
    Mono<String> cachedEtag = ifMatch != null
        ? cacheService.getETag(userId, calendarId, eventUid)
        : Mono.empty();

    return cachedEtag
        .flatMap(etag -> {
          if (!ETagUtil.matches(ifMatch, etag)) {
            return Mono.<EventSaveResult>error(
                new PreconditionFailedException("ETag mismatch"));
          }
          return doSaveEvent(userId, calendarId, eventUid, icalData, null, ifNoneMatch);
        })
        .switchIfEmpty(Mono.defer(() -> doSaveEvent(userId, calendarId, eventUid, icalData, ifMatch, ifNoneMatch)))
        .flatMap(result -> {
          Mono<Void> cacheETag = cacheService.cacheETag(userId, calendarId, eventUid, result.etag());
          Mono<String> updateCTag = calendarService.updateCTag(userId, calendarId);
          return Mono.when(cacheETag, updateCTag).thenReturn(result);
        });
  }

  @Transactional
  protected Mono<EventSaveResult> doSaveEvent(UserId userId, CalendarId calendarId, String eventUid,
      String icalData, String ifMatch, String ifNoneMatch) {
    return calendarService.getCalendar(userId, calendarId)
        .switchIfEmpty(Mono.error(new CalendarNotFoundException("Calendar " + calendarId + " not found")))
        .flatMap(calendar -> eventRepository.findByUserIdAndCalendarIdAndUid(userId, calendarId, eventUid)
            .flatMap(existing -> {
              if ("*".equals(ifNoneMatch)) {
                return Mono.<EventSaveResult>error(
                    new PreconditionFailedException("Event already exists"));
              }
              if (ifMatch != null && !ETagUtil.matches(ifMatch, existing.getEtag() != null ? existing.getEtag().getValue() : null)) {
                return Mono.<EventSaveResult>error(
                    new PreconditionFailedException("ETag mismatch"));
              }
              return updateEventDb(existing, icalData);
            })
            .switchIfEmpty(Mono.defer(() -> createNewEventDb(userId, calendarId, eventUid, icalData))));
  }

  private Mono<EventSaveResult> createNewEventDb(UserId userId, CalendarId calendarId, String eventUid, String icalData) {
    ParsedEvent parsed = iCalParser.parse(icalData);
    String etag = generateETag(icalData);
    LocalDateTime now = LocalDateTime.now();

    CalendarEvent event = new CalendarEvent();
    event.setUserId(userId);
    event.setCalendarId(calendarId);
    event.setUid(EventUid.of(eventUid));
    event.setIcalData(ICalData.of(icalData));
    event.setEtag(ETag.of(etag));
    event.setSummary(EventSummary.ofNullable(parsed.summary()));
    event.setDescription(EventDescription.ofNullable(parsed.description()));
    event.setLocation(EventLocation.ofNullable(parsed.location()));
    event.setDtstart(parsed.dtstart());
    event.setDtend(parsed.dtend());
    event.setAllDay(parsed.allDay());
    event.setRrule(RecurrenceRule.ofNullable(parsed.rrule()));
    event.setCreatedAt(now);
    event.setUpdatedAt(now);

    return eventRepository.save(event)
        .doOnNext(cacheService::cacheEvent)
        .flatMap(saved -> recordSyncChange(userId, calendarId, eventUid, ChangeType.CREATED)
            .thenReturn(new EventSaveResult(etag, true)));
  }

  private Mono<EventSaveResult> updateEventDb(CalendarEvent existing, String icalData) {
    ParsedEvent parsed = iCalParser.parse(icalData);
    String etag = generateETag(icalData);

    existing.setIcalData(ICalData.of(icalData));
    existing.setEtag(ETag.of(etag));
    existing.setSummary(EventSummary.ofNullable(parsed.summary()));
    existing.setDescription(EventDescription.ofNullable(parsed.description()));
    existing.setLocation(EventLocation.ofNullable(parsed.location()));
    existing.setDtstart(parsed.dtstart());
    existing.setDtend(parsed.dtend());
    existing.setAllDay(parsed.allDay());
    existing.setRrule(RecurrenceRule.ofNullable(parsed.rrule()));
    existing.setUpdatedAt(LocalDateTime.now());

    return eventRepository.save(existing)
        .doOnNext(cacheService::cacheEvent)
        .flatMap(
            saved -> recordSyncChange(saved.getUserId(), saved.getCalendarId(),
                saved.getUid() != null ? saved.getUid().getValue() : null, ChangeType.MODIFIED)
                .thenReturn(new EventSaveResult(etag, false)));
  }

  public Mono<Void> deleteEvent(UserId userId, CalendarId calendarId, String eventUid, String ifMatch) {
    return doDeleteEvent(userId, calendarId, eventUid, ifMatch)
        .then(Mono.defer(() -> {
          Mono<Void> invalidateLocal = Mono.fromRunnable(() -> cacheService.invalidateEvent(userId, calendarId, eventUid));
          Mono<Boolean> invalidateETag = cacheService.invalidateETag(userId, calendarId, eventUid);
          Mono<String> updateCTag = calendarService.updateCTag(userId, calendarId);
          return Mono.when(invalidateLocal, invalidateETag, updateCTag);
        }));
  }

  @Transactional
  protected Mono<Void> doDeleteEvent(UserId userId, CalendarId calendarId, String eventUid, String ifMatch) {
    return eventRepository.findByUserIdAndCalendarIdAndUid(userId, calendarId, eventUid)
        .switchIfEmpty(Mono.error(new EventNotFoundException("Event " + eventUid + " not found")))
        .flatMap(existing -> {
          if (ifMatch != null && !ETagUtil.matches(ifMatch, existing.getEtag() != null ? existing.getEtag().getValue() : null)) {
            return Mono.<Void>error(new PreconditionFailedException("ETag mismatch"));
          }
          return eventRepository.delete(existing)
              .then(recordSyncChange(userId, calendarId, eventUid, ChangeType.DELETED))
              .then();
        });
  }

  private static final HexFormat HEX_FORMAT = HexFormat.of();

  private String generateETag(String data) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] hash = md.digest(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      return HEX_FORMAT.formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 algorithm not found", e);
    }
  }

  private Mono<SyncChange> recordSyncChange(UserId userId, CalendarId calendarId, String eventUid, ChangeType changeType) {
    String syncToken = syncTokenPrefix + System.currentTimeMillis() + "-" + java.util.UUID.randomUUID().toString();
    SyncChange syncChange = new SyncChange();
    syncChange.setUserId(userId);
    syncChange.setCalendarId(calendarId);
    syncChange.setEventUid(eventUid);
    syncChange.setChangeType(changeType);
    syncChange.setSyncToken(syncToken);
    syncChange.setCreatedAt(LocalDateTime.now());
    return syncChangeRepository.save(syncChange);
  }
}
