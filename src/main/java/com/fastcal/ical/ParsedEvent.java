package com.fastcal.ical;

import java.time.LocalDateTime;

public record ParsedEvent(
    String uid,
    String summary,
    String description,
    String location,
    LocalDateTime dtstart,
    LocalDateTime dtend,
    boolean allDay,
    String rrule) {
}
