package org.clever.core.json.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.DateUtils;

import java.io.IOException;
import java.util.Date;

/**
 * 自定义反序列化时间
 * 作者： lzw<br/>
 * 创建时间：2019-08-18 15:47 <br/>
 */
public class DateDeserializer extends JsonDeserializer<Date> {
    public final static DateDeserializer instance = new DateDeserializer();

    @Override
    public Date deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (StringUtils.isBlank(p.getText())) {
            return null;
        }
        String str = StringUtils.trim(p.getText());
        return DateUtils.parseDate(str);
    }
}
