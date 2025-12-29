package com.fastcal.config.converter;

import com.fastcal.domain.model.vo.UserDisplayName;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class UserDisplayNameReadingConverter implements Converter<String, UserDisplayName> {

  @Override
  public UserDisplayName convert(String source) {
    return UserDisplayName.of(source);
  }
}
