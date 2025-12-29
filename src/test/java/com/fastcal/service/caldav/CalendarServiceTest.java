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
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.util.ReflectionTestUtils;
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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CalendarServiceTest {

  @Mock
  private CalendarRepository calendarRepository;

  @Mock
  private CacheService cacheService;

  private MkCalendarParser mkCalendarParser;

  private CalendarService calendarService;

  private static final UserId USER_ID = UserId.of("user1@example.com");
  private static final CalendarId CALENDAR_ID = CalendarId.of("calendar1");

  @BeforeEach
  void setUp() {
    mkCalendarParser = new MkCalendarParser();
    calendarService = new CalendarService(calendarRepository, cacheService, mkCalendarParser);
    ReflectionTestUtils.setField(calendarService, "syncTokenPrefix", "http://fastcal.com/ns/sync/");
  }

  private Calendar createTestCalendar() {
    Calendar calendar = Calendar.of(USER_ID, CALENDAR_ID, CalendarDisplayName.of("Test Calendar"));
    calendar.setId(1L);
    calendar.setDescription(CalendarDescription.of("Test Description"));
    calendar.setColor(CalendarColor.of("#FF0000"));
    calendar.setTimezone(CalendarTimezone.of("Asia/Seoul"));
    calendar.setCtag(CTag.of("ctag-123"));
    calendar.setSyncToken(SyncToken.of("http://fastcal.com/ns/sync/123456"));
    calendar.setCreatedAt(LocalDateTime.now());
    calendar.setUpdatedAt(LocalDateTime.now());
    return calendar;
  }

  @Nested
  @DisplayName("getCalendar()")
  class GetCalendarTests {

    @Test
    @DisplayName("should return cached calendar when available")
    void shouldReturnCachedCalendar() {
      Calendar cachedCalendar = createTestCalendar();
      when(cacheService.getCalendar(USER_ID, CALENDAR_ID)).thenReturn(cachedCalendar);

      StepVerifier.create(calendarService.getCalendar(USER_ID, CALENDAR_ID))
          .expectNext(cachedCalendar)
          .verifyComplete();

      verify(calendarRepository, never()).findByUserIdAndCalendarId(any(UserId.class), any(CalendarId.class));
    }

    @Test
    @DisplayName("should fetch from repository when not cached")
    void shouldFetchFromRepositoryWhenNotCached() {
      Calendar calendar = createTestCalendar();
      when(cacheService.getCalendar(USER_ID, CALENDAR_ID)).thenReturn(null);
      when(calendarRepository.findByUserIdAndCalendarId(USER_ID, CALENDAR_ID))
          .thenReturn(Mono.just(calendar));

      StepVerifier.create(calendarService.getCalendar(USER_ID, CALENDAR_ID))
          .expectNext(calendar)
          .verifyComplete();

      verify(cacheService).cacheCalendar(calendar);
    }

    @Test
    @DisplayName("should return empty when calendar not found")
    void shouldReturnEmptyWhenNotFound() {
      when(cacheService.getCalendar(USER_ID, CALENDAR_ID)).thenReturn(null);
      when(calendarRepository.findByUserIdAndCalendarId(USER_ID, CALENDAR_ID))
          .thenReturn(Mono.empty());

      StepVerifier.create(calendarService.getCalendar(USER_ID, CALENDAR_ID))
          .verifyComplete();
    }
  }

  @Nested
  @DisplayName("getCalendarsByUser()")
  class GetCalendarsByUserTests {

    @Test
    @DisplayName("should return all calendars for user")
    void shouldReturnAllCalendars() {
      Calendar calendar1 = createTestCalendar();
      Calendar calendar2 = Calendar.of(USER_ID, CalendarId.of("calendar2"), CalendarDisplayName.of("Second Calendar"));
      calendar2.setId(2L);

      when(calendarRepository.findByUserId(USER_ID))
          .thenReturn(Flux.just(calendar1, calendar2));

      StepVerifier.create(calendarService.getCalendarsByUser(USER_ID))
          .expectNext(calendar1)
          .expectNext(calendar2)
          .verifyComplete();
    }

    @Test
    @DisplayName("should return empty when user has no calendars")
    void shouldReturnEmptyWhenNoCalendars() {
      when(calendarRepository.findByUserId(USER_ID))
          .thenReturn(Flux.empty());

      StepVerifier.create(calendarService.getCalendarsByUser(USER_ID))
          .verifyComplete();
    }
  }

  @Nested
  @DisplayName("createCalendar()")
  class CreateCalendarTests {

    private static final String MKCALENDAR_XML = """
        <?xml version="1.0" encoding="UTF-8"?>
        <C:mkcalendar xmlns:D="DAV:" xmlns:C="urn:ietf:params:xml:ns:caldav" xmlns:A="http://apple.com/ns/ical/">
            <D:set>
                <D:prop>
                    <D:displayname>New Calendar</D:displayname>
                    <C:calendar-description>My new calendar</C:calendar-description>
                    <A:calendar-color>#0000FF</A:calendar-color>
                    <C:calendar-timezone>Asia/Seoul</C:calendar-timezone>
                </D:prop>
            </D:set>
        </C:mkcalendar>
        """;

    @Test
    @DisplayName("should create new calendar with all properties")
    void shouldCreateNewCalendar() {
      when(calendarRepository.findByUserIdAndCalendarId(USER_ID, CALENDAR_ID))
          .thenReturn(Mono.empty());
      when(calendarRepository.save(any(Calendar.class))).thenAnswer(invocation -> {
        Calendar cal = invocation.getArgument(0);
        cal.setId(1L);
        return Mono.just(cal);
      });
      when(cacheService.cacheCTag(eq(USER_ID), eq(CALENDAR_ID), anyString()))
          .thenReturn(Mono.just(true));

      StepVerifier.create(calendarService.createCalendar(USER_ID, CALENDAR_ID, MKCALENDAR_XML))
          .assertNext(calendar -> {
            assertThat(calendar.getUserId().getValue()).isEqualTo(USER_ID.getValue());
            assertThat(calendar.getCalendarId()).isEqualTo(CALENDAR_ID);
            assertThat(calendar.getDisplayName().getValue()).isEqualTo("New Calendar");
            assertThat(calendar.getDescription().getValue()).isEqualTo("My new calendar");
            assertThat(calendar.getColor().getValue()).isEqualTo("#0000FF");
            assertThat(calendar.getCtag()).isNotNull();
            assertThat(calendar.getSyncToken().getValue()).startsWith("http://fastcal.com/ns/sync/");
          })
          .verifyComplete();

      verify(cacheService).cacheCalendar(any(Calendar.class));
    }

    @Test
    @DisplayName("should use calendarId as displayName when not provided in XML")
    void shouldUseCalendarIdAsDisplayName() {
      String xmlWithoutDisplayName = """
          <?xml version="1.0" encoding="UTF-8"?>
          <C:mkcalendar xmlns:D="DAV:" xmlns:C="urn:ietf:params:xml:ns:caldav">
          </C:mkcalendar>
          """;

      when(calendarRepository.findByUserIdAndCalendarId(USER_ID, CALENDAR_ID))
          .thenReturn(Mono.empty());
      when(calendarRepository.save(any(Calendar.class))).thenAnswer(invocation -> {
        Calendar cal = invocation.getArgument(0);
        cal.setId(1L);
        return Mono.just(cal);
      });
      when(cacheService.cacheCTag(eq(USER_ID), eq(CALENDAR_ID), anyString()))
          .thenReturn(Mono.just(true));

      StepVerifier.create(calendarService.createCalendar(USER_ID, CALENDAR_ID, xmlWithoutDisplayName))
          .assertNext(calendar -> {
            assertThat(calendar.getDisplayName().getValue()).isEqualTo(CALENDAR_ID.getValue());
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("should throw CalendarAlreadyExistsException when calendar exists")
    void shouldThrowWhenCalendarExists() {
      Calendar existingCalendar = createTestCalendar();
      when(calendarRepository.findByUserIdAndCalendarId(USER_ID, CALENDAR_ID))
          .thenReturn(Mono.just(existingCalendar));

      StepVerifier.create(calendarService.createCalendar(USER_ID, CALENDAR_ID, MKCALENDAR_XML))
          .expectError(CalendarAlreadyExistsException.class)
          .verify();

      verify(calendarRepository, never()).save(any());
    }

    @Test
    @DisplayName("should handle empty XML gracefully")
    void shouldHandleEmptyXml() {
      when(calendarRepository.findByUserIdAndCalendarId(USER_ID, CALENDAR_ID))
          .thenReturn(Mono.empty());
      when(calendarRepository.save(any(Calendar.class))).thenAnswer(invocation -> {
        Calendar cal = invocation.getArgument(0);
        cal.setId(1L);
        return Mono.just(cal);
      });
      when(cacheService.cacheCTag(eq(USER_ID), eq(CALENDAR_ID), anyString()))
          .thenReturn(Mono.just(true));

      StepVerifier.create(calendarService.createCalendar(USER_ID, CALENDAR_ID, ""))
          .assertNext(calendar -> {
            assertThat(calendar.getDisplayName().getValue()).isEqualTo(CALENDAR_ID.getValue());
            assertThat(calendar.getDescription()).isNull();
            assertThat(calendar.getColor()).isNull();
          })
          .verifyComplete();
    }
  }

  @Nested
  @DisplayName("deleteCalendar()")
  class DeleteCalendarTests {

    @Test
    @DisplayName("should delete calendar successfully")
    void shouldDeleteCalendar() {
      Calendar calendar = createTestCalendar();

      when(calendarRepository.findByUserIdAndCalendarId(USER_ID, CALENDAR_ID))
          .thenReturn(Mono.just(calendar));
      when(calendarRepository.delete(calendar)).thenReturn(Mono.empty());
      when(cacheService.invalidateCTag(USER_ID, CALENDAR_ID)).thenReturn(Mono.just(true));

      StepVerifier.create(calendarService.deleteCalendar(USER_ID, CALENDAR_ID))
          .verifyComplete();

      verify(cacheService).invalidateCalendar(USER_ID, CALENDAR_ID);
      verify(cacheService).invalidateCTag(USER_ID, CALENDAR_ID);
    }

    @Test
    @DisplayName("should throw CalendarNotFoundException when calendar not exists")
    void shouldThrowWhenCalendarNotExists() {
      when(calendarRepository.findByUserIdAndCalendarId(USER_ID, CALENDAR_ID))
          .thenReturn(Mono.empty());

      StepVerifier.create(calendarService.deleteCalendar(USER_ID, CALENDAR_ID))
          .expectError(CalendarNotFoundException.class)
          .verify();

      verify(calendarRepository, never()).delete(any());
    }
  }

  @Nested
  @DisplayName("updateCTag()")
  class UpdateCTagTests {

    @Test
    @DisplayName("should update CTag and SyncToken")
    void shouldUpdateCTagAndSyncToken() {
      when(calendarRepository.updateCTagAndSyncToken(eq(USER_ID), eq(CALENDAR_ID), anyString(), anyString()))
          .thenReturn(Mono.just(1));
      when(cacheService.cacheCTag(eq(USER_ID), eq(CALENDAR_ID), anyString()))
          .thenReturn(Mono.just(true));

      StepVerifier.create(calendarService.updateCTag(USER_ID, CALENDAR_ID))
          .assertNext(newCTag -> {
            assertThat(newCTag).isNotNull();
            assertThat(newCTag).isNotEmpty();
          })
          .verifyComplete();

      verify(cacheService).invalidateCalendar(USER_ID, CALENDAR_ID);
    }

    @Test
    @DisplayName("should return new CTag even when cache fails")
    void shouldReturnNewCTagWhenCacheFails() {
      when(calendarRepository.updateCTagAndSyncToken(eq(USER_ID), eq(CALENDAR_ID), anyString(), anyString()))
          .thenReturn(Mono.just(1));
      when(cacheService.cacheCTag(eq(USER_ID), eq(CALENDAR_ID), anyString()))
          .thenReturn(Mono.just(false));

      StepVerifier.create(calendarService.updateCTag(USER_ID, CALENDAR_ID))
          .assertNext(newCTag -> assertThat(newCTag).isNotNull())
          .verifyComplete();
    }
  }

  @Nested
  @DisplayName("updateCalendarProperties()")
  class UpdateCalendarPropertiesTests {

    private static final String PROPPATCH_XML = """
        <?xml version="1.0" encoding="UTF-8"?>
        <D:propertyupdate xmlns:D="DAV:" xmlns:C="urn:ietf:params:xml:ns:caldav" xmlns:A="http://apple.com/ns/ical/">
            <D:set>
                <D:prop>
                    <D:displayname>Updated Calendar</D:displayname>
                    <C:calendar-description>Updated description</C:calendar-description>
                    <A:calendar-color>#00FF00</A:calendar-color>
                </D:prop>
            </D:set>
        </D:propertyupdate>
        """;

    @Test
    @DisplayName("should update calendar properties")
    void shouldUpdateCalendarProperties() {
      Calendar calendar = createTestCalendar();
      CTag originalCTag = calendar.getCtag();

      when(calendarRepository.findByUserIdAndCalendarId(USER_ID, CALENDAR_ID))
          .thenReturn(Mono.just(calendar));
      when(calendarRepository.save(any(Calendar.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
      when(cacheService.cacheCTag(eq(USER_ID), eq(CALENDAR_ID), anyString()))
          .thenReturn(Mono.just(true));

      StepVerifier.create(calendarService.updateCalendarProperties(USER_ID, CALENDAR_ID, PROPPATCH_XML))
          .assertNext(updated -> {
            assertThat(updated.getDisplayName().getValue()).isEqualTo("Updated Calendar");
            assertThat(updated.getDescription().getValue()).isEqualTo("Updated description");
            assertThat(updated.getColor().getValue()).isEqualTo("#00FF00");
            assertThat(updated.getCtag()).isNotEqualTo(originalCTag);
          })
          .verifyComplete();

      verify(cacheService).cacheCalendar(any(Calendar.class));
    }

    @Test
    @DisplayName("should only update provided properties")
    void shouldOnlyUpdateProvidedProperties() {
      Calendar calendar = createTestCalendar();
      CalendarDescription originalDescription = calendar.getDescription();
      CalendarColor originalColor = calendar.getColor();

      String partialXml = """
          <?xml version="1.0" encoding="UTF-8"?>
          <D:propertyupdate xmlns:D="DAV:">
              <D:set>
                  <D:prop>
                      <D:displayname>Only Name Updated</D:displayname>
                  </D:prop>
              </D:set>
          </D:propertyupdate>
          """;

      when(calendarRepository.findByUserIdAndCalendarId(USER_ID, CALENDAR_ID))
          .thenReturn(Mono.just(calendar));
      when(calendarRepository.save(any(Calendar.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
      when(cacheService.cacheCTag(eq(USER_ID), eq(CALENDAR_ID), anyString()))
          .thenReturn(Mono.just(true));

      StepVerifier.create(calendarService.updateCalendarProperties(USER_ID, CALENDAR_ID, partialXml))
          .assertNext(updated -> {
            assertThat(updated.getDisplayName().getValue()).isEqualTo("Only Name Updated");
            assertThat(updated.getDescription()).isEqualTo(originalDescription);
            assertThat(updated.getColor()).isEqualTo(originalColor);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("should throw CalendarNotFoundException when calendar not exists")
    void shouldThrowWhenCalendarNotExists() {
      when(calendarRepository.findByUserIdAndCalendarId(USER_ID, CALENDAR_ID))
          .thenReturn(Mono.empty());

      StepVerifier.create(calendarService.updateCalendarProperties(USER_ID, CALENDAR_ID, PROPPATCH_XML))
          .expectError(CalendarNotFoundException.class)
          .verify();

      verify(calendarRepository, never()).save(any());
    }

    @Test
    @DisplayName("should handle empty PROPPATCH XML")
    void shouldHandleEmptyProppatchXml() {
      Calendar calendar = createTestCalendar();
      String originalName = calendar.getDisplayName() != null ? calendar.getDisplayName().getValue() : null;

      when(calendarRepository.findByUserIdAndCalendarId(USER_ID, CALENDAR_ID))
          .thenReturn(Mono.just(calendar));
      when(calendarRepository.save(any(Calendar.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
      when(cacheService.cacheCTag(eq(USER_ID), eq(CALENDAR_ID), anyString()))
          .thenReturn(Mono.just(true));

      StepVerifier.create(calendarService.updateCalendarProperties(USER_ID, CALENDAR_ID, ""))
          .assertNext(updated -> {
            String updatedName = updated.getDisplayName() != null ? updated.getDisplayName().getValue() : null;
            assertThat(updatedName).isEqualTo(originalName);
          })
          .verifyComplete();
    }
  }

  @Nested
  @DisplayName("Edge Cases")
  class EdgeCaseTests {

    @Test
    @DisplayName("should propagate error when repository save fails")
    void shouldPropagateRepositorySaveError() {
      String xml = "<D:displayname>Test</D:displayname>";

      when(calendarRepository.findByUserIdAndCalendarId(USER_ID, CALENDAR_ID))
          .thenReturn(Mono.empty());
      when(calendarRepository.save(any(Calendar.class)))
          .thenReturn(Mono.error(new RuntimeException("Database error")));

      StepVerifier.create(calendarService.createCalendar(USER_ID, CALENDAR_ID, xml))
          .expectError(RuntimeException.class)
          .verify();
    }

    @Test
    @DisplayName("should propagate error when repository delete fails")
    void shouldPropagateRepositoryDeleteError() {
      Calendar calendar = createTestCalendar();

      when(calendarRepository.findByUserIdAndCalendarId(USER_ID, CALENDAR_ID))
          .thenReturn(Mono.just(calendar));
      when(calendarRepository.delete(calendar))
          .thenReturn(Mono.error(new RuntimeException("Delete failed")));

      StepVerifier.create(calendarService.deleteCalendar(USER_ID, CALENDAR_ID))
          .expectError(RuntimeException.class)
          .verify();
    }

    @Test
    @DisplayName("should handle special characters in XML properties")
    void shouldHandleSpecialCharactersInXml() {
      String xmlWithSpecialChars = """
          <?xml version="1.0" encoding="UTF-8"?>
          <C:mkcalendar xmlns:D="DAV:" xmlns:C="urn:ietf:params:xml:ns:caldav">
              <D:set>
                  <D:prop>
                      <D:displayname>Calendar &amp; Events &lt;Test&gt;</D:displayname>
                      <C:calendar-description>Description with "quotes" and 'apostrophes'</C:calendar-description>
                  </D:prop>
              </D:set>
          </C:mkcalendar>
          """;

      when(calendarRepository.findByUserIdAndCalendarId(USER_ID, CALENDAR_ID))
          .thenReturn(Mono.empty());
      when(calendarRepository.save(any(Calendar.class))).thenAnswer(invocation -> {
        Calendar cal = invocation.getArgument(0);
        cal.setId(1L);
        return Mono.just(cal);
      });
      when(cacheService.cacheCTag(eq(USER_ID), eq(CALENDAR_ID), anyString()))
          .thenReturn(Mono.just(true));

      StepVerifier.create(calendarService.createCalendar(USER_ID, CALENDAR_ID, xmlWithSpecialChars))
          .assertNext(calendar -> {
            // StAX parser decodes XML entities
            assertThat(calendar.getDisplayName().getValue()).isEqualTo("Calendar & Events <Test>");
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("should handle unicode in XML properties")
    void shouldHandleUnicodeInXml() {
      String xmlWithUnicode = """
          <?xml version="1.0" encoding="UTF-8"?>
          <C:mkcalendar xmlns:D="DAV:" xmlns:C="urn:ietf:params:xml:ns:caldav">
              <D:set>
                  <D:prop>
                      <D:displayname>캘린더 - 한글 테스트</D:displayname>
                      <C:calendar-description>日本語説明 🎉</C:calendar-description>
                  </D:prop>
              </D:set>
          </C:mkcalendar>
          """;

      when(calendarRepository.findByUserIdAndCalendarId(USER_ID, CALENDAR_ID))
          .thenReturn(Mono.empty());
      when(calendarRepository.save(any(Calendar.class))).thenAnswer(invocation -> {
        Calendar cal = invocation.getArgument(0);
        cal.setId(1L);
        return Mono.just(cal);
      });
      when(cacheService.cacheCTag(eq(USER_ID), eq(CALENDAR_ID), anyString()))
          .thenReturn(Mono.just(true));

      StepVerifier.create(calendarService.createCalendar(USER_ID, CALENDAR_ID, xmlWithUnicode))
          .assertNext(calendar -> {
            assertThat(calendar.getDisplayName().getValue()).isEqualTo("캘린더 - 한글 테스트");
            assertThat(calendar.getDescription().getValue()).contains("日本語説明");
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("should generate unique CTag for each operation")
    void shouldGenerateUniqueCTag() {
      when(calendarRepository.updateCTagAndSyncToken(eq(USER_ID), eq(CALENDAR_ID), anyString(), anyString()))
          .thenReturn(Mono.just(1));
      when(cacheService.cacheCTag(eq(USER_ID), eq(CALENDAR_ID), anyString()))
          .thenReturn(Mono.just(true));

      String ctag1 = calendarService.updateCTag(USER_ID, CALENDAR_ID).block();
      String ctag2 = calendarService.updateCTag(USER_ID, CALENDAR_ID).block();

      assertThat(ctag1).isNotEqualTo(ctag2);
    }

    @Test
    @DisplayName("should verify CTag is valid UUID format")
    void shouldVerifyCTagIsUuidFormat() {
      when(calendarRepository.updateCTagAndSyncToken(eq(USER_ID), eq(CALENDAR_ID), anyString(), anyString()))
          .thenReturn(Mono.just(1));
      when(cacheService.cacheCTag(eq(USER_ID), eq(CALENDAR_ID), anyString()))
          .thenReturn(Mono.just(true));

      ArgumentCaptor<String> ctagCaptor = ArgumentCaptor.forClass(String.class);

      calendarService.updateCTag(USER_ID, CALENDAR_ID).block();

      verify(calendarRepository).updateCTagAndSyncToken(eq(USER_ID), eq(CALENDAR_ID), ctagCaptor.capture(),
          anyString());
      String capturedCTag = ctagCaptor.getValue();

      assertThat(capturedCTag).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    @DisplayName("should verify SyncToken format")
    void shouldVerifySyncTokenFormat() {
      when(calendarRepository.updateCTagAndSyncToken(eq(USER_ID), eq(CALENDAR_ID), anyString(), anyString()))
          .thenReturn(Mono.just(1));
      when(cacheService.cacheCTag(eq(USER_ID), eq(CALENDAR_ID), anyString()))
          .thenReturn(Mono.just(true));

      ArgumentCaptor<String> syncTokenCaptor = ArgumentCaptor.forClass(String.class);

      calendarService.updateCTag(USER_ID, CALENDAR_ID).block();

      verify(calendarRepository).updateCTagAndSyncToken(eq(USER_ID), eq(CALENDAR_ID), anyString(),
          syncTokenCaptor.capture());
      String capturedSyncToken = syncTokenCaptor.getValue();

      assertThat(capturedSyncToken).startsWith("http://fastcal.com/ns/sync/");
    }

    @Test
    @DisplayName("should handle cache invalidation failure gracefully on delete")
    void shouldHandleCacheInvalidationFailureOnDelete() {
      Calendar calendar = createTestCalendar();

      when(calendarRepository.findByUserIdAndCalendarId(USER_ID, CALENDAR_ID))
          .thenReturn(Mono.just(calendar));
      when(calendarRepository.delete(calendar)).thenReturn(Mono.empty());
      when(cacheService.invalidateCTag(USER_ID, CALENDAR_ID))
          .thenReturn(Mono.error(new RuntimeException("Cache error")));

      StepVerifier.create(calendarService.deleteCalendar(USER_ID, CALENDAR_ID))
          .expectError(RuntimeException.class)
          .verify();
    }

    @Test
    @DisplayName("should handle null XML gracefully")
    void shouldHandleNullXml() {
      when(calendarRepository.findByUserIdAndCalendarId(USER_ID, CALENDAR_ID))
          .thenReturn(Mono.empty());
      when(calendarRepository.save(any(Calendar.class))).thenAnswer(invocation -> {
        Calendar cal = invocation.getArgument(0);
        cal.setId(1L);
        return Mono.just(cal);
      });
      when(cacheService.cacheCTag(eq(USER_ID), eq(CALENDAR_ID), anyString()))
          .thenReturn(Mono.just(true));

      // MkCalendarParser handles null gracefully, using calendarId as displayName
      StepVerifier.create(calendarService.createCalendar(USER_ID, CALENDAR_ID, null))
          .assertNext(calendar -> {
            assertThat(calendar.getDisplayName().getValue()).isEqualTo(CALENDAR_ID.getValue());
            assertThat(calendar.getDescription()).isNull();
          })
          .verifyComplete();
    }
  }
}
