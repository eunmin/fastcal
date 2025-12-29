package com.fastcal.xml;

import java.util.Map;

public record DavResponse(
    String href,
    Map<String, PropValue> props,
    Map<String, PropValue> notFoundProps,
    Integer status) {
}
