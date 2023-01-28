package org.clever.data.redis.core;

import org.clever.data.redis.connection.DataType;
import org.clever.util.Assert;
import org.clever.util.StringUtils;

import java.util.StringJoiner;

/**
 * 用于 {@literal SCAN} 命令的选项
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 18:04 <br/>
 *
 * @see KeyScanOptions
 */
public class ScanOptions {
    /**
     * 在不设置限制或匹配模式的情况下应用默认 {@link ScanOptions} 的常量。
     */
    public static ScanOptions NONE = new ScanOptions(null, null, null);

    private final Long count;
    private final String pattern;
    private final byte[] bytePattern;

    ScanOptions(Long count, String pattern, byte[] bytePattern) {
        this.count = count;
        this.pattern = pattern;
        this.bytePattern = bytePattern;
    }

    /**
     * 返回新 {@link ScanOptionsBuilder} 的静态工厂方法
     */
    public static ScanOptionsBuilder scanOptions() {
        return new ScanOptionsBuilder();
    }

    public Long getCount() {
        return count;
    }

    public String getPattern() {
        if (bytePattern != null && pattern == null) {
            return new String(bytePattern);
        }
        return pattern;
    }

    public byte[] getBytePattern() {
        if (bytePattern == null && pattern != null) {
            return pattern.getBytes();
        }
        return bytePattern;
    }

    public String toOptionString() {
        if (this.equals(ScanOptions.NONE)) {
            return "";
        }
        StringJoiner joiner = new StringJoiner(", ");
        if (this.getCount() != null) {
            joiner.add("'count' " + this.getCount());
        }
        String pattern = getPattern();
        if (StringUtils.hasText(pattern)) {
            joiner.add("'match' '" + pattern + "'");
        }
        return joiner.toString();
    }

    public static class ScanOptionsBuilder {
        Long count;
        String pattern;
        byte[] bytePattern;
        DataType type;

        ScanOptionsBuilder() {
        }

        /**
         * 返回使用给定的 {@code count} 配置的当前 {@link ScanOptionsBuilder}
         */
        public ScanOptionsBuilder count(long count) {
            this.count = count;
            return this;
        }

        /**
         * 返回使用给定的 {@code pattern} 配置的当前 {@link ScanOptionsBuilder}
         */
        public ScanOptionsBuilder match(String pattern) {
            this.pattern = pattern;
            return this;
        }

        /**
         * 返回使用给定的 {@code pattern} 配置的当前 {@link ScanOptionsBuilder}
         */
        public ScanOptionsBuilder match(byte[] pattern) {
            this.bytePattern = pattern;
            return this;
        }

        /**
         * 返回使用给定的 {@code type} 配置的当前 {@link ScanOptionsBuilder}。<br />
         * 请在使用前验证目标命令是否支持 <a href="https://redis.io/commands/SCAN#the-type-option">TYPE</a> 选项。
         *
         * @param type 不得为 {@literal null}。不要设置或使用 {@link DataType#NONE}
         */
        public ScanOptionsBuilder type(DataType type) {
            Assert.notNull(type, "Type must not be null! Use NONE instead.");
            this.type = type;
            return this;
        }

        /**
         * 返回使用给定的 {@code type} 配置的当前 {@link ScanOptionsBuilder}
         *
         * @param type {@link DataType#fromCode(String)} 的文本表示。不得为 {@literal null}。
         * @throws IllegalArgumentException 如果给定类型是 {@literal null} 或未知
         */
        public ScanOptionsBuilder type(String type) {
            Assert.notNull(type, "Type must not be null!");
            return type(DataType.fromCode(type));
        }

        /**
         * 构建一个新的 {@link ScanOptions} 对象
         *
         * @return 一个新的 {@link ScanOptions} 对象
         */
        public ScanOptions build() {
            if (type != null && !DataType.NONE.equals(type)) {
                return new KeyScanOptions(count, pattern, bytePattern, type.code());
            }
            return new ScanOptions(count, pattern, bytePattern);
        }
    }
}
