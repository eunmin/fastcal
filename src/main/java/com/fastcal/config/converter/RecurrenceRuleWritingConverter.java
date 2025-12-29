package com.fastcal.config.converter;

import com.fastcal.domain.model.vo.RecurrenceRule;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class RecurrenceRuleWritingConverter implements Converter<RecurrenceRule, String> {

  @Override
  public String convert(RecurrenceRule source) {
    return source != null ? source.getValue() : null;
  }
}
