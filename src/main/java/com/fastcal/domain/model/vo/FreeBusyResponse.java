package com.fastcal.domain.model.vo;

import java.time.LocalDateTime;
import java.util.List;

public record FreeBusyResponse(String value) {

  private static final String ICAL_VERSION = "2.0";
  private static final String METHOD = "REPLY";

  public FreeBusyResponse {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("FreeBusyResponse cannot be null or blank");
    }
  }

  public static FreeBusyResponse of(
      String productId,
      LocalDateTime start,
      LocalDateTime end,
      List<BusyPeriod> busyPeriods) {

    StringBuilder periods = new StringBuilder();
    for (BusyPeriod period : busyPeriods) {
      periods.append("FREEBUSY:")
          .append(ICalDateTime.of(period.start()).toICalFormat())
          .append("/")
          .append(ICalDateTime.of(period.end()).toICalFormat())
          .append("\n");
    }

    String ical = """
        BEGIN:VCALENDAR
        VERSION:%s
        PRODID:%s
        METHOD:%s
        BEGIN:VFREEBUSY
        DTSTART:%s
        DTEND:%s
        %sEND:VFREEBUSY
        END:VCALENDAR
        """.formatted(
        ICAL_VERSION,
        productId,
        METHOD,
        ICalDateTime.of(start).toICalFormat(),
        ICalDateTime.of(end).toICalFormat(),
        periods);

    return new FreeBusyResponse(ical);
  }

  public String getValue() {
    return value;
  }

  public record BusyPeriod(LocalDateTime start, LocalDateTime end) {
    public BusyPeriod {
      if (start == null || end == null) {
        throw new IllegalArgumentException("BusyPeriod start and end cannot be null");
      }
    }
  }
}
