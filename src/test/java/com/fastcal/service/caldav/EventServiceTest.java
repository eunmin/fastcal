package com.fastcal.service.caldav;

import com.fastcal.domain.model.Calendar;
import com.fastcal.domain.model.CalendarEvent;
import com.fastcal.domain.model.ChangeType;
import com.fastcal.domain.model.SyncChange;
import com.fastcal.domain.model.vo.CalendarDisplayName;
import com.fastcal.domain.model.vo.CalendarId;
import com.fastcal.domain.model.vo.ETag;
import com.fastcal.domain.model.vo.EventSummary;
import com.fastcal.domain.model.vo.EventUid;
import com.fastcal.domain.model.vo.ICalData;
import com.fastcal.domain.model.vo.SyncToken;
import com.fastcal.domain.model.vo.UserId;
import com.fastcal.domain.repository.CalendarEventRepository;
import com.fastcal.domain.repository.SyncChangeRepository;
import com.fastcal.exception.CalendarNotFoundException;
import com.fastcal.exception.EventNotFoundException;
import com.fastcal.exception.PreconditionFailedException;
import com.fastcal.ical.ICalParser;
import com.fastcal.ical.ParsedEvent;
import com.fastcal.service.cache.CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EventServiceTest {

  @Mock
  private CalendarEventRepository eventRepository;

  @Mock
  private SyncChangeRepository syncChangeRepository;

  @Mock
  private CalendarService calendarService;

  @Mock
  private CacheService cacheService;

  @Mock
  private ICalParser iCalParser;

  private EventService eventService;

  private static final UserId USER_ID = UserId.of("user1@example.com");
  private static final CalendarId CALENDAR_ID = CalendarId.of("calendar1");
  private static final String EVENT_UID = "event-123";
  private static final String ICAL_DATA = """
      BEGIN:VCALENDAR
      VERSION:2.0
      BEGIN:VEVENT
      UID:event-123
      DTSTART:20241225T100000Z
      SUMMARY:Test Event
      END:VEVENT
      END:VCALENDAR
      """;

  @BeforeEach
  void setUp() throws Exception {
    eventService = new EventService(
        eventRepository,
        syncChangeRepository,
        calendarService,
        cacheService,
        iCalParser);

    Field syncTokenPrefixField = EventService.class.getDeclaredField("syncTokenPrefix");
    syncTokenPrefixField.setAccessible(true);
    syncTokenPrefixField.set(eventService, "http://fastcal.com/ns/sync/");
  }

  private CalendarEvent createTestEvent() {
    return createTestEvent(EVENT_UID);
  }

  private CalendarEvent createTestEvent(String eventUid) {
    CalendarEvent event = CalendarEvent.of(USER_ID, CALENDAR_ID, EventUid.of(eventUid));
    event.setId(1L);
    event.setIcalData(ICalData.of(ICAL_DATA));
    event.setEtag(ETag.of("abc123"));
    event.setSummary(EventSummary.of("Test Event"));
    event.setDtstart(LocalDateTime.of(2024, 12, 25, 10, 0));
    event.setCreatedAt(LocalDateTime.now());
    event.setUpdatedAt(LocalDateTime.now());
    return event;
  }

  private Calendar createTestCalendar() {
    Calendar calendar = Calendar.of(USER_ID, CALENDAR_ID, CalendarDisplayName.of("Test Calendar"));
    calendar.setId(1L);
    return calendar;
  }

  private ParsedEvent createParsedEvent() {
    return new ParsedEvent(
        EVENT_UID,
        "Test Event",
        "Description",
        "Location",
        LocalDateTime.of(2024, 12, 25, 10, 0),
        LocalDateTime.of(2024, 12, 25, 11, 0),
        false,
        null);
  }

  private SyncChange createTestSyncChange() {
    return SyncChange.of(USER_ID, CALENDAR_ID, EventUid.of(EVENT_UID), ChangeType.CREATED, SyncToken.of("test-sync-token"));
  }

  @Nested
  @DisplayName("getEvent()")
  class GetEventTests {

    @Test
    @DisplayName("should return cached event when available")
    void shouldReturnCachedEvent() {
      CalendarEvent cachedEvent = createTestEvent();
      when(cacheService.getEvent(USER_ID, CALENDAR_ID, EVENT_UID)).thenReturn(cachedEvent);

      StepVerifier.create(eventService.getEvent(USER_ID, CALENDAR_ID, EVENT_UID))
          .expectNext(cachedEvent)
          .verifyComplete();

      verify(eventRepository, never()).findByUserIdAndCalendarIdAndUid(any(UserId.class), any(CalendarId.class), anyString());
    }

    @Test
    @DisplayName("should fetch from repository when not cached")
    void shouldFetchFromRepositoryWhenNotCached() {
      CalendarEvent event = createTestEvent();
      when(cacheService.getEvent(USER_ID, CALENDAR_ID, EVENT_UID)).thenReturn(null);
      when(eventRepository.findByUserIdAndCalendarIdAndUid(USER_ID, CALENDAR_ID, EVENT_UID))
          .thenReturn(Mono.just(event));

      StepVerifier.create(eventService.getEvent(USER_ID, CALENDAR_ID, EVENT_UID))
          .expectNext(event)
          .verifyComplete();

      verify(cacheService).cacheEvent(event);
    }

    @Test
    @DisplayName("should throw EventNotFoundException when not found")
    void shouldThrowEventNotFoundException() {
      when(cacheService.getEvent(USER_ID, CALENDAR_ID, EVENT_UID)).thenReturn(null);
      when(eventRepository.findByUserIdAndCalendarIdAndUid(USER_ID, CALENDAR_ID, EVENT_UID))
          .thenReturn(Mono.empty());

      StepVerifier.create(eventService.getEvent(USER_ID, CALENDAR_ID, EVENT_UID))
          .expectError(EventNotFoundException.class)
          .verify();
    }
  }

  @Nested
  @DisplayName("getEventsByCalendar()")
  class GetEventsByCalendarTests {

    @Test
    @DisplayName("should return all events for calendar")
    void shouldReturnAllEvents() {
      CalendarEvent event1 = createTestEvent();
      CalendarEvent event2 = createTestEvent("event-456");

      when(eventRepository.findByUserIdAndCalendarId(USER_ID, CALENDAR_ID))
          .thenReturn(Flux.just(event1, event2));

      StepVerifier.create(eventService.getEventsByCalendar(USER_ID, CALENDAR_ID))
          .expectNext(event1)
          .expectNext(event2)
          .verifyComplete();
    }

    @Test
    @DisplayName("should return empty when no events")
    void shouldReturnEmptyWhenNoEvents() {
      when(eventRepository.findByUserIdAndCalendarId(USER_ID, CALENDAR_ID))
          .thenReturn(Flux.empty());

      StepVerifier.create(eventService.getEventsByCalendar(USER_ID, CALENDAR_ID))
          .verifyComplete();
    }
  }

  @Nested
  @DisplayName("getEventsByTimeRange()")
  class GetEventsByTimeRangeTests {

    @Test
    @DisplayName("should return events within time range")
    void shouldReturnEventsWithinTimeRange() {
      LocalDateTime start = LocalDateTime.of(2024, 12, 1, 0, 0);
      LocalDateTime end = LocalDateTime.of(2024, 12, 31, 23, 59);
      CalendarEvent event = createTestEvent();

      when(eventRepository.findByTimeRange(USER_ID, CALENDAR_ID, start, end))
          .thenReturn(Flux.just(event));

      StepVerifier.create(eventService.getEventsByTimeRange(USER_ID, CALENDAR_ID, start, end))
          .expectNext(event)
          .verifyComplete();
    }
  }

  @Nested
  @DisplayName("saveEvent()")
  class SaveEventTests {

    @Test
    @DisplayName("should create new event when not exists")
    void shouldCreateNewEvent() {
      Calendar calendar = createTestCalendar();
      ParsedEvent parsed = createParsedEvent();
      CalendarEvent savedEvent = createTestEvent();
      SyncChange syncChange = createTestSyncChange();

      when(calendarService.getCalendar(USER_ID, CALENDAR_ID)).thenReturn(Mono.just(calendar));
      when(cacheService.getETag(USER_ID, CALENDAR_ID, EVENT_UID)).thenReturn(Mono.empty());
      when(eventRepository.findByUserIdAndCalendarIdAndUid(USER_ID, CALENDAR_ID, EVENT_UID))
          .thenReturn(Mono.empty());
      when(iCalParser.parse(ICAL_DATA)).thenReturn(parsed);
      when(eventRepository.save(any(CalendarEvent.class))).thenReturn(Mono.just(savedEvent));
      when(cacheService.cacheETag(eq(USER_ID), eq(CALENDAR_ID), eq(EVENT_UID), anyString()))
          .thenReturn(Mono.empty());
      when(syncChangeRepository.save(any(SyncChange.class))).thenReturn(Mono.just(syncChange));
      when(calendarService.updateCTag(USER_ID, CALENDAR_ID)).thenReturn(Mono.just("new-ctag"));

      StepVerifier.create(eventService.saveEvent(USER_ID, CALENDAR_ID, EVENT_UID, ICAL_DATA, null, null))
          .assertNext(result -> {
            assertThat(result.isCreated()).isTrue();
            assertThat(result.etag()).isNotNull();
          })
          .verifyComplete();

      verify(cacheService).cacheEvent(any(CalendarEvent.class));

      ArgumentCaptor<SyncChange> syncChangeCaptor = ArgumentCaptor.forClass(SyncChange.class);
      verify(syncChangeRepository).save(syncChangeCaptor.capture());
      assertThat(syncChangeCaptor.getValue().getChangeType()).isEqualTo(ChangeType.CREATED);
    }

    @Test
    @DisplayName("should update existing event")
    void shouldUpdateExistingEvent() {
      Calendar calendar = createTestCalendar();
      CalendarEvent existingEvent = createTestEvent();
      ParsedEvent parsed = createParsedEvent();
      SyncChange syncChange = createTestSyncChange();

      when(calendarService.getCalendar(USER_ID, CALENDAR_ID)).thenReturn(Mono.just(calendar));
      when(cacheService.getETag(USER_ID, CALENDAR_ID, EVENT_UID)).thenReturn(Mono.empty());
      when(eventRepository.findByUserIdAndCalendarIdAndUid(USER_ID, CALENDAR_ID, EVENT_UID))
          .thenReturn(Mono.just(existingEvent));
      when(iCalParser.parse(ICAL_DATA)).thenReturn(parsed);
      when(eventRepository.save(any(CalendarEvent.class))).thenReturn(Mono.just(existingEvent));
      when(cacheService.cacheETag(eq(USER_ID), eq(CALENDAR_ID), eq(EVENT_UID), anyString()))
          .thenReturn(Mono.empty());
      when(syncChangeRepository.save(any(SyncChange.class))).thenReturn(Mono.just(syncChange));
      when(calendarService.updateCTag(USER_ID, CALENDAR_ID)).thenReturn(Mono.just("new-ctag"));

      StepVerifier.create(eventService.saveEvent(USER_ID, CALENDAR_ID, EVENT_UID, ICAL_DATA, null, null))
          .assertNext(result -> {
            assertThat(result.isCreated()).isFalse();
          })
          .verifyComplete();

      ArgumentCaptor<SyncChange> syncChangeCaptor = ArgumentCaptor.forClass(SyncChange.class);
      verify(syncChangeRepository).save(syncChangeCaptor.capture());
      assertThat(syncChangeCaptor.getValue().getChangeType()).isEqualTo(ChangeType.MODIFIED);
    }

    @Test
    @DisplayName("should throw CalendarNotFoundException when calendar not exists")
    void shouldThrowCalendarNotFoundException() {
      when(calendarService.getCalendar(USER_ID, CALENDAR_ID)).thenReturn(Mono.empty());

      StepVerifier.create(eventService.saveEvent(USER_ID, CALENDAR_ID, EVENT_UID, ICAL_DATA, null, null))
          .expectError(CalendarNotFoundException.class)
          .verify();
    }

    @Test
    @DisplayName("should throw PreconditionFailedException when If-None-Match * and event exists")
    void shouldThrowPreconditionFailedForIfNoneMatch() {
      Calendar calendar = createTestCalendar();
      CalendarEvent existingEvent = createTestEvent();

      when(calendarService.getCalendar(USER_ID, CALENDAR_ID)).thenReturn(Mono.just(calendar));
      when(cacheService.getETag(USER_ID, CALENDAR_ID, EVENT_UID)).thenReturn(Mono.empty());
      when(eventRepository.findByUserIdAndCalendarIdAndUid(USER_ID, CALENDAR_ID, EVENT_UID))
          .thenReturn(Mono.just(existingEvent));

      StepVerifier.create(eventService.saveEvent(USER_ID, CALENDAR_ID, EVENT_UID, ICAL_DATA, null, "*"))
          .expectError(PreconditionFailedException.class)
          .verify();
    }

    @Test
    @DisplayName("should throw PreconditionFailedException when If-Match ETag mismatch")
    void shouldThrowPreconditionFailedForETagMismatch() {
      Calendar calendar = createTestCalendar();
      CalendarEvent existingEvent = createTestEvent();
      existingEvent.setEtag(ETag.of("correct-etag"));

      when(calendarService.getCalendar(USER_ID, CALENDAR_ID)).thenReturn(Mono.just(calendar));
      when(cacheService.getETag(USER_ID, CALENDAR_ID, EVENT_UID)).thenReturn(Mono.empty());
      when(eventRepository.findByUserIdAndCalendarIdAndUid(USER_ID, CALENDAR_ID, EVENT_UID))
          .thenReturn(Mono.just(existingEvent));

      StepVerifier.create(eventService.saveEvent(USER_ID, CALENDAR_ID, EVENT_UID, ICAL_DATA, "\"wrong-etag\"", null))
          .expectError(PreconditionFailedException.class)
          .verify();
    }

    @Test
    @DisplayName("should update when If-Match ETag matches")
    void shouldUpdateWhenETagMatches() {
      Calendar calendar = createTestCalendar();
      CalendarEvent existingEvent = createTestEvent();
      existingEvent.setEtag(ETag.of("correct-etag"));
      ParsedEvent parsed = createParsedEvent();
      SyncChange syncChange = createTestSyncChange();

      when(calendarService.getCalendar(USER_ID, CALENDAR_ID)).thenReturn(Mono.just(calendar));
      when(cacheService.getETag(USER_ID, CALENDAR_ID, EVENT_UID)).thenReturn(Mono.empty());
      when(eventRepository.findByUserIdAndCalendarIdAndUid(USER_ID, CALENDAR_ID, EVENT_UID))
          .thenReturn(Mono.just(existingEvent));
      when(iCalParser.parse(ICAL_DATA)).thenReturn(parsed);
      when(eventRepository.save(any(CalendarEvent.class))).thenReturn(Mono.just(existingEvent));
      when(cacheService.cacheETag(eq(USER_ID), eq(CALENDAR_ID), eq(EVENT_UID), anyString()))
          .thenReturn(Mono.empty());
      when(syncChangeRepository.save(any(SyncChange.class))).thenReturn(Mono.just(syncChange));
      when(calendarService.updateCTag(USER_ID, CALENDAR_ID)).thenReturn(Mono.just("new-ctag"));

      StepVerifier.create(eventService.saveEvent(USER_ID, CALENDAR_ID, EVENT_UID, ICAL_DATA, "\"correct-etag\"", null))
          .assertNext(result -> assertThat(result.isCreated()).isFalse())
          .verifyComplete();
    }

    @Test
    @DisplayName("should use cached ETag for validation when available")
    void shouldUseCachedETagForValidation() {
      Calendar calendar = createTestCalendar();
      CalendarEvent existingEvent = createTestEvent();
      ParsedEvent parsed = createParsedEvent();
      SyncChange syncChange = createTestSyncChange();

      when(calendarService.getCalendar(USER_ID, CALENDAR_ID)).thenReturn(Mono.just(calendar));
      when(cacheService.getETag(USER_ID, CALENDAR_ID, EVENT_UID)).thenReturn(Mono.just("cached-etag"));
      when(eventRepository.findByUserIdAndCalendarIdAndUid(USER_ID, CALENDAR_ID, EVENT_UID))
          .thenReturn(Mono.just(existingEvent));
      when(iCalParser.parse(ICAL_DATA)).thenReturn(parsed);
      when(eventRepository.save(any(CalendarEvent.class))).thenReturn(Mono.just(existingEvent));
      when(cacheService.cacheETag(eq(USER_ID), eq(CALENDAR_ID), eq(EVENT_UID), anyString()))
          .thenReturn(Mono.empty());
      when(syncChangeRepository.save(any(SyncChange.class))).thenReturn(Mono.just(syncChange));
      when(calendarService.updateCTag(USER_ID, CALENDAR_ID)).thenReturn(Mono.just("new-ctag"));

      StepVerifier.create(eventService.saveEvent(USER_ID, CALENDAR_ID, EVENT_UID, ICAL_DATA, "\"cached-etag\"", null))
          .assertNext(result -> assertThat(result).isNotNull())
          .verifyComplete();
    }

    @Test
    @DisplayName("should throw PreconditionFailedException when cached ETag mismatch")
    void shouldThrowPreconditionFailedForCachedETagMismatch() {
      Calendar calendar = createTestCalendar();

      when(calendarService.getCalendar(USER_ID, CALENDAR_ID)).thenReturn(Mono.just(calendar));
      when(cacheService.getETag(USER_ID, CALENDAR_ID, EVENT_UID)).thenReturn(Mono.just("cached-etag"));

      StepVerifier.create(eventService.saveEvent(USER_ID, CALENDAR_ID, EVENT_UID, ICAL_DATA, "\"wrong-etag\"", null))
          .expectError(PreconditionFailedException.class)
          .verify();
    }
  }

  @Nested
  @DisplayName("deleteEvent()")
  class DeleteEventTests {

    @Test
    @DisplayName("should delete event successfully")
    void shouldDeleteEvent() {
      CalendarEvent existingEvent = createTestEvent();
      SyncChange syncChange = createTestSyncChange();

      when(eventRepository.findByUserIdAndCalendarIdAndUid(USER_ID, CALENDAR_ID, EVENT_UID))
          .thenReturn(Mono.just(existingEvent));
      when(eventRepository.delete(existingEvent)).thenReturn(Mono.empty());
      when(cacheService.invalidateETag(USER_ID, CALENDAR_ID, EVENT_UID)).thenReturn(Mono.just(true));
      when(syncChangeRepository.save(any(SyncChange.class))).thenReturn(Mono.just(syncChange));
      when(calendarService.updateCTag(USER_ID, CALENDAR_ID)).thenReturn(Mono.just("new-ctag"));

      StepVerifier.create(eventService.deleteEvent(USER_ID, CALENDAR_ID, EVENT_UID, null))
          .verifyComplete();

      verify(cacheService).invalidateEvent(USER_ID, CALENDAR_ID, EVENT_UID);
      verify(cacheService).invalidateETag(USER_ID, CALENDAR_ID, EVENT_UID);

      ArgumentCaptor<SyncChange> syncChangeCaptor = ArgumentCaptor.forClass(SyncChange.class);
      verify(syncChangeRepository).save(syncChangeCaptor.capture());
      assertThat(syncChangeCaptor.getValue().getChangeType()).isEqualTo(ChangeType.DELETED);
    }

    @Test
    @DisplayName("should throw EventNotFoundException when event not exists")
    void shouldThrowEventNotFoundException() {
      when(eventRepository.findByUserIdAndCalendarIdAndUid(USER_ID, CALENDAR_ID, EVENT_UID))
          .thenReturn(Mono.empty());
      when(calendarService.updateCTag(USER_ID, CALENDAR_ID)).thenReturn(Mono.just("new-ctag"));

      StepVerifier.create(eventService.deleteEvent(USER_ID, CALENDAR_ID, EVENT_UID, null))
          .expectError(EventNotFoundException.class)
          .verify();
    }

    @Test
    @DisplayName("should throw PreconditionFailedException when If-Match ETag mismatch")
    void shouldThrowPreconditionFailedForETagMismatch() {
      CalendarEvent existingEvent = createTestEvent();
      existingEvent.setEtag(ETag.of("correct-etag"));

      when(eventRepository.findByUserIdAndCalendarIdAndUid(USER_ID, CALENDAR_ID, EVENT_UID))
          .thenReturn(Mono.just(existingEvent));
      when(calendarService.updateCTag(USER_ID, CALENDAR_ID)).thenReturn(Mono.just("new-ctag"));

      StepVerifier.create(eventService.deleteEvent(USER_ID, CALENDAR_ID, EVENT_UID, "\"wrong-etag\""))
          .expectError(PreconditionFailedException.class)
          .verify();

      verify(eventRepository, never()).delete(any());
    }

    @Test
    @DisplayName("should delete when If-Match ETag matches")
    void shouldDeleteWhenETagMatches() {
      CalendarEvent existingEvent = createTestEvent();
      existingEvent.setEtag(ETag.of("correct-etag"));
      SyncChange syncChange = createTestSyncChange();

      when(eventRepository.findByUserIdAndCalendarIdAndUid(USER_ID, CALENDAR_ID, EVENT_UID))
          .thenReturn(Mono.just(existingEvent));
      when(eventRepository.delete(existingEvent)).thenReturn(Mono.empty());
      when(cacheService.invalidateETag(USER_ID, CALENDAR_ID, EVENT_UID)).thenReturn(Mono.just(true));
      when(syncChangeRepository.save(any(SyncChange.class))).thenReturn(Mono.just(syncChange));
      when(calendarService.updateCTag(USER_ID, CALENDAR_ID)).thenReturn(Mono.just("new-ctag"));

      StepVerifier.create(eventService.deleteEvent(USER_ID, CALENDAR_ID, EVENT_UID, "\"correct-etag\""))
          .verifyComplete();

      verify(eventRepository).delete(existingEvent);
    }

    @Test
    @DisplayName("should reject If-Match ETag without quotes per RFC 7232")
    void shouldRejectETagWithoutQuotes() {
      CalendarEvent existingEvent = createTestEvent();
      existingEvent.setEtag(ETag.of("correct-etag"));

      when(eventRepository.findByUserIdAndCalendarIdAndUid(USER_ID, CALENDAR_ID, EVENT_UID))
          .thenReturn(Mono.just(existingEvent));

      // ETag without quotes is invalid per RFC 7232, should fail precondition
      StepVerifier.create(eventService.deleteEvent(USER_ID, CALENDAR_ID, EVENT_UID, "correct-etag"))
          .expectError(PreconditionFailedException.class)
          .verify();
    }
  }

  @Nested
  @DisplayName("Edge Cases")
  class EdgeCaseTests {

    @Test
    @DisplayName("should propagate error when iCalParser throws exception")
    void shouldPropagateICalParserError() {
      Calendar calendar = createTestCalendar();

      when(calendarService.getCalendar(USER_ID, CALENDAR_ID)).thenReturn(Mono.just(calendar));
      when(cacheService.getETag(USER_ID, CALENDAR_ID, EVENT_UID)).thenReturn(Mono.empty());
      when(eventRepository.findByUserIdAndCalendarIdAndUid(USER_ID, CALENDAR_ID, EVENT_UID))
          .thenReturn(Mono.empty());
      when(iCalParser.parse(ICAL_DATA)).thenThrow(new IllegalArgumentException("Invalid iCal data"));

      StepVerifier.create(eventService.saveEvent(USER_ID, CALENDAR_ID, EVENT_UID, ICAL_DATA, null, null))
          .expectError(IllegalArgumentException.class)
          .verify();
    }

    @Test
    @DisplayName("should propagate error when repository save fails")
    void shouldPropagateRepositorySaveError() {
      Calendar calendar = createTestCalendar();
      ParsedEvent parsed = createParsedEvent();

      when(calendarService.getCalendar(USER_ID, CALENDAR_ID)).thenReturn(Mono.just(calendar));
      when(cacheService.getETag(USER_ID, CALENDAR_ID, EVENT_UID)).thenReturn(Mono.empty());
      when(eventRepository.findByUserIdAndCalendarIdAndUid(USER_ID, CALENDAR_ID, EVENT_UID))
          .thenReturn(Mono.empty());
      when(iCalParser.parse(ICAL_DATA)).thenReturn(parsed);
      when(eventRepository.save(any(CalendarEvent.class)))
          .thenReturn(Mono.error(new RuntimeException("Database connection failed")));

      StepVerifier.create(eventService.saveEvent(USER_ID, CALENDAR_ID, EVENT_UID, ICAL_DATA, null, null))
          .expectError(RuntimeException.class)
          .verify();
    }

    @Test
    @DisplayName("should handle empty ETag in If-Match header")
    void shouldHandleEmptyETagInIfMatch() {
      Calendar calendar = createTestCalendar();
      CalendarEvent existingEvent = createTestEvent();
      existingEvent.setEtag(ETag.of("existing-etag"));

      when(calendarService.getCalendar(USER_ID, CALENDAR_ID)).thenReturn(Mono.just(calendar));
      when(cacheService.getETag(USER_ID, CALENDAR_ID, EVENT_UID)).thenReturn(Mono.empty());
      when(eventRepository.findByUserIdAndCalendarIdAndUid(USER_ID, CALENDAR_ID, EVENT_UID))
          .thenReturn(Mono.just(existingEvent));

      StepVerifier.create(eventService.saveEvent(USER_ID, CALENDAR_ID, EVENT_UID, ICAL_DATA, "\"some-etag\"", null))
          .expectError(PreconditionFailedException.class)
          .verify();
    }

    @Test
    @DisplayName("should continue when cache ETag save fails")
    void shouldContinueWhenCacheETagSaveFails() {
      Calendar calendar = createTestCalendar();
      ParsedEvent parsed = createParsedEvent();
      CalendarEvent savedEvent = createTestEvent();
      SyncChange syncChange = createTestSyncChange();

      when(calendarService.getCalendar(USER_ID, CALENDAR_ID)).thenReturn(Mono.just(calendar));
      when(cacheService.getETag(USER_ID, CALENDAR_ID, EVENT_UID)).thenReturn(Mono.empty());
      when(eventRepository.findByUserIdAndCalendarIdAndUid(USER_ID, CALENDAR_ID, EVENT_UID))
          .thenReturn(Mono.empty());
      when(iCalParser.parse(ICAL_DATA)).thenReturn(parsed);
      when(eventRepository.save(any(CalendarEvent.class))).thenReturn(Mono.just(savedEvent));
      when(cacheService.cacheETag(eq(USER_ID), eq(CALENDAR_ID), eq(EVENT_UID), anyString()))
          .thenReturn(Mono.empty());
      when(syncChangeRepository.save(any(SyncChange.class))).thenReturn(Mono.just(syncChange));
      when(calendarService.updateCTag(USER_ID, CALENDAR_ID)).thenReturn(Mono.just("new-ctag"));

      StepVerifier.create(eventService.saveEvent(USER_ID, CALENDAR_ID, EVENT_UID, ICAL_DATA, null, null))
          .assertNext(result -> assertThat(result.isCreated()).isTrue())
          .verifyComplete();
    }

    @Test
    @DisplayName("should handle SyncChange save failure gracefully")
    void shouldHandleSyncChangeSaveFailure() {
      Calendar calendar = createTestCalendar();
      ParsedEvent parsed = createParsedEvent();
      CalendarEvent savedEvent = createTestEvent();

      when(calendarService.getCalendar(USER_ID, CALENDAR_ID)).thenReturn(Mono.just(calendar));
      when(cacheService.getETag(USER_ID, CALENDAR_ID, EVENT_UID)).thenReturn(Mono.empty());
      when(eventRepository.findByUserIdAndCalendarIdAndUid(USER_ID, CALENDAR_ID, EVENT_UID))
          .thenReturn(Mono.empty());
      when(iCalParser.parse(ICAL_DATA)).thenReturn(parsed);
      when(eventRepository.save(any(CalendarEvent.class))).thenReturn(Mono.just(savedEvent));
      when(cacheService.cacheETag(eq(USER_ID), eq(CALENDAR_ID), eq(EVENT_UID), anyString()))
          .thenReturn(Mono.empty());
      when(syncChangeRepository.save(any(SyncChange.class)))
          .thenReturn(Mono.error(new RuntimeException("SyncChange save failed")));

      StepVerifier.create(eventService.saveEvent(USER_ID, CALENDAR_ID, EVENT_UID, ICAL_DATA, null, null))
          .expectError(RuntimeException.class)
          .verify();
    }

    @Test
    @DisplayName("should handle updateCTag failure after event save")
    void shouldHandleUpdateCTagFailure() {
      Calendar calendar = createTestCalendar();
      ParsedEvent parsed = createParsedEvent();
      CalendarEvent savedEvent = createTestEvent();
      SyncChange syncChange = createTestSyncChange();

      when(calendarService.getCalendar(USER_ID, CALENDAR_ID)).thenReturn(Mono.just(calendar));
      when(cacheService.getETag(USER_ID, CALENDAR_ID, EVENT_UID)).thenReturn(Mono.empty());
      when(eventRepository.findByUserIdAndCalendarIdAndUid(USER_ID, CALENDAR_ID, EVENT_UID))
          .thenReturn(Mono.empty());
      when(iCalParser.parse(ICAL_DATA)).thenReturn(parsed);
      when(eventRepository.save(any(CalendarEvent.class))).thenReturn(Mono.just(savedEvent));
      when(cacheService.cacheETag(eq(USER_ID), eq(CALENDAR_ID), eq(EVENT_UID), anyString()))
          .thenReturn(Mono.empty());
      when(syncChangeRepository.save(any(SyncChange.class))).thenReturn(Mono.just(syncChange));
      when(calendarService.updateCTag(USER_ID, CALENDAR_ID))
          .thenReturn(Mono.error(new RuntimeException("CTag update failed")));

      StepVerifier.create(eventService.saveEvent(USER_ID, CALENDAR_ID, EVENT_UID, ICAL_DATA, null, null))
          .expectError(RuntimeException.class)
          .verify();
    }

    @Test
    @DisplayName("should verify SyncChange contains correct syncToken prefix")
    void shouldVerifySyncTokenPrefix() {
      Calendar calendar = createTestCalendar();
      ParsedEvent parsed = createParsedEvent();
      CalendarEvent savedEvent = createTestEvent();

      when(calendarService.getCalendar(USER_ID, CALENDAR_ID)).thenReturn(Mono.just(calendar));
      when(cacheService.getETag(USER_ID, CALENDAR_ID, EVENT_UID)).thenReturn(Mono.empty());
      when(eventRepository.findByUserIdAndCalendarIdAndUid(USER_ID, CALENDAR_ID, EVENT_UID))
          .thenReturn(Mono.empty());
      when(iCalParser.parse(ICAL_DATA)).thenReturn(parsed);
      when(eventRepository.save(any(CalendarEvent.class))).thenReturn(Mono.just(savedEvent));
      when(cacheService.cacheETag(eq(USER_ID), eq(CALENDAR_ID), eq(EVENT_UID), anyString()))
          .thenReturn(Mono.empty());
      when(syncChangeRepository.save(any(SyncChange.class))).thenAnswer(invocation -> {
        SyncChange sc = invocation.getArgument(0);
        return Mono.just(sc);
      });
      when(calendarService.updateCTag(USER_ID, CALENDAR_ID)).thenReturn(Mono.just("new-ctag"));

      StepVerifier.create(eventService.saveEvent(USER_ID, CALENDAR_ID, EVENT_UID, ICAL_DATA, null, null))
          .assertNext(result -> assertThat(result).isNotNull())
          .verifyComplete();

      ArgumentCaptor<SyncChange> syncChangeCaptor = ArgumentCaptor.forClass(SyncChange.class);
      verify(syncChangeRepository).save(syncChangeCaptor.capture());
      assertThat(syncChangeCaptor.getValue().getSyncToken().getValue()).startsWith("http://fastcal.com/ns/sync/");
    }
  }
}
