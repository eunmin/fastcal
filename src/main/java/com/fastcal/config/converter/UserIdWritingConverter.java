package com.fastcal.config.converter;

import com.fastcal.domain.model.vo.UserId;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class UserIdWritingConverter implements Converter<UserId, String> {

  @Override
  public String convert(UserId source) {
    return source.getValue();
  }
}
