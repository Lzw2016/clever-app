package org.clever.data.redis.serializer;

import org.clever.util.Assert;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 简单的 {@link java.lang.String} 到 {@literal byte[]} 序列化器 <br/>
 * 使用指定的字符集（默认为 {@literal UTF-8}）将 {@link java.lang.String Strings} 转换为字节，反之亦然
 * <p>
 * 当与 Redis 的交互主要通过字符串发生时很有用。
 * <p>
 * 不执行任何 {@literal null} 转换，因为空字符串是有效的键值
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/27 17:04 <br/>
 */
public class StringRedisSerializer implements RedisSerializer<String> {
    private final Charset charset;

    /**
     * {@link StringRedisSerializer} 使用 7 位 ASCII，又名 ISO646-US，又名 Unicode 字符集的基本拉丁语块
     *
     * @see StandardCharsets#US_ASCII
     */
    public static final StringRedisSerializer US_ASCII = new StringRedisSerializer(StandardCharsets.US_ASCII);

    /**
     * {@link StringRedisSerializer} 使用 ISO 拉丁字母表 1，又名 ISO-LATIN-1
     *
     * @see StandardCharsets#ISO_8859_1
     */
    public static final StringRedisSerializer ISO_8859_1 = new StringRedisSerializer(StandardCharsets.ISO_8859_1);

    /**
     * {@link StringRedisSerializer} 使用 8 位 UCS 转换格式
     *
     * @see StandardCharsets#UTF_8
     */
    public static final StringRedisSerializer UTF_8 = new StringRedisSerializer(StandardCharsets.UTF_8);

    /**
     * 使用 {@link StandardCharsets#UTF_8 UTF-8} 创建一个新的 {@link StringRedisSerializer}
     */
    public StringRedisSerializer() {
        this(StandardCharsets.UTF_8);
    }

    /**
     * 使用给定的 {@link Charset} 创建一个新的 {@link StringRedisSerializer} 来编码和解码字符串
     *
     * @param charset 不得为 {@literal null}
     */
    public StringRedisSerializer(Charset charset) {
        Assert.notNull(charset, "Charset must not be null!");
        this.charset = charset;
    }

    @Override
    public String deserialize(byte[] bytes) {
        return (bytes == null ? null : new String(bytes, charset));
    }

    @Override
    public byte[] serialize(String string) {
        return (string == null ? null : string.getBytes(charset));
    }

    @Override
    public Class<?> getTargetType() {
        return String.class;
    }
}
