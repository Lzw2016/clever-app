package org.clever.js.graaljs.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.oracle.truffle.api.interop.TruffleObject;
import lombok.extern.slf4j.Slf4j;
import org.clever.core.mapper.JacksonMapper;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.Proxy;

import java.math.BigInteger;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/10/19 11:25 <br/>
 */
@Slf4j
public class JacksonMapperSupport {
    private static volatile boolean INITIALIZED = false;
    private static JacksonMapper HTTP_API_JACKSON_MAPPER;
    private static JacksonMapper REDIS_JACKSON_MAPPER;

    /**
     * 初始化内部使用的 JacksonMapper
     */
    public static synchronized void initGraalModule() {
        if (INITIALIZED) {
            return;
        }
        INITIALIZED = true;
        SimpleModule module = new SimpleModule();
        module.addSerializer(Value.class, ValueSerializer.INSTANCE);
        module.addSerializer(TruffleObject.class, ToStringSerializer.instance);
        module.addSerializer(Proxy.class, HostWrapperSerializer.INSTANCE);
        // 新版本 graaljs 不需要
        // try {
        //     Class<?> clazz = Class.forName("com.oracle.truffle.polyglot.HostWrapper");
        //     module.addSerializer(clazz, HostWrapperSerializer.instance);
        // } catch (ClassNotFoundException e) {
        //     log.warn("类型com.oracle.truffle.polyglot.HostWrapper加载失败", e);
        // }
        JacksonMapper.getInstance().getMapper().registerModules(module);
    }

    /**
     * HTTP API 数据序列化使用的JacksonMapper
     */
    public static synchronized JacksonMapper getHttpApiJacksonMapper() {
        if (HTTP_API_JACKSON_MAPPER != null) {
            return HTTP_API_JACKSON_MAPPER;
        }
        initGraalModule();
        ObjectMapper objectMapper = JacksonMapper.getInstance().getMapper().copy();
        SimpleModule module = new SimpleModule();
        module.addSerializer(BigInteger.class, ToStringSerializer.instance);
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);
        objectMapper.registerModules(module);
        HTTP_API_JACKSON_MAPPER = new JacksonMapper(objectMapper);
        return HTTP_API_JACKSON_MAPPER;
    }

    /**
     * Redis客户端数据序列化使用的JacksonMapper
     */
    public static synchronized JacksonMapper getRedisJacksonMapper() {
        if (REDIS_JACKSON_MAPPER != null) {
            return REDIS_JACKSON_MAPPER;
        }
        initGraalModule();
        ObjectMapper objectMapper = JacksonMapper.getInstance().getMapper().copy();
        // SimpleModule module = new SimpleModule();
        // module.addSerializer(BigInteger.class, ToStringSerializer.instance);
        // module.addSerializer(Long.class, ToStringSerializer.instance);
        // module.addSerializer(Long.TYPE, ToStringSerializer.instance);
        // objectMapper.registerModules(module);
        REDIS_JACKSON_MAPPER = new JacksonMapper(objectMapper);
        return REDIS_JACKSON_MAPPER;
    }
}

//    public static class GraalToStringSerializer extends JsonSerializer<Object> {
//        public static final GraalToStringSerializer INSTANCE = new GraalToStringSerializer();
//
//        private GraalToStringSerializer() {
//        }
//
//        @Override
//        public void serialize(Object object, JsonGenerator gen, SerializerProvider serializers) throws IOException {
//            String json = null;
//            if (object != null) {
//                json = object.toString();
//            }
//            if (StringUtils.isNotBlank(json)) {
//                gen.writeRawValue(json);
//                // gen.writeObject(new JSONObject(json).toMap());
//            }
//        }
//    }
