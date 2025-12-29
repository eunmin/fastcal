package com.fastcal.exception;

import org.springframework.http.HttpStatus;

public class CalendarAlreadyExistsException extends CalDavException {

  public CalendarAlreadyExistsException(String message) {
    super(message, HttpStatus.CONFLICT, "calendar-collection-location-ok");
  }
}
