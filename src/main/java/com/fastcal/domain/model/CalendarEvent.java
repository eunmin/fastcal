package com.fastcal.domain.model;

import com.fastcal.domain.model.vo.CalendarId;
import com.fastcal.domain.model.vo.ETag;
import com.fastcal.domain.model.vo.EventDescription;
import com.fastcal.domain.model.vo.EventLocation;
import com.fastcal.domain.model.vo.EventSummary;
import com.fastcal.domain.model.vo.EventUid;
import com.fastcal.domain.model.vo.ICalData;
import com.fastcal.domain.model.vo.RecurrenceRule;
import com.fastcal.domain.model.vo.UserId;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("calendar_events")
public class CalendarEvent {

  @Id
  private Long id;

  @Column("user_id")
  private UserId userId;

  @Column("calendar_id")
  private CalendarId calendarId;

  @Column("uid")
  private EventUid uid;

  @Column("ical_data")
  private ICalData icalData;

  @Column("etag")
  private ETag etag;

  @Column("summary")
  private EventSummary summary;

  @Column("description")
  private EventDescription description;

  @Column("location")
  private EventLocation location;

  @Column("dtstart")
  private LocalDateTime dtstart;

  @Column("dtend")
  private LocalDateTime dtend;

  @Column("all_day")
  private boolean allDay;

  @Column("rrule")
  private RecurrenceRule rrule;

  @CreatedDate
  @Column("created_at")
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column("updated_at")
  private LocalDateTime updatedAt;

  public CalendarEvent() {
  }

  public CalendarEvent(Long id, UserId userId, CalendarId calendarId, EventUid uid, ICalData icalData, ETag etag,
      EventSummary summary, EventDescription description, EventLocation location, LocalDateTime dtstart,
      LocalDateTime dtend, boolean allDay, RecurrenceRule rrule,
      LocalDateTime createdAt, LocalDateTime updatedAt) {
    this.id = id;
    this.userId = userId;
    this.calendarId = calendarId;
    this.uid = uid;
    this.icalData = icalData;
    this.etag = etag;
    this.summary = summary;
    this.description = description;
    this.location = location;
    this.dtstart = dtstart;
    this.dtend = dtend;
    this.allDay = allDay;
    this.rrule = rrule;
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

  public void setUserId(UserId userId) {
    this.userId = userId;
  }

  public CalendarId getCalendarId() {
    return calendarId;
  }

  public void setCalendarId(CalendarId calendarId) {
    this.calendarId = calendarId;
  }

  public EventUid getUid() {
    return uid;
  }

  public void setUid(EventUid uid) {
    this.uid = uid;
  }

  public ICalData getIcalData() {
    return icalData;
  }

  public void setIcalData(ICalData icalData) {
    this.icalData = icalData;
  }

  public ETag getEtag() {
    return etag;
  }

  public void setEtag(ETag etag) {
    this.etag = etag;
  }

  public EventSummary getSummary() {
    return summary;
  }

  public void setSummary(EventSummary summary) {
    this.summary = summary;
  }

  public EventDescription getDescription() {
    return description;
  }

  public void setDescription(EventDescription description) {
    this.description = description;
  }

  public EventLocation getLocation() {
    return location;
  }

  public void setLocation(EventLocation location) {
    this.location = location;
  }

  public LocalDateTime getDtstart() {
    return dtstart;
  }

  public void setDtstart(LocalDateTime dtstart) {
    this.dtstart = dtstart;
  }

  public LocalDateTime getDtend() {
    return dtend;
  }

  public void setDtend(LocalDateTime dtend) {
    this.dtend = dtend;
  }

  public boolean isAllDay() {
    return allDay;
  }

  public void setAllDay(boolean allDay) {
    this.allDay = allDay;
  }

  public RecurrenceRule getRrule() {
    return rrule;
  }

  public void setRrule(RecurrenceRule rrule) {
    this.rrule = rrule;
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
