package com.fastcal.config.converter;

import com.fastcal.domain.model.vo.UserDisplayName;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class UserDisplayNameWritingConverter implements Converter<UserDisplayName, String> {

  @Override
  public String convert(UserDisplayName source) {
    return source.getValue();
  }
}
