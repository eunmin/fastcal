package com.fastcal.config.converter;

import com.fastcal.domain.model.vo.EventDescription;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class EventDescriptionReadingConverter implements Converter<String, EventDescription> {

  @Override
  public EventDescription convert(String source) {
    return EventDescription.ofNullable(source);
  }
}
