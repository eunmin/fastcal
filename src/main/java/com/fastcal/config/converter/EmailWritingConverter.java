package com.fastcal.config.converter;

import com.fastcal.domain.model.vo.Email;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class EmailWritingConverter implements Converter<Email, String> {

  @Override
  public String convert(Email source) {
    return source.getValue();
  }
}
