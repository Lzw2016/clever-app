package org.clever.core.json.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/02/28 14:35 <br/>
 */
public class BigDecimalSerializer extends JsonSerializer<BigDecimal> {
    public final static BigDecimalSerializer instance = new BigDecimalSerializer();

    @Override
    public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(value.toPlainString());
    }
}
