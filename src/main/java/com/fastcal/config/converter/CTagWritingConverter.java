package com.fastcal.config.converter;

import com.fastcal.domain.model.vo.CTag;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class CTagWritingConverter implements Converter<CTag, String> {

  @Override
  public String convert(CTag source) {
    return source.getValue();
  }
}
