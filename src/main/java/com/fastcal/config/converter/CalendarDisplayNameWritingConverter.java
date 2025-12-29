package com.fastcal.config.converter;

import com.fastcal.domain.model.vo.CalendarDisplayName;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class CalendarDisplayNameWritingConverter implements Converter<CalendarDisplayName, String> {

  @Override
  public String convert(CalendarDisplayName source) {
    return source.getValue();
  }
}
