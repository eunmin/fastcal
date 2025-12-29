package com.fastcal.domain.repository;

import com.fastcal.domain.model.CalendarEvent;
import com.fastcal.domain.model.vo.CalendarId;
import com.fastcal.domain.model.vo.UserId;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public interface CalendarEventRepository extends ReactiveCrudRepository<CalendarEvent, Long> {

  Flux<CalendarEvent> findByUserIdAndCalendarId(UserId userId, CalendarId calendarId);

  Mono<CalendarEvent> findByUserIdAndCalendarIdAndUid(UserId userId, CalendarId calendarId, String uid);

  @Query("""
      SELECT * FROM calendar_events
      WHERE user_id = :userId
      AND calendar_id = :calendarId
      AND dtstart <= :endTime
      AND (dtend >= :startTime OR dtend IS NULL)
      """)
  Flux<CalendarEvent> findByTimeRange(UserId userId, CalendarId calendarId,
      LocalDateTime startTime, LocalDateTime endTime);

  Mono<Void> deleteByUserIdAndCalendarId(UserId userId, CalendarId calendarId);
}
