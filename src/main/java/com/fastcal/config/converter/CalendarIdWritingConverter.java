package com.fastcal.config.converter;

import com.fastcal.domain.model.vo.CalendarId;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class CalendarIdWritingConverter implements Converter<CalendarId, String> {

  @Override
  public String convert(CalendarId source) {
    return source.getValue();
  }
}
