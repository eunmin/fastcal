package com.fastcal.config.converter;

import com.fastcal.domain.model.vo.HashedPassword;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class HashedPasswordWritingConverter implements Converter<HashedPassword, String> {

  @Override
  public String convert(HashedPassword source) {
    return source.getValue();
  }
}
