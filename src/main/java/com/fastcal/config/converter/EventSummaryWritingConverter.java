package com.fastcal.config.converter;

import com.fastcal.domain.model.vo.EventSummary;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class EventSummaryWritingConverter implements Converter<EventSummary, String> {

  @Override
  public String convert(EventSummary source) {
    return source != null ? source.getValue() : null;
  }
}
