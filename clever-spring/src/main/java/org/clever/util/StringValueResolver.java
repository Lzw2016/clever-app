package org.clever.util;

/**
 * 用于解析字符串值的简单策略接口
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 16:36 <br/>
 */
@FunctionalInterface
public interface StringValueResolver {

    /**
     * 解析给定的字符串值，例如解析占位符
     *
     * @param strVal 原始字符串值，不能为空
     * @throws IllegalArgumentException 如果字符串值无法解析
     */
    String resolveStringValue(String strVal);
}
