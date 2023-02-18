package org.clever.data.redis.hash;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.deser.std.UntypedObjectDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.CalendarSerializer;
import com.fasterxml.jackson.databind.ser.std.DateSerializer;
import org.clever.data.mapping.MappingException;
import org.clever.data.util.DirectFieldAccessFallbackBeanWrapper;
import org.clever.util.Assert;
import org.clever.util.NumberUtils;
import org.clever.util.StringUtils;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.Map.Entry;

/**
 * 基于 {@link ObjectMapper} 的 {@link HashMapper} 实现，允许展平。
 * 给定一个具有 {@code Address} 的实体 {@code Person}，如下所示，扁平化将为所有嵌套属性创建单独的哈希条目，并尽可能将复杂类型解析为简单类型。
 * <p>
 * 扁平化要求所有属性名称不干扰 JSON 路径。使用扁平化不支持在映射键中使用点或括号或作为属性名称。
 * 生成的 Hash 不能映射回对象。
 * <h3>例子</h3>
 * <pre>{@code
 * class Person {
 *     String firstname;
 *     String lastname;
 *     Address address;
 *     Date date;
 *     LocalDateTime localDateTime;
 * }
 *
 * class Address {
 *     String city;
 *     String country;
 * }
 * }</pre>
 *
 * <h3>Normal</h3>
 * <table>
 * <tr>
 * <th>Hash field</th>
 * <th>Value</th>
 * </tr>
 * <tr>
 * <td>firstname</td>
 * <td>Jon</td>
 * </tr>
 * <tr>
 * <td>lastname</td>
 * <td>Snow</td>
 * </tr>
 * <tr>
 * <td>address</td>
 * <td>{ "city" : "Castle Black", "country" : "The North" }</td>
 * </tr>
 * <tr>
 * <td>date</td>
 * <td>1561543964015</td>
 * </tr>
 * <tr>
 * <td>localDateTime</td>
 * <td>2018-01-02T12:13:14</td>
 * </tr>
 * </table>
 * <h3>Flat</h3>
 * <table>
 * <tr>
 * <th>Hash field</th>
 * <th>Value</th>
 * </tr>
 * <tr>
 * <td>firstname</td>
 * <td>Jon</td>
 * </tr>
 * <tr>
 * <td>lastname</td>
 * <td>Snow</td>
 * </tr>
 * <tr>
 * <td>address.city</td>
 * <td>Castle Black</td>
 * </tr>
 * <tr>
 * <td>address.country</td>
 * <td>The North</td>
 * </tr>
 * <tr>
 * <td>date</td>
 * <td>1561543964015</td>
 * </tr>
 * <tr>
 * <td>localDateTime</td>
 * <td>2018-01-02T12:13:14</td>
 * </tr>
 * </table>
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/02/15 22:57 <br/>
 */
public class Jackson2HashMapper implements HashMapper<Object, String, Object> {
    private static final Jackson2HashMapper INSTANCE = new Jackson2HashMapper(false);
    private static final HashMapperModule HASH_MAPPER_MODULE = new HashMapperModule();

    public static Jackson2HashMapper getSharedInstance() {
        return INSTANCE;
    }

    private final ObjectMapper typingMapper;
    private final ObjectMapper untypedMapper;
    private final boolean flatten;

    /**
     * 使用默认的 {@link ObjectMapper} 创建新的 {@link Jackson2HashMapper}
     */
    @SuppressWarnings("deprecation")
    public Jackson2HashMapper(boolean flatten) {
        this(new ObjectMapper().findAndRegisterModules(), flatten);
        typingMapper.enableDefaultTyping(DefaultTyping.NON_FINAL, As.PROPERTY);
        typingMapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
        // Prevent splitting time types into arrays. E
        typingMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        typingMapper.setSerializationInclusion(Include.NON_NULL);
        typingMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        typingMapper.registerModule(HASH_MAPPER_MODULE);
    }

    /**
     * 创建新的 {@link Jackson2HashMapper}
     *
     * @param mapper 不能是 {@literal null}
     */
    @SuppressWarnings("deprecation")
    public Jackson2HashMapper(ObjectMapper mapper, boolean flatten) {
        Assert.notNull(mapper, "Mapper must not be null!");
        this.typingMapper = mapper;
        this.flatten = flatten;
        this.untypedMapper = new ObjectMapper();
        untypedMapper.findAndRegisterModules();
        this.untypedMapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
        this.untypedMapper.setSerializationInclusion(Include.NON_NULL);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> toHash(Object source) {
        JsonNode tree = typingMapper.valueToTree(source);
        return flatten ? flattenMap(tree.fields()) : untypedMapper.convertValue(tree, Map.class);
    }

    @Override
    public Object fromHash(Map<String, Object> hash) {
        try {
            if (flatten) {
                return typingMapper.reader().forType(Object.class).readValue(untypedMapper.writeValueAsBytes(doUnflatten(hash)));
            }
            return typingMapper.treeToValue(untypedMapper.valueToTree(hash), Object.class);
        } catch (IOException e) {
            throw new MappingException(e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> doUnflatten(Map<String, Object> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        Set<String> treatSeperate = new LinkedHashSet<>();
        for (Entry<String, Object> entry : source.entrySet()) {
            String key = entry.getKey();
            String[] args = key.split("\\.");
            if (args.length == 1 && !args[0].contains("[")) {
                result.put(entry.getKey(), entry.getValue());
                continue;
            }
            if (args.length == 1 && args[0].contains("[")) {
                String prunedKey = args[0].substring(0, args[0].indexOf('['));
                if (result.containsKey(prunedKey)) {
                    appendValueToTypedList(args[0], entry.getValue(), (List<Object>) result.get(prunedKey));
                } else {
                    result.put(prunedKey, createTypedListWithValue(entry.getValue()));
                }
            } else {
                treatSeperate.add(key.substring(0, key.indexOf('.')));
            }
        }
        for (String partial : treatSeperate) {
            Map<String, Object> newSource = new LinkedHashMap<>();
            for (Entry<String, Object> entry : source.entrySet()) {
                if (entry.getKey().startsWith(partial)) {
                    newSource.put(entry.getKey().substring(partial.length() + 1), entry.getValue());
                }
            }
            if (partial.endsWith("]")) {
                String prunedKey = partial.substring(0, partial.indexOf('['));
                if (result.containsKey(prunedKey)) {
                    appendValueToTypedList(partial, doUnflatten(newSource), (List<Object>) result.get(prunedKey));
                } else {
                    result.put(prunedKey, createTypedListWithValue(doUnflatten(newSource)));
                }
            } else {
                result.put(partial, doUnflatten(newSource));
            }
        }
        return result;
    }

    private Map<String, Object> flattenMap(Iterator<Entry<String, JsonNode>> source) {
        Map<String, Object> resultMap = new HashMap<>();
        this.doFlatten("", source, resultMap);
        return resultMap;
    }

    private void doFlatten(String propertyPrefix, Iterator<Entry<String, JsonNode>> inputMap, Map<String, Object> resultMap) {
        if (StringUtils.hasText(propertyPrefix)) {
            propertyPrefix = propertyPrefix + ".";
        }
        while (inputMap.hasNext()) {
            Entry<String, JsonNode> entry = inputMap.next();
            flattenElement(propertyPrefix + entry.getKey(), entry.getValue(), resultMap);
        }
    }

    private void flattenElement(String propertyPrefix, Object source, Map<String, Object> resultMap) {
        if (!(source instanceof JsonNode)) {
            resultMap.put(propertyPrefix, source);
            return;
        }
        JsonNode element = (JsonNode) source;
        if (element.isArray()) {
            Iterator<JsonNode> nodes = element.elements();
            while (nodes.hasNext()) {
                JsonNode cur = nodes.next();
                if (cur.isArray()) {
                    this.flattenCollection(propertyPrefix, cur.elements(), resultMap);
                } else {
                    if (cur.asText().equals("java.util.Date")) {
                        resultMap.put(propertyPrefix, nodes.next().asText());
                        break;
                    }
                }
            }
        } else if (element.isContainerNode()) {
            this.doFlatten(propertyPrefix, element.fields(), resultMap);
        } else {
            resultMap.put(propertyPrefix, new DirectFieldAccessFallbackBeanWrapper(element).getPropertyValue("_value"));
        }
    }

    private void flattenCollection(String propertyPrefix, Iterator<JsonNode> list, Map<String, Object> resultMap) {
        int counter = 0;
        while (list.hasNext()) {
            JsonNode element = list.next();
            flattenElement(propertyPrefix + "[" + counter + "]", element, resultMap);
            counter++;
        }
    }

    @SuppressWarnings("unchecked")
    private void appendValueToTypedList(String key, Object value, List<Object> destination) {
        int index = Integer.parseInt(key.substring(key.indexOf('[') + 1, key.length() - 1));
        List<Object> resultList = ((List<Object>) destination.get(1));
        if (resultList.size() < index) {
            resultList.add(value);
        } else {
            resultList.add(index, value);
        }
    }

    private List<Object> createTypedListWithValue(Object value) {
        List<Object> listWithTypeHint = new ArrayList<>();
        listWithTypeHint.add(ArrayList.class.getName());
        List<Object> values = new ArrayList<>();
        values.add(value);
        listWithTypeHint.add(values);
        return listWithTypeHint;
    }

    private static class HashMapperModule extends SimpleModule {
        HashMapperModule() {
            addSerializer(java.util.Date.class, new UntypedSerializer<>(new DateToTimestampSerializer()));
            addSerializer(java.util.Calendar.class, new UntypedSerializer<>(new CalendarToTimestampSerializer()));
            addDeserializer(java.util.Date.class, new UntypedDateDeserializer());
            addDeserializer(java.util.Calendar.class, new UntypedCalendarDeserializer());
        }
    }

    /**
     * {@link JsonDeserializer} 用于 {@link Date} 对象而不考虑类型提示
     */
    private static class UntypedDateDeserializer extends JsonDeserializer<Date> {
        private final JsonDeserializer<?> delegate = new UntypedObjectDeserializer(null, null);

        @Override
        public Object deserializeWithType(JsonParser p, DeserializationContext ctxt, TypeDeserializer typeDeserializer) throws IOException {
            return deserialize(p, ctxt);
        }

        @Override
        public Date deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            Object val = delegate.deserialize(p, ctxt);
            if (val instanceof Date) {
                return (Date) val;
            }
            try {
                return ctxt.getConfig().getDateFormat().parse(val.toString());
            } catch (ParseException e) {
                return new Date(NumberUtils.parseNumber(val.toString(), Long.class));
            }
        }
    }

    /**
     * {@link JsonDeserializer} 用于 {@link Calendar} 对象而不考虑类型提示
     */
    private static class UntypedCalendarDeserializer extends JsonDeserializer<Calendar> {
        private final UntypedDateDeserializer dateDeserializer = new UntypedDateDeserializer();

        @Override
        public Object deserializeWithType(JsonParser p, DeserializationContext ctxt, TypeDeserializer typeDeserializer) throws IOException {
            return deserialize(p, ctxt);
        }

        @Override
        public Calendar deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            Date date = dateDeserializer.deserialize(p, ctxt);
            if (date == null) {
                return null;
            }
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            return calendar;
        }
    }

    /**
     * 无类型 {@link JsonSerializer} 在不编写 JSON 类型提示的情况下序列化普通值
     */
    private static class UntypedSerializer<T> extends JsonSerializer<T> {
        private final JsonSerializer<T> delegate;

        UntypedSerializer(JsonSerializer<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void serializeWithType(T value, JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSer) throws IOException {
            serialize(value, gen, serializers);
        }

        @Override
        public void serialize(T value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value == null) {
                serializers.defaultSerializeNull(gen);
            } else {
                delegate.serialize(value, gen, serializers);
            }
        }
    }

    private static class DateToTimestampSerializer extends DateSerializer {
        @Override
        protected boolean _asTimestamp(SerializerProvider serializers) {
            return true;
        }
    }

    private static class CalendarToTimestampSerializer extends CalendarSerializer {
        @Override
        protected boolean _asTimestamp(SerializerProvider serializers) {
            return true;
        }
    }
}
