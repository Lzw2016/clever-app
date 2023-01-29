package org.clever.data.redis.connection;

import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis 数据类型的枚举
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 18:05 <br/>
 */
public enum DataType {
    NONE("none"),
    STRING("string"),
    LIST("list"),
    SET("set"),
    ZSET("zset"),
    HASH("hash"),
    STREAM("stream");

    private static final Map<String, DataType> codeLookup = new ConcurrentHashMap<>(7);

    static {
        for (DataType type : EnumSet.allOf(DataType.class))
            codeLookup.put(type.code, type);
    }

    private final String code;

    DataType(String name) {
        this.code = name;
    }

    /**
     * 返回与当前枚举关联的代码
     *
     * @return 这个枚举的code
     */
    public String code() {
        return code;
    }

    /**
     * 将枚举代码转换为实际枚举的实用方法
     *
     * @param code 枚举code
     * @return 对应于给定代码的实际枚举
     */
    public static DataType fromCode(String code) {
        DataType data = codeLookup.get(code);
        if (data == null)
            throw new IllegalArgumentException("unknown data type code");
        return data;
    }
}
