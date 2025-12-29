package com.fastcal.config.converter;

import com.fastcal.domain.model.vo.EventLocation;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class EventLocationWritingConverter implements Converter<EventLocation, String> {

  @Override
  public String convert(EventLocation source) {
    return source != null ? source.getValue() : null;
  }
}
