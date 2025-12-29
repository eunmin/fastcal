package com.fastcal.config.converter;

import com.fastcal.domain.model.vo.CalendarDescription;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class CalendarDescriptionWritingConverter implements Converter<CalendarDescription, String> {

  @Override
  public String convert(CalendarDescription source) {
    return source.getValue();
  }
}
