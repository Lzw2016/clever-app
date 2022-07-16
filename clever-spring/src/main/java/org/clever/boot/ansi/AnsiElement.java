package org.clever.boot.ansi;

/**
 * ANSI 可编码元素
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/14 22:07 <br/>
 */
public interface AnsiElement {
    /**
     * @return ANSI 转义码
     */
    @Override
    String toString();
}
