package org.clever.core;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/12/10 08:55 <br/>
 */
public enum RenameStrategy {
    /**
     * 不改变
     */
    None,
    /**
     * 小写驼峰
     */
    ToCamel,
    /**
     * 全小写下划线
     */
    ToUnderline,
    /**
     * 全大写
     */
    ToUpperCase,
    /**
     * 全小写
     */
    ToLowerCase,
}
