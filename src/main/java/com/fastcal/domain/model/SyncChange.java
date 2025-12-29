package com.fastcal.domain.model;

import com.fastcal.domain.model.vo.CalendarId;
import com.fastcal.domain.model.vo.UserId;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("sync_changes")
public class SyncChange {

  @Id
  private Long id;

  @Column("user_id")
  private UserId userId;

  @Column("calendar_id")
  private CalendarId calendarId;

  @Column("event_uid")
  private String eventUid;

  @Column("change_type")
  private ChangeType changeType;

  @Column("sync_token")
  private String syncToken;

  @Column("created_at")
  private LocalDateTime createdAt;

  public SyncChange() {
  }

  public SyncChange(Long id, UserId userId, CalendarId calendarId, String eventUid,
      ChangeType changeType, String syncToken, LocalDateTime createdAt) {
    this.id = id;
    this.userId = userId;
    this.calendarId = calendarId;
    this.eventUid = eventUid;
    this.changeType = changeType;
    this.syncToken = syncToken;
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

  public void setUserId(UserId userId) {
    this.userId = userId;
  }

  public CalendarId getCalendarId() {
    return calendarId;
  }

  public void setCalendarId(CalendarId calendarId) {
    this.calendarId = calendarId;
  }

  public String getEventUid() {
    return eventUid;
  }

  public void setEventUid(String eventUid) {
    this.eventUid = eventUid;
  }

  public ChangeType getChangeType() {
    return changeType;
  }

  public void setChangeType(ChangeType changeType) {
    this.changeType = changeType;
  }

  public String getSyncToken() {
    return syncToken;
  }

  public void setSyncToken(String syncToken) {
    this.syncToken = syncToken;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
