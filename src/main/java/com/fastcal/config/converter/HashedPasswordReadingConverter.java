package com.fastcal.config.converter;

import com.fastcal.domain.model.vo.HashedPassword;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class HashedPasswordReadingConverter implements Converter<String, HashedPassword> {

  @Override
  public HashedPassword convert(String source) {
    return HashedPassword.of(source);
  }
}
