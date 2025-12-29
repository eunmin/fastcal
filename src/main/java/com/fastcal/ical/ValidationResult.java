package com.fastcal.ical;

public record ValidationResult(
    boolean valid,
    String error) {
}
