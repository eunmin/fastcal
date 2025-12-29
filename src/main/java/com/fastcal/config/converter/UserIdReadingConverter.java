package com.fastcal.config.converter;

import com.fastcal.domain.model.vo.UserId;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class UserIdReadingConverter implements Converter<String, UserId> {

  @Override
  public UserId convert(String source) {
    return UserId.of(source);
  }
}
