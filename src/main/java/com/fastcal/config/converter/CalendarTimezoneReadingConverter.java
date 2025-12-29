package com.fastcal.config.converter;

import com.fastcal.domain.model.vo.CalendarTimezone;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class CalendarTimezoneReadingConverter implements Converter<String, CalendarTimezone> {

  @Override
  public CalendarTimezone convert(String source) {
    return CalendarTimezone.of(source);
  }
}
