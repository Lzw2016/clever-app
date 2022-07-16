package org.clever.boot.ansi;

/**
 * {@link AnsiElement Ansi} 样式
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/14 22:08 <br/>
 */
public enum AnsiStyle implements AnsiElement {
    NORMAL("0"),
    BOLD("1"),
    FAINT("2"),
    ITALIC("3"),
    UNDERLINE("4");

    private final String code;

    AnsiStyle(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return this.code;
    }
}
