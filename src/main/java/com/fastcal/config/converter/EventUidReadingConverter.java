package com.fastcal.config.converter;

import com.fastcal.domain.model.vo.EventUid;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class EventUidReadingConverter implements Converter<String, EventUid> {

  @Override
  public EventUid convert(String source) {
    return EventUid.ofNullable(source);
  }
}
