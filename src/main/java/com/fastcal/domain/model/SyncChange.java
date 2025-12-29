package com.fastcal.domain.model;

import com.fastcal.domain.model.vo.CalendarId;
import com.fastcal.domain.model.vo.EventUid;
import com.fastcal.domain.model.vo.SyncToken;
import com.fastcal.domain.model.vo.UserId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.lang.NonNull;

import java.time.LocalDateTime;
import java.util.Objects;

@Table("sync_changes")
public class SyncChange {

  @Id
  private Long id;

  @Column("user_id")
  private final UserId userId;

  @Column("calendar_id")
  private final CalendarId calendarId;

  @Column("event_uid")
  private final EventUid eventUid;

  @Column("change_type")
  private final ChangeType changeType;

  @Column("sync_token")
  private final SyncToken syncToken;

  @CreatedDate
  @Column("created_at")
  private LocalDateTime createdAt;

  protected SyncChange() {
    this.userId = null;
    this.calendarId = null;
    this.eventUid = null;
    this.changeType = null;
    this.syncToken = null;
  }

  public static SyncChange of(@NonNull UserId userId, @NonNull CalendarId calendarId,
      @NonNull EventUid eventUid, @NonNull ChangeType changeType, @NonNull SyncToken syncToken) {
    Objects.requireNonNull(userId, "userId must not be null");
    Objects.requireNonNull(calendarId, "calendarId must not be null");
    Objects.requireNonNull(eventUid, "eventUid must not be null");
    Objects.requireNonNull(changeType, "changeType must not be null");
    Objects.requireNonNull(syncToken, "syncToken must not be null");
    return new SyncChange(null, userId, calendarId, eventUid, changeType, syncToken, null);
  }

  @PersistenceCreator
  private SyncChange(Long id, UserId userId, CalendarId calendarId, EventUid eventUid,
      ChangeType changeType, SyncToken syncToken, LocalDateTime createdAt) {
    this.id = id;
    this.userId = Objects.requireNonNull(userId, "userId must not be null");
    this.calendarId = Objects.requireNonNull(calendarId, "calendarId must not be null");
    this.eventUid = Objects.requireNonNull(eventUid, "eventUid must not be null");
    this.changeType = Objects.requireNonNull(changeType, "changeType must not be null");
    this.syncToken = Objects.requireNonNull(syncToken, "syncToken must not be null");
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public UserId getUserId() {
    return userId;
  }

  public CalendarId getCalendarId() {
    return calendarId;
  }

  public EventUid getEventUid() {
    return eventUid;
  }

  public ChangeType getChangeType() {
    return changeType;
  }

  public SyncToken getSyncToken() {
    return syncToken;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }
}
