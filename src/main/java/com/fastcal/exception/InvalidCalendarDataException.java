package com.fastcal.exception;

import org.springframework.http.HttpStatus;

public class InvalidCalendarDataException extends CalDavException {

  public InvalidCalendarDataException(String message) {
    super(message, HttpStatus.BAD_REQUEST, "valid-calendar-data");
  }
}
