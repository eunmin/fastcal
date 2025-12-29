package com.fastcal.exception;

import org.springframework.http.HttpStatus;

public class EventNotFoundException extends CalDavException {

  public EventNotFoundException(String message) {
    super(message, HttpStatus.NOT_FOUND, null);
  }
}
