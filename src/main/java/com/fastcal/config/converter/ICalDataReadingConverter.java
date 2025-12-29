package com.fastcal.config.converter;

import com.fastcal.domain.model.vo.ICalData;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class ICalDataReadingConverter implements Converter<String, ICalData> {

  @Override
  public ICalData convert(String source) {
    return ICalData.ofNullable(source);
  }
}
