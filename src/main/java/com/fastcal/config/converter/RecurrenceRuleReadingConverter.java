package com.fastcal.config.converter;

import com.fastcal.domain.model.vo.RecurrenceRule;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class RecurrenceRuleReadingConverter implements Converter<String, RecurrenceRule> {

  @Override
  public RecurrenceRule convert(String source) {
    return RecurrenceRule.ofNullable(source);
  }
}
