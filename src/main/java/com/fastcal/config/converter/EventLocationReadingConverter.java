package com.fastcal.config.converter;

import com.fastcal.domain.model.vo.EventLocation;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class EventLocationReadingConverter implements Converter<String, EventLocation> {

  @Override
  public EventLocation convert(String source) {
    return EventLocation.ofNullable(source);
  }
}
