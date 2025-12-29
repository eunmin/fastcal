package com.fastcal.config.converter;

import com.fastcal.domain.model.vo.EventDescription;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class EventDescriptionWritingConverter implements Converter<EventDescription, String> {

  @Override
  public String convert(EventDescription source) {
    return source != null ? source.getValue() : null;
  }
}
