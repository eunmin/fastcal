package com.fastcal.exception;

import org.springframework.http.HttpStatus;

public class PreconditionFailedException extends CalDavException {

  public PreconditionFailedException(String message) {
    super(message, HttpStatus.PRECONDITION_FAILED, null);
  }
}
