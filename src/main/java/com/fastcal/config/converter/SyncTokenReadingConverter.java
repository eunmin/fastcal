package com.fastcal.config.converter;

import com.fastcal.domain.model.vo.SyncToken;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class SyncTokenReadingConverter implements Converter<String, SyncToken> {

  @Override
  public SyncToken convert(String source) {
    return SyncToken.of(source);
  }
}
