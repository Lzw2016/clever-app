package org.clever.boot.ansi;

import org.clever.util.Assert;

/**
 * ANSI 8 位前景或背景颜色代码的 {@link AnsiElement} 实现
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/14 22:17 <br/>
 */
public final class Ansi8BitColor implements AnsiElement {
    private final String prefix;
    private final int code;

    /**
     * 创建一个新的 {@link Ansi8BitColor}
     *
     * @param prefix 前缀转义字符
     * @param code   颜色代码（必须是 0-255）
     * @throws IllegalArgumentException 如果颜色代码不在 0 和 255 之间
     */
    private Ansi8BitColor(String prefix, int code) {
        Assert.isTrue(code >= 0 && code <= 255, "Code must be between 0 and 255");
        this.prefix = prefix;
        this.code = code;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Ansi8BitColor other = (Ansi8BitColor) obj;
        return this.prefix.equals(other.prefix) && this.code == other.code;
    }

    @Override
    public int hashCode() {
        return this.prefix.hashCode() * 31 + this.code;
    }

    @Override
    public String toString() {
        return this.prefix + this.code;
    }

    /**
     * 返回给定代码的前景 ANSI 颜色代码实例
     *
     * @param code 颜色代码
     * @return ANSI 颜色代码实例
     */
    public static Ansi8BitColor foreground(int code) {
        return new Ansi8BitColor("38;5;", code);
    }

    /**
     * 返回给定代码的背景 ANSI 颜色代码实例
     *
     * @param code 颜色代码
     * @return ANSI 颜色代码实例
     */
    public static Ansi8BitColor background(int code) {
        return new Ansi8BitColor("48;5;", code);
    }
}
