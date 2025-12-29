package com.fastcal.config.converter;

import com.fastcal.domain.model.vo.EventUid;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class EventUidWritingConverter implements Converter<EventUid, String> {

  @Override
  public String convert(EventUid source) {
    return source != null ? source.getValue() : null;
  }
}
