package org.clever.core.convert.support;

import org.clever.core.convert.converter.Converter;

/**
 * 只调用{@link Object#toString()}将源对象转换为字符串
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 17:02 <br/>
 */
final class ObjectToStringConverter implements Converter<Object, String> {
    @Override
    public String convert(Object source) {
        return source.toString();
    }
}
