package com.fastcal.config.converter;

import com.fastcal.domain.model.vo.CalendarId;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class CalendarIdReadingConverter implements Converter<String, CalendarId> {

  @Override
  public CalendarId convert(String source) {
    return CalendarId.of(source);
  }
}
