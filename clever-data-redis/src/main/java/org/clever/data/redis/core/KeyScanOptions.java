package org.clever.data.redis.core;

import org.clever.data.redis.connection.DataType;
import org.clever.util.StringUtils;

import java.util.StringJoiner;

/**
 * 用于 {@literal SCAN} 命令的选项
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 18:10 <br/>
 */
public class KeyScanOptions extends ScanOptions {
    /**
     * 在不设置限制或匹配模式的情况下应用默认 {@link KeyScanOptions} 的常量
     */
    public static KeyScanOptions NONE = new KeyScanOptions(null, null, null, null);
    private final String type;

    KeyScanOptions(Long count, String pattern, byte[] bytePattern, String type) {
        super(count, pattern, bytePattern);
        this.type = type;
    }

    /**
     * 返回新 {@link ScanOptionsBuilder} 的静态工厂方法
     */
    public static ScanOptionsBuilder scanOptions(DataType type) {
        return new ScanOptionsBuilder().type(type);
    }

    public String getType() {
        return type;
    }

    @Override
    public String toOptionString() {
        if (this.equals(KeyScanOptions.NONE)) {
            return "";
        }
        StringJoiner joiner = new StringJoiner(", ").add(super.toOptionString());
        if (StringUtils.hasText(type)) {
            joiner.add("'type' '" + type + "'");
        }
        return joiner.toString();
    }
}
