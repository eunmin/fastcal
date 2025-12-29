package com.fastcal.config.converter;

import com.fastcal.domain.model.vo.CTag;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class CTagReadingConverter implements Converter<String, CTag> {

  @Override
  public CTag convert(String source) {
    return CTag.of(source);
  }
}
