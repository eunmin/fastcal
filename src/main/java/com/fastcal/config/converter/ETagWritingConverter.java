package com.fastcal.config.converter;

import com.fastcal.domain.model.vo.ETag;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class ETagWritingConverter implements Converter<ETag, String> {

  @Override
  public String convert(ETag source) {
    return source != null ? source.getValue() : null;
  }
}
