package com.fastcal.config.converter;

import com.fastcal.domain.model.vo.CalendarColor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class CalendarColorReadingConverter implements Converter<String, CalendarColor> {

  @Override
  public CalendarColor convert(String source) {
    return CalendarColor.of(source);
  }
}
