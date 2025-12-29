package com.fastcal.config.converter;

import com.fastcal.domain.model.vo.SyncToken;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class SyncTokenWritingConverter implements Converter<SyncToken, String> {

  @Override
  public String convert(SyncToken source) {
    return source.getValue();
  }
}
