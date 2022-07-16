package org.clever.core.style;

/**
 * 简单的实用程序类，允许方便地访问值样式逻辑，主要用于支持描述性日志消息。
 *
 * <p>对于更复杂的需求，请直接使用{@link ValueStyler}抽象。这个类只是在下面使用了一个共享的{@link DefaultValueStyler}实例。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 21:40 <br/>
 *
 * @see ValueStyler
 * @see DefaultValueStyler
 */
public abstract class StylerUtils {
    /**
     * style方法使用的默认ValueStyler实例。也可用于此包中的ToStringCreator类。
     */
    static final ValueStyler DEFAULT_VALUE_STYLER = new DefaultValueStyler();

    /**
     * 根据默认约定设置指定值的样式。
     *
     * @param value 样式的对象值
     * @return 带样式的字符串
     * @see DefaultValueStyler
     */
    public static String style(Object value) {
        return DEFAULT_VALUE_STYLER.style(value);
    }
}
