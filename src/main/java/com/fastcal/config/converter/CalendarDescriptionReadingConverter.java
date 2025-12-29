package com.fastcal.config.converter;

import com.fastcal.domain.model.vo.CalendarDescription;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class CalendarDescriptionReadingConverter implements Converter<String, CalendarDescription> {

  @Override
  public CalendarDescription convert(String source) {
    return CalendarDescription.of(source);
  }
}
