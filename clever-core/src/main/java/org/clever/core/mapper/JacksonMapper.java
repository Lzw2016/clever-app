package org.clever.core.mapper;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.databind.util.JSONPObject;
import com.fasterxml.jackson.datatype.joda.cfg.JacksonJodaDateFormat;
import com.fasterxml.jackson.datatype.joda.ser.DateTimeSerializer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.clever.beans.BeanUtils;
import org.clever.core.KotlinDetector;
import org.clever.core.json.jackson.BigDecimalSerializer;
import org.clever.core.json.jackson.DateDeserializer;
import org.clever.util.ClassUtils;
import org.clever.util.LinkedMultiValueMap;
import org.clever.util.MultiValueMap;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Json串与Java对象的相互转换工具<br/>
 * 1.通过Jackson实现<br/>
 * <p/>
 * 作者：LiZW <br/>
 * 创建时间：2016-4-28 0:55 <br/>
 */
@Slf4j
public class JacksonMapper {
    private static volatile boolean kotlinWarningLogged = false;
    private static final JacksonMapper Instance;

    static {
        Instance = new JacksonMapper();
    }

    /**
     * 对象转换器
     */
    private final ObjectMapper mapper;

    public JacksonMapper(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 通用配置,参考 Jackson2ObjectMapperBuilder
     */
    private JacksonMapper() {
        mapper = newObjectMapper();
    }

    /**
     * 返回标准的实例
     */
    public static JacksonMapper getInstance() {
        return Instance;
    }

    /**
     * Object可以是POJO，也可以是Collection或数组。 如果对象为Null, 返回"null". 如果集合为空集合, 返回"[]".<br/>
     *
     * @param object 需要序列化的对象
     * @return 序列化后的Json字符串
     */
    @SneakyThrows
    public String toJson(Object object) {
        return mapper.writeValueAsString(object);
    }

    @SneakyThrows
    public String toJsonPretty(Object obj) {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
    }

    /**
     * 输出JSON格式数据.
     */
    public String toJsonP(String functionName, Object object) {
        return toJson(new JSONPObject(functionName, object));
    }

    /**
     * 反序列化POJO或简单Collection如List&lt;String&gt;<br/>
     * 如果JSON字符串为null或空字符串, 返回null. 如果JSON字符串为"[]", 返回空集合<br/>
     * 如需反序列化复杂Collection如List&lt;MyBean&gt;，请使用fromJson(String, JavaType)<br/>
     *
     * @param jsonString Json字符串
     * @param clazz      反序列化的对象类型
     * @return 反序列化的对象
     * @see #fromJson(String, JavaType)
     */
    @SneakyThrows
    public <T> T fromJson(String jsonString, Class<T> clazz) {
        if (jsonString == null) {
            return null;
        }
        return mapper.readValue(jsonString, clazz);
    }

    /**
     * 反序列化复杂Collection如List&lt;MyBean&gt;<br/>
     * 先使用createCollectionType()或contructMapType()构造类型, 然后调用本函数.<br/>
     *
     * @param jsonString Json字符串
     * @param javaType   JavaType
     * @return 反序列化的对象
     * @see #constructorCollectionType(Class, Class)
     * @see #constructorMapType(Class, Class, Class)
     */
    @SneakyThrows
    @SuppressWarnings("unchecked")
    public <T> T fromJson(String jsonString, JavaType javaType) {
        if (jsonString == null) {
            return null;
        }
        return (T) mapper.readValue(jsonString, javaType);
    }

    /**
     * 构造Collection类型.
     *
     * @param collectionClass 集合类型
     * @param elementClass    集合泛型类型
     * @return 返回JavaType
     * @see #fromJson(String, JavaType)
     */
    @SuppressWarnings("rawtypes")
    public JavaType constructorCollectionType(Class<? extends Collection> collectionClass, Class<?> elementClass) {
        return mapper.getTypeFactory().constructCollectionType(collectionClass, elementClass);
    }

    /**
     * 构造Map类型.
     *
     * @param mapClass   Map集合类型
     * @param keyClass   Map的Key泛型类型
     * @param valueClass Map的Value泛型类型
     * @return 返回JavaType
     * @see #fromJson(String, JavaType)
     */
    @SuppressWarnings("rawtypes")
    public JavaType constructorMapType(Class<? extends Map> mapClass, Class<?> keyClass, Class<?> valueClass) {
        return mapper.getTypeFactory().constructMapType(mapClass, keyClass, valueClass);
    }

    /**
     * 当JSON里只含有Bean的部分属性时，更新一个已存在Bean，只覆盖该部分的属性.
     *
     * @param jsonString Json字符串
     * @param object     需要更新的对象
     * @return 操作成功返回true
     */
    @SuppressWarnings("TryWithIdenticalCatches")
    public boolean update(String jsonString, Object object) {
        try {
            mapper.readerForUpdating(object).readValue(jsonString);
            return true;
        } catch (JsonProcessingException e) {
            log.error("update json string:" + jsonString + " to object:" + object + " error.", e);
            return false;
        } catch (Exception e) {
            log.error("update json string:" + jsonString + " to object:" + object + " error.", e);
            return false;
        }
    }

    /**
     * 返回当前 Jackson 对应的 ObjectMapper
     */
    public ObjectMapper getMapper() {
        return mapper;
    }

    public static ObjectMapper newObjectMapper() {
        final String dateFormatPattern = "yyyy-MM-dd HH:mm:ss";
        final ClassLoader moduleClassLoader = JacksonMapper.class.getClassLoader();
        // 创建 ObjectMapper
        ObjectMapper mapper = new ObjectMapper();
        // 设置输入时忽略在JSON字符串中存在但Java对象实际没有的属性
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        // 允许单引号、允许不带引号的字段名称
        mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        // 使用枚举的的toString函数来读写枚举
        // mapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
        // mapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
        // 设置时区 getTimeZone("GMT+8")
        mapper.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        // locale: zh_CN
        mapper.setLocale(Locale.CHINA);
        // 设置时间格式
        mapper.setDateFormat(new SimpleDateFormat(dateFormatPattern));
        // 注册 Module
        List<Module> modules = new ArrayList<>();
        MultiValueMap<Object, Module> modulesToRegister = new LinkedMultiValueMap<>();
        ObjectMapper.findModules(moduleClassLoader).forEach(module -> registerModule(module, modulesToRegister));
        registerWellKnownModulesIfAvailable(modulesToRegister, moduleClassLoader);
        for (List<Module> nestedModules : modulesToRegister.values()) {
            modules.addAll(nestedModules);
        }
        SimpleModule module = new SimpleModule();
        modules.add(module);
        module.addSerializer(DateTime.class, new DateTimeSerializer(new JacksonJodaDateFormat(DateTimeFormat.forPattern(dateFormatPattern).withZoneUTC())));
        module.addSerializer(BigDecimal.class, BigDecimalSerializer.instance);
        module.addSerializer(BigInteger.class, ToStringSerializer.instance);
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);
        module.addDeserializer(Date.class, DateDeserializer.instance);
        mapper.registerModules(modules);
        return mapper;
    }

    private static void registerModule(Module module, MultiValueMap<Object, Module> modulesToRegister) {
        if (module.getTypeId() == null) {
            modulesToRegister.add(SimpleModule.class.getName(), module);
        } else {
            modulesToRegister.set(module.getTypeId(), module);
        }
    }

    @SuppressWarnings("unchecked")
    private static void registerWellKnownModulesIfAvailable(MultiValueMap<Object, Module> modulesToRegister, ClassLoader moduleClassLoader) {
        try {
            Class<? extends Module> jdk8ModuleClass = (Class<? extends Module>) ClassUtils.forName("com.fasterxml.jackson.datatype.jdk8.Jdk8Module", moduleClassLoader);
            Module jdk8Module = BeanUtils.instantiateClass(jdk8ModuleClass);
            modulesToRegister.set(jdk8Module.getTypeId(), jdk8Module);
        } catch (ClassNotFoundException ex) {
            // jackson-datatype-jdk8 not available
        }

        try {
            Class<? extends Module> javaTimeModuleClass = (Class<? extends Module>) ClassUtils.forName("com.fasterxml.jackson.datatype.jsr310.JavaTimeModule", moduleClassLoader);
            Module javaTimeModule = BeanUtils.instantiateClass(javaTimeModuleClass);
            modulesToRegister.set(javaTimeModule.getTypeId(), javaTimeModule);
        } catch (ClassNotFoundException ex) {
            // jackson-datatype-jsr310 not available
        }

        // Joda-Time present?
        if (ClassUtils.isPresent("org.joda.time.LocalDate", moduleClassLoader)) {
            try {
                Class<? extends Module> jodaModuleClass = (Class<? extends Module>) ClassUtils.forName("com.fasterxml.jackson.datatype.joda.JodaModule", moduleClassLoader);
                Module jodaModule = BeanUtils.instantiateClass(jodaModuleClass);
                modulesToRegister.set(jodaModule.getTypeId(), jodaModule);
            } catch (ClassNotFoundException ex) {
                // jackson-datatype-joda not available
            }
        }

        // Kotlin present?
        if (KotlinDetector.isKotlinPresent()) {
            try {
                Class<? extends Module> kotlinModuleClass = (Class<? extends Module>) ClassUtils.forName("com.fasterxml.jackson.module.kotlin.KotlinModule", moduleClassLoader);
                Module kotlinModule = BeanUtils.instantiateClass(kotlinModuleClass);
                modulesToRegister.set(kotlinModule.getTypeId(), kotlinModule);
            } catch (ClassNotFoundException ex) {
                if (!kotlinWarningLogged) {
                    kotlinWarningLogged = true;
                    // log.warn("For Jackson Kotlin classes support please add \"com.fasterxml.jackson.module:jackson-module-kotlin\" to the classpath");
                }
            }
        }
    }
}
