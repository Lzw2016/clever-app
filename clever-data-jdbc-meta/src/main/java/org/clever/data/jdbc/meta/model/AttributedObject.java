package org.clever.data.jdbc.meta.model;

import lombok.Getter;
import org.clever.util.LinkedCaseInsensitiveMap;

import java.util.Map;

/**
 * 数据库 “schema名、表名、字段名” 使用全小写
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/04/27 20:10 <br/>
 */
@Getter
public abstract class AttributedObject {
    /**
     * 对象属性信息
     */
    public final Map<String, Object> attributes = new LinkedCaseInsensitiveMap<>();

    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String name) {
        return (T) attributes.get(name);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String name, T defaultValue) {
        return (T) attributes.getOrDefault(name, defaultValue);
    }

    boolean hasAttribute(String name) {
        return attributes.containsKey(name);
    }
}
