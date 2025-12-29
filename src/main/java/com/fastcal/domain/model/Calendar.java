package com.fastcal.domain.model;

import com.fastcal.domain.model.vo.CalendarColor;
import com.fastcal.domain.model.vo.CalendarDescription;
import com.fastcal.domain.model.vo.CalendarDisplayName;
import com.fastcal.domain.model.vo.CalendarId;
import com.fastcal.domain.model.vo.CalendarTimezone;
import com.fastcal.domain.model.vo.CTag;
import com.fastcal.domain.model.vo.SyncToken;
import com.fastcal.domain.model.vo.UserId;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.lang.NonNull;

import java.time.LocalDateTime;
import java.util.Objects;

@Table("calendars")
public class Calendar {

  @Id
  private Long id;

  @Column("user_id")
  private final UserId userId;

  @Column("calendar_id")
  private final CalendarId calendarId;

  @Column("display_name")
  private CalendarDisplayName displayName;

  @Column("description")
  private CalendarDescription description;

  @Column("color")
  private CalendarColor color;

  @Column("timezone")
  private CalendarTimezone timezone;

  @Column("ctag")
  private CTag ctag;

  @Column("sync_token")
  private SyncToken syncToken;

  @CreatedDate
  @Column("created_at")
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column("updated_at")
  private LocalDateTime updatedAt;

  protected Calendar() {
    this.userId = null;
    this.calendarId = null;
  }

  public static Calendar of(@NonNull UserId userId, @NonNull CalendarId calendarId, CalendarDisplayName displayName) {
    Objects.requireNonNull(userId, "userId must not be null");
    Objects.requireNonNull(calendarId, "calendarId must not be null");
    return new Calendar(null, userId, calendarId, displayName, null, null, null, null, null, null, null);
  }

  @PersistenceCreator
  private Calendar(Long id, UserId userId, CalendarId calendarId, CalendarDisplayName displayName,
      CalendarDescription description, CalendarColor color, CalendarTimezone timezone, CTag ctag, SyncToken syncToken,
      LocalDateTime createdAt, LocalDateTime updatedAt) {
    this.id = id;
    this.userId = Objects.requireNonNull(userId, "userId must not be null");
    this.calendarId = Objects.requireNonNull(calendarId, "calendarId must not be null");
    this.displayName = displayName;
    this.description = description;
    this.color = color;
    this.timezone = timezone;
    this.ctag = ctag;
    this.syncToken = syncToken;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
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

  public CalendarDisplayName getDisplayName() {
    return displayName;
  }

  public void setDisplayName(CalendarDisplayName displayName) {
    this.displayName = displayName;
  }

  public CalendarDescription getDescription() {
    return description;
  }

  public void setDescription(CalendarDescription description) {
    this.description = description;
  }

  public CalendarColor getColor() {
    return color;
  }

  public void setColor(CalendarColor color) {
    this.color = color;
  }

  public CalendarTimezone getTimezone() {
    return timezone;
  }

  public void setTimezone(CalendarTimezone timezone) {
    this.timezone = timezone;
  }

  public CTag getCtag() {
    return ctag;
  }

  public void setCtag(CTag ctag) {
    this.ctag = ctag;
  }

  public SyncToken getSyncToken() {
    return syncToken;
  }

  public void setSyncToken(SyncToken syncToken) {
    this.syncToken = syncToken;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
