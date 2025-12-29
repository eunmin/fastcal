package com.fastcal.config.converter;

import com.fastcal.domain.model.vo.Email;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class EmailReadingConverter implements Converter<String, Email> {

  @Override
  public Email convert(String source) {
    return Email.of(source);
  }
}
