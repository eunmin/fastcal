package com.fastcal.config.converter;

import com.fastcal.domain.model.vo.CalendarTimezone;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class CalendarTimezoneWritingConverter implements Converter<CalendarTimezone, String> {

  @Override
  public String convert(CalendarTimezone source) {
    return source.getValue();
  }
}
