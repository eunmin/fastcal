package com.fastcal.exception;

import org.springframework.http.HttpStatus;

public abstract class CalDavException extends RuntimeException {

  private final HttpStatus httpStatus;
  private final String davErrorType;

  protected CalDavException(String message, HttpStatus httpStatus, String davErrorType) {
    super(message);
    this.httpStatus = httpStatus;
    this.davErrorType = davErrorType;
  }

  public HttpStatus getHttpStatus() {
    return httpStatus;
  }

  public String getDavErrorType() {
    return davErrorType;
  }
}
