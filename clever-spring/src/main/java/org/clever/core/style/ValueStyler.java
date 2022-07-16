package org.clever.core.style;

/**
 * 根据约定封装值字符串样式算法的策略。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 21:39 <br/>
 */
public interface ValueStyler {
    /**
     * 设置给定值的样式，返回字符串表示形式。
     *
     * @param value 样式的对象值
     * @return 带样式的字符串
     */
    String style(Object value);
}
