package com.fastcal.config.converter;

import com.fastcal.domain.model.vo.CalendarColor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class CalendarColorWritingConverter implements Converter<CalendarColor, String> {

  @Override
  public String convert(CalendarColor source) {
    return source.getValue();
  }
}
