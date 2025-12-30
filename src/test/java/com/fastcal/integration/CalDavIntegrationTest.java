package com.fastcal.integration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@DisplayName("CalDAV Integration Tests")
class CalDavIntegrationTest {

  private static final String TEST_USER_EMAIL = "test@example.com";
  private static final String TEST_USER_PASSWORD = "password";
  private static final String TEST_CALENDAR_ID = "testcalendar";

  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
      .withDatabaseName("testdb")
      .withUsername("test")
      .withPassword("test")
      .withInitScript("schema.sql");

  static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
      .withExposedPorts(6379);

  @BeforeAll
  static void beforeAll() {
    postgres.start();
    redis.start();
  }

  @AfterAll
  static void afterAll() {
    postgres.stop();
    redis.stop();
  }

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.r2dbc.url", () ->
        "r2dbc:postgresql://" + postgres.getHost() + ":" + postgres.getMappedPort(5432) + "/testdb");
    registry.add("spring.r2dbc.username", postgres::getUsername);
    registry.add("spring.r2dbc.password", postgres::getPassword);
    registry.add("spring.data.redis.host", redis::getHost);
    registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    registry.add("ldap.enabled", () -> "false");
  }

  @TestConfiguration
  static class TestSecurityConfig {
    @Bean
    @Primary
    public ReactiveAuthenticationManager testAuthenticationManager() {
      return authentication -> {
        String username = authentication.getName();
        String password = (String) authentication.getCredentials();

        if (TEST_USER_EMAIL.equals(username) && TEST_USER_PASSWORD.equals(password)) {
          return Mono.just(new UsernamePasswordAuthenticationToken(
              username,
              password,
              List.of(new SimpleGrantedAuthority("ROLE_USER"))
          ));
        }
        return Mono.error(new org.springframework.security.authentication.BadCredentialsException("Invalid credentials"));
      };
    }
  }

  @Autowired
  private WebTestClient webTestClient;

  @Autowired
  private DatabaseClient databaseClient;

  @BeforeEach
  void setUp() {
    webTestClient = webTestClient.mutate()
        .responseTimeout(Duration.ofSeconds(30))
        .build();

    databaseClient.sql("DELETE FROM sync_changes").then().block();
    databaseClient.sql("DELETE FROM calendar_events").then().block();
    databaseClient.sql("DELETE FROM calendars").then().block();
  }

  @Nested
  @DisplayName("Authentication Tests")
  class AuthenticationTests {

    @Test
    @DisplayName("Should return 401 for unauthenticated request to calendars endpoint")
    void shouldReturn401ForUnauthenticatedRequest() {
      webTestClient.method(HttpMethod.valueOf("PROPFIND"))
          .uri("/calendars/{userId}/", TEST_USER_EMAIL)
          .header("Depth", "0")
          .contentType(MediaType.APPLICATION_XML)
          .bodyValue(propfindRequest())
          .exchange()
          .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Should return 401 for wrong password")
    void shouldReturn401ForWrongPassword() {
      webTestClient.method(HttpMethod.valueOf("PROPFIND"))
          .uri("/calendars/{userId}/", TEST_USER_EMAIL)
          .headers(headers -> headers.setBasicAuth(TEST_USER_EMAIL, "wrongpassword"))
          .header("Depth", "0")
          .contentType(MediaType.APPLICATION_XML)
          .bodyValue(propfindRequest())
          .exchange()
          .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Should allow access with valid credentials")
    void shouldAllowAccessWithValidCredentials() {
      webTestClient.method(HttpMethod.valueOf("PROPFIND"))
          .uri("/calendars/{userId}/", TEST_USER_EMAIL)
          .headers(headers -> headers.setBasicAuth(TEST_USER_EMAIL, TEST_USER_PASSWORD))
          .header("Depth", "0")
          .contentType(MediaType.APPLICATION_XML)
          .bodyValue(propfindRequest())
          .exchange()
          .expectStatus().isEqualTo(207);
    }

    @Test
    @DisplayName("Should return 403 when accessing other user's calendar")
    void shouldReturn403ForOtherUserCalendar() {
      webTestClient.method(HttpMethod.valueOf("PROPFIND"))
          .uri("/calendars/{userId}/", "other@example.com")
          .headers(headers -> headers.setBasicAuth(TEST_USER_EMAIL, TEST_USER_PASSWORD))
          .header("Depth", "0")
          .contentType(MediaType.APPLICATION_XML)
          .bodyValue(propfindRequest())
          .exchange()
          .expectStatus().isForbidden();
    }
  }

  @Nested
  @DisplayName("Well-Known Discovery Tests")
  class WellKnownTests {

    @Test
    @DisplayName("Should redirect /.well-known/caldav to /calendars/")
    void shouldRedirectWellKnown() {
      webTestClient.get()
          .uri("/.well-known/caldav")
          .exchange()
          .expectStatus().isEqualTo(301)
          .expectHeader().valueEquals("Location", "/calendars/");
    }

    @Test
    @DisplayName("Should allow well-known without authentication")
    void shouldAllowWellKnownWithoutAuth() {
      webTestClient.get()
          .uri("/.well-known/caldav")
          .exchange()
          .expectStatus().isEqualTo(301);
    }
  }

  @Nested
  @DisplayName("MKCALENDAR Tests")
  class MkCalendarTests {

    @Test
    @DisplayName("Should create calendar with MKCALENDAR")
    void shouldCreateCalendar() {
      webTestClient.method(HttpMethod.valueOf("MKCALENDAR"))
          .uri("/calendars/{userId}/{calendarId}/", TEST_USER_EMAIL, TEST_CALENDAR_ID)
          .headers(headers -> headers.setBasicAuth(TEST_USER_EMAIL, TEST_USER_PASSWORD))
          .contentType(MediaType.APPLICATION_XML)
          .bodyValue(mkcalendarRequest("My Test Calendar"))
          .exchange()
          .expectStatus().isCreated()
          .expectHeader().exists("Location");
    }

    @Test
    @DisplayName("Should return 409 when creating duplicate calendar")
    void shouldReturn409ForDuplicateCalendar() {
      webTestClient.method(HttpMethod.valueOf("MKCALENDAR"))
          .uri("/calendars/{userId}/{calendarId}/", TEST_USER_EMAIL, TEST_CALENDAR_ID)
          .headers(headers -> headers.setBasicAuth(TEST_USER_EMAIL, TEST_USER_PASSWORD))
          .contentType(MediaType.APPLICATION_XML)
          .bodyValue(mkcalendarRequest("My Test Calendar"))
          .exchange()
          .expectStatus().isCreated();

      webTestClient.method(HttpMethod.valueOf("MKCALENDAR"))
          .uri("/calendars/{userId}/{calendarId}/", TEST_USER_EMAIL, TEST_CALENDAR_ID)
          .headers(headers -> headers.setBasicAuth(TEST_USER_EMAIL, TEST_USER_PASSWORD))
          .contentType(MediaType.APPLICATION_XML)
          .bodyValue(mkcalendarRequest("My Test Calendar"))
          .exchange()
          .expectStatus().isEqualTo(409);
    }
  }

  @Nested
  @DisplayName("PROPFIND Tests")
  class PropFindTests {

    @BeforeEach
    void createTestCalendar() {
      webTestClient.method(HttpMethod.valueOf("MKCALENDAR"))
          .uri("/calendars/{userId}/{calendarId}/", TEST_USER_EMAIL, TEST_CALENDAR_ID)
          .headers(headers -> headers.setBasicAuth(TEST_USER_EMAIL, TEST_USER_PASSWORD))
          .contentType(MediaType.APPLICATION_XML)
          .bodyValue(mkcalendarRequest("Test Calendar"))
          .exchange()
          .expectStatus().isCreated();
    }

    @Test
    @DisplayName("Should list calendars with PROPFIND Depth:1")
    void shouldListCalendarsWithDepth1() {
      webTestClient.method(HttpMethod.valueOf("PROPFIND"))
          .uri("/calendars/{userId}/", TEST_USER_EMAIL)
          .headers(headers -> headers.setBasicAuth(TEST_USER_EMAIL, TEST_USER_PASSWORD))
          .header("Depth", "1")
          .contentType(MediaType.APPLICATION_XML)
          .bodyValue(propfindRequest())
          .exchange()
          .expectStatus().isEqualTo(207)
          .expectBody(String.class)
          .value(body -> {
            assertThat(body).contains("multistatus");
            assertThat(body).contains(TEST_CALENDAR_ID);
          });
    }

    @Test
    @DisplayName("Should return calendar properties with PROPFIND")
    void shouldReturnCalendarProperties() {
      webTestClient.method(HttpMethod.valueOf("PROPFIND"))
          .uri("/calendars/{userId}/{calendarId}/", TEST_USER_EMAIL, TEST_CALENDAR_ID)
          .headers(headers -> headers.setBasicAuth(TEST_USER_EMAIL, TEST_USER_PASSWORD))
          .header("Depth", "0")
          .contentType(MediaType.APPLICATION_XML)
          .bodyValue(propfindRequest())
          .exchange()
          .expectStatus().isEqualTo(207)
          .expectBody(String.class)
          .value(body -> {
            assertThat(body).contains("multistatus");
            assertThat(body).contains("Test Calendar");
          });
    }
  }

  @Nested
  @DisplayName("Event CRUD Tests")
  class EventCrudTests {

    private static final String TEST_EVENT_UID = "test-event-123";

    @BeforeEach
    void createTestCalendar() {
      webTestClient.method(HttpMethod.valueOf("MKCALENDAR"))
          .uri("/calendars/{userId}/{calendarId}/", TEST_USER_EMAIL, TEST_CALENDAR_ID)
          .headers(headers -> headers.setBasicAuth(TEST_USER_EMAIL, TEST_USER_PASSWORD))
          .contentType(MediaType.APPLICATION_XML)
          .bodyValue(mkcalendarRequest("Test Calendar"))
          .exchange()
          .expectStatus().isCreated();
    }

    @Test
    @DisplayName("Should create event with PUT")
    void shouldCreateEvent() {
      webTestClient.put()
          .uri("/calendars/{userId}/{calendarId}/{eventUid}.ics",
              TEST_USER_EMAIL, TEST_CALENDAR_ID, TEST_EVENT_UID)
          .headers(headers -> headers.setBasicAuth(TEST_USER_EMAIL, TEST_USER_PASSWORD))
          .contentType(MediaType.parseMediaType("text/calendar"))
          .bodyValue(icalEvent(TEST_EVENT_UID, "Test Meeting"))
          .exchange()
          .expectStatus().isCreated()
          .expectHeader().exists("ETag")
          .expectHeader().exists("Location");
    }

    @Test
    @DisplayName("Should get event with GET")
    void shouldGetEvent() {
      webTestClient.put()
          .uri("/calendars/{userId}/{calendarId}/{eventUid}.ics",
              TEST_USER_EMAIL, TEST_CALENDAR_ID, TEST_EVENT_UID)
          .headers(headers -> headers.setBasicAuth(TEST_USER_EMAIL, TEST_USER_PASSWORD))
          .contentType(MediaType.parseMediaType("text/calendar"))
          .bodyValue(icalEvent(TEST_EVENT_UID, "Test Meeting"))
          .exchange()
          .expectStatus().isCreated();

      webTestClient.get()
          .uri("/calendars/{userId}/{calendarId}/{eventUid}.ics",
              TEST_USER_EMAIL, TEST_CALENDAR_ID, TEST_EVENT_UID)
          .headers(headers -> headers.setBasicAuth(TEST_USER_EMAIL, TEST_USER_PASSWORD))
          .exchange()
          .expectStatus().isOk()
          .expectHeader().contentType("text/calendar;charset=utf-8")
          .expectHeader().exists("ETag")
          .expectBody(String.class)
          .value(body -> {
            assertThat(body).contains("BEGIN:VCALENDAR");
            assertThat(body).contains("UID:" + TEST_EVENT_UID);
            assertThat(body).contains("Test Meeting");
          });
    }

    @Test
    @DisplayName("Should update event with PUT")
    void shouldUpdateEvent() {
      String etag = webTestClient.put()
          .uri("/calendars/{userId}/{calendarId}/{eventUid}.ics",
              TEST_USER_EMAIL, TEST_CALENDAR_ID, TEST_EVENT_UID)
          .headers(headers -> headers.setBasicAuth(TEST_USER_EMAIL, TEST_USER_PASSWORD))
          .contentType(MediaType.parseMediaType("text/calendar"))
          .bodyValue(icalEvent(TEST_EVENT_UID, "Test Meeting"))
          .exchange()
          .expectStatus().isCreated()
          .returnResult(Void.class)
          .getResponseHeaders()
          .getETag();

      webTestClient.put()
          .uri("/calendars/{userId}/{calendarId}/{eventUid}.ics",
              TEST_USER_EMAIL, TEST_CALENDAR_ID, TEST_EVENT_UID)
          .headers(headers -> {
            headers.setBasicAuth(TEST_USER_EMAIL, TEST_USER_PASSWORD);
            headers.setIfMatch(etag);
          })
          .contentType(MediaType.parseMediaType("text/calendar"))
          .bodyValue(icalEvent(TEST_EVENT_UID, "Updated Meeting"))
          .exchange()
          .expectStatus().isNoContent();

      webTestClient.get()
          .uri("/calendars/{userId}/{calendarId}/{eventUid}.ics",
              TEST_USER_EMAIL, TEST_CALENDAR_ID, TEST_EVENT_UID)
          .headers(headers -> headers.setBasicAuth(TEST_USER_EMAIL, TEST_USER_PASSWORD))
          .exchange()
          .expectStatus().isOk()
          .expectBody(String.class)
          .value(body -> assertThat(body).contains("Updated Meeting"));
    }

    @Test
    @DisplayName("Should delete event with DELETE")
    void shouldDeleteEvent() {
      webTestClient.put()
          .uri("/calendars/{userId}/{calendarId}/{eventUid}.ics",
              TEST_USER_EMAIL, TEST_CALENDAR_ID, TEST_EVENT_UID)
          .headers(headers -> headers.setBasicAuth(TEST_USER_EMAIL, TEST_USER_PASSWORD))
          .contentType(MediaType.parseMediaType("text/calendar"))
          .bodyValue(icalEvent(TEST_EVENT_UID, "Test Meeting"))
          .exchange()
          .expectStatus().isCreated();

      webTestClient.delete()
          .uri("/calendars/{userId}/{calendarId}/{eventUid}.ics",
              TEST_USER_EMAIL, TEST_CALENDAR_ID, TEST_EVENT_UID)
          .headers(headers -> headers.setBasicAuth(TEST_USER_EMAIL, TEST_USER_PASSWORD))
          .exchange()
          .expectStatus().isNoContent();

      webTestClient.get()
          .uri("/calendars/{userId}/{calendarId}/{eventUid}.ics",
              TEST_USER_EMAIL, TEST_CALENDAR_ID, TEST_EVENT_UID)
          .headers(headers -> headers.setBasicAuth(TEST_USER_EMAIL, TEST_USER_PASSWORD))
          .exchange()
          .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("Should return 404 for non-existent event")
    void shouldReturn404ForNonExistentEvent() {
      webTestClient.get()
          .uri("/calendars/{userId}/{calendarId}/{eventUid}.ics",
              TEST_USER_EMAIL, TEST_CALENDAR_ID, "non-existent")
          .headers(headers -> headers.setBasicAuth(TEST_USER_EMAIL, TEST_USER_PASSWORD))
          .exchange()
          .expectStatus().isNotFound();
    }
  }

  @Nested
  @DisplayName("REPORT Tests")
  class ReportTests {

    private static final String TEST_EVENT_UID = "report-test-event";

    @BeforeEach
    void createTestData() {
      webTestClient.method(HttpMethod.valueOf("MKCALENDAR"))
          .uri("/calendars/{userId}/{calendarId}/", TEST_USER_EMAIL, TEST_CALENDAR_ID)
          .headers(headers -> headers.setBasicAuth(TEST_USER_EMAIL, TEST_USER_PASSWORD))
          .contentType(MediaType.APPLICATION_XML)
          .bodyValue(mkcalendarRequest("Test Calendar"))
          .exchange()
          .expectStatus().isCreated();

      webTestClient.put()
          .uri("/calendars/{userId}/{calendarId}/{eventUid}.ics",
              TEST_USER_EMAIL, TEST_CALENDAR_ID, TEST_EVENT_UID)
          .headers(headers -> headers.setBasicAuth(TEST_USER_EMAIL, TEST_USER_PASSWORD))
          .contentType(MediaType.parseMediaType("text/calendar"))
          .bodyValue(icalEvent(TEST_EVENT_UID, "Report Test Event"))
          .exchange()
          .expectStatus().isCreated();
    }

    @Test
    @DisplayName("Should return events with calendar-query REPORT")
    void shouldReturnEventsWithCalendarQuery() {
      webTestClient.method(HttpMethod.valueOf("REPORT"))
          .uri("/calendars/{userId}/{calendarId}/", TEST_USER_EMAIL, TEST_CALENDAR_ID)
          .headers(headers -> headers.setBasicAuth(TEST_USER_EMAIL, TEST_USER_PASSWORD))
          .header("Depth", "1")
          .contentType(MediaType.APPLICATION_XML)
          .bodyValue(calendarQueryReport())
          .exchange()
          .expectStatus().isEqualTo(207)
          .expectBody(String.class)
          .value(body -> {
            assertThat(body).contains("multistatus");
            assertThat(body).contains(TEST_EVENT_UID);
          });
    }

    @Test
    @DisplayName("Should return events with calendar-multiget REPORT")
    void shouldReturnEventsWithMultiget() {
      String multigetReport = """
          <?xml version="1.0" encoding="UTF-8"?>
          <C:calendar-multiget xmlns:D="DAV:" xmlns:C="urn:ietf:params:xml:ns:caldav">
            <D:prop>
              <D:getetag/>
              <C:calendar-data/>
            </D:prop>
            <D:href>/calendars/%s/%s/%s.ics</D:href>
          </C:calendar-multiget>
          """.formatted(TEST_USER_EMAIL, TEST_CALENDAR_ID, TEST_EVENT_UID);

      webTestClient.method(HttpMethod.valueOf("REPORT"))
          .uri("/calendars/{userId}/{calendarId}/", TEST_USER_EMAIL, TEST_CALENDAR_ID)
          .headers(headers -> headers.setBasicAuth(TEST_USER_EMAIL, TEST_USER_PASSWORD))
          .header("Depth", "1")
          .contentType(MediaType.APPLICATION_XML)
          .bodyValue(multigetReport)
          .exchange()
          .expectStatus().isEqualTo(207)
          .expectBody(String.class)
          .value(body -> {
            assertThat(body).contains("multistatus");
            assertThat(body).contains("BEGIN:VCALENDAR");
          });
    }
  }

  @Nested
  @DisplayName("Calendar Delete Tests")
  class CalendarDeleteTests {

    @Test
    @DisplayName("Should delete calendar with DELETE")
    void shouldDeleteCalendar() {
      webTestClient.method(HttpMethod.valueOf("MKCALENDAR"))
          .uri("/calendars/{userId}/{calendarId}/", TEST_USER_EMAIL, TEST_CALENDAR_ID)
          .headers(headers -> headers.setBasicAuth(TEST_USER_EMAIL, TEST_USER_PASSWORD))
          .contentType(MediaType.APPLICATION_XML)
          .bodyValue(mkcalendarRequest("Test Calendar"))
          .exchange()
          .expectStatus().isCreated();

      webTestClient.delete()
          .uri("/calendars/{userId}/{calendarId}/", TEST_USER_EMAIL, TEST_CALENDAR_ID)
          .headers(headers -> headers.setBasicAuth(TEST_USER_EMAIL, TEST_USER_PASSWORD))
          .exchange()
          .expectStatus().isNoContent();

      webTestClient.method(HttpMethod.valueOf("PROPFIND"))
          .uri("/calendars/{userId}/{calendarId}/", TEST_USER_EMAIL, TEST_CALENDAR_ID)
          .headers(headers -> headers.setBasicAuth(TEST_USER_EMAIL, TEST_USER_PASSWORD))
          .header("Depth", "0")
          .contentType(MediaType.APPLICATION_XML)
          .bodyValue(propfindRequest())
          .exchange()
          .expectStatus().isNotFound();
    }
  }

  @Nested
  @DisplayName("OPTIONS Tests")
  class OptionsTests {

    @Test
    @DisplayName("Should return DAV capabilities with OPTIONS")
    void shouldReturnDavCapabilities() {
      webTestClient.options()
          .uri("/calendars/{userId}/", TEST_USER_EMAIL)
          .exchange()
          .expectStatus().isOk()
          .expectHeader().exists("DAV")
          .expectHeader().value("Allow", allow -> {
            assertThat(allow).contains("PROPFIND");
            assertThat(allow).contains("MKCALENDAR");
          });
    }
  }

  private String propfindRequest() {
    return """
        <?xml version="1.0" encoding="UTF-8"?>
        <D:propfind xmlns:D="DAV:">
          <D:prop>
            <D:displayname/>
            <D:resourcetype/>
          </D:prop>
        </D:propfind>
        """;
  }

  private String mkcalendarRequest(String displayName) {
    return """
        <?xml version="1.0" encoding="UTF-8"?>
        <C:mkcalendar xmlns:D="DAV:" xmlns:C="urn:ietf:params:xml:ns:caldav">
          <D:set>
            <D:prop>
              <D:displayname>%s</D:displayname>
            </D:prop>
          </D:set>
        </C:mkcalendar>
        """.formatted(displayName);
  }

  private String icalEvent(String uid, String summary) {
    return """
        BEGIN:VCALENDAR
        VERSION:2.0
        PRODID:-//FastCal//Test//EN
        BEGIN:VEVENT
        UID:%s
        DTSTART:20251230T100000Z
        DTEND:20251230T110000Z
        SUMMARY:%s
        DESCRIPTION:Test event description
        END:VEVENT
        END:VCALENDAR
        """.formatted(uid, summary);
  }

  private String calendarQueryReport() {
    return """
        <?xml version="1.0" encoding="UTF-8"?>
        <C:calendar-query xmlns:D="DAV:" xmlns:C="urn:ietf:params:xml:ns:caldav">
          <D:prop>
            <D:getetag/>
            <C:calendar-data/>
          </D:prop>
          <C:filter>
            <C:comp-filter name="VCALENDAR">
              <C:comp-filter name="VEVENT"/>
            </C:comp-filter>
          </C:filter>
        </C:calendar-query>
        """;
  }
}
