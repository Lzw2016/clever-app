package org.clever.core.style;

/**
 * 用于打印{@code toString()}方法的策略接口。
 * 封装打印算法；其他一些对象（如生成器）应提供工作流。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 21:38 <br/>
 */
public interface ToStringStyler {
    /**
     * 设置{@code toString()}对象的样式，然后再设置其字段的样式。
     *
     * @param buffer 要打印到的缓冲区
     * @param obj    对象到样式
     */
    void styleStart(StringBuilder buffer, Object obj);

    /**
     * 在对{@code toString()}对象的字段进行样式设置后，对其进行样式设置。
     *
     * @param buffer 要打印到的缓冲区
     * @param obj    对象到样式
     */
    void styleEnd(StringBuilder buffer, Object obj);

    /**
     * 将字段值设置为字符串样式。
     *
     * @param buffer    要打印到的缓冲区
     * @param fieldName 字段的名称
     * @param value     字段值
     */
    void styleField(StringBuilder buffer, String fieldName, Object value);

    /**
     * 设置给定值的样式。
     *
     * @param buffer 要打印到的缓冲区
     * @param value  字段值
     */
    void styleValue(StringBuilder buffer, Object value);

    /**
     * 设置字段分隔符的样式。
     *
     * @param buffer 要打印到的缓冲区
     */
    void styleFieldSeparator(StringBuilder buffer);
}
