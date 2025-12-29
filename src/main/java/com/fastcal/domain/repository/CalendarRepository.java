package com.fastcal.domain.repository;

import com.fastcal.domain.model.Calendar;
import com.fastcal.domain.model.vo.CalendarId;
import com.fastcal.domain.model.vo.UserId;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface CalendarRepository extends ReactiveCrudRepository<Calendar, Long> {

  Flux<Calendar> findByUserId(UserId userId);

  Mono<Calendar> findByUserIdAndCalendarId(UserId userId, CalendarId calendarId);

  @Modifying
  @Query("UPDATE calendars SET ctag = :ctag, sync_token = :syncToken, updated_at = NOW() " +
      "WHERE user_id = :userId AND calendar_id = :calendarId")
  Mono<Integer> updateCTagAndSyncToken(UserId userId, CalendarId calendarId, String ctag, String syncToken);
}
