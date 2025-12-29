package com.fastcal.config.converter;

import com.fastcal.domain.model.vo.ETag;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class ETagReadingConverter implements Converter<String, ETag> {

  @Override
  public ETag convert(String source) {
    return ETag.ofNullable(source);
  }
}
