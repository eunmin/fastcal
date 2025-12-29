package com.fastcal.config.converter;

import com.fastcal.domain.model.vo.CalendarDisplayName;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class CalendarDisplayNameReadingConverter implements Converter<String, CalendarDisplayName> {

  @Override
  public CalendarDisplayName convert(String source) {
    return CalendarDisplayName.of(source);
  }
}
