package com.fastcal.config.converter;

import com.fastcal.domain.model.vo.EventSummary;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class EventSummaryReadingConverter implements Converter<String, EventSummary> {

  @Override
  public EventSummary convert(String source) {
    return EventSummary.ofNullable(source);
  }
}
