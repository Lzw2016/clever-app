package org.clever.core.json.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.clever.core.json.JsonWrapper;

import java.io.IOException;

public class JsonWrapperSerializer extends JsonSerializer<JsonWrapper> {
    public static final SimpleModule MODEL;

    static {
        MODEL = new SimpleModule();
        MODEL.addSerializer(JsonWrapper.class, new JsonWrapperSerializer());
    }

    @Override
    public void serialize(JsonWrapper value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeObject(value.getInnerMap());
    }
}
