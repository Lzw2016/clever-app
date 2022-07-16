package org.clever.boot.convert;

import org.clever.format.Formatter;

import java.text.ParseException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * 格式化{@link OffsetDateTime}，使用 {@link Formatter} 接口和 {@link DateTimeFormatter#ISO_OFFSET_DATE_TIME ISO offset formatting}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 23:03 <br/>
 */
class IsoOffsetFormatter implements Formatter<OffsetDateTime> {
    @Override
    public String print(OffsetDateTime object, Locale locale) {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(object);
    }

    @Override
    public OffsetDateTime parse(String text, Locale locale) throws ParseException {
        return OffsetDateTime.parse(text, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
