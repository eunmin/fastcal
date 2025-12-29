package com.fastcal.exception;

import org.springframework.http.HttpStatus;

public class CalendarNotFoundException extends CalDavException {

  public CalendarNotFoundException(String message) {
    super(message, HttpStatus.NOT_FOUND, null);
  }
}
