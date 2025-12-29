package com.fastcal.config.converter;

import com.fastcal.domain.model.vo.ICalData;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class ICalDataWritingConverter implements Converter<ICalData, String> {

  @Override
  public String convert(ICalData source) {
    return source != null ? source.getValue() : null;
  }
}
