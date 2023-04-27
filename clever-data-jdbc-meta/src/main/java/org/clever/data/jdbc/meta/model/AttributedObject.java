package org.clever.data.jdbc.meta.model;

import java.util.HashMap;
import java.util.Map;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/04/27 20:10 <br/>
 */
public abstract class AttributedObject {
    /**
     * 对象属性信息
     */
    public final Map<String, Object> attributes = new HashMap<>();

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

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    boolean hasAttribute(String name) {
        return attributes.containsKey(name);
    }
}
