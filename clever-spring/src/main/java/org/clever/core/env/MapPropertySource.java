package org.clever.core.env;

import org.clever.util.StringUtils;

import java.util.Map;

/**
 * 从{@code Map}对象读取键和值的{@link PropertySource}。
 * 为了符合{@link #getProperty}和{@link #containsProperty}语义，基础map不应包含任何空值
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 21:12 <br/>
 *
 * @see PropertiesPropertySource
 */
public class MapPropertySource extends EnumerablePropertySource<Map<String, Object>> {
    /**
     * 新建{@code MapPropertySource} 使用给定的名称和 {@code Map}
     *
     * @param name   关联的名称
     * @param source Map源(无空值，以获得一致的{@link #getProperty}和{@link #containsProperty}行为)
     */
    public MapPropertySource(String name, Map<String, Object> source) {
        super(name, source);
    }

    @Override
    public Object getProperty(String name) {
        return this.source.get(name);
    }

    @Override
    public boolean containsProperty(String name) {
        return this.source.containsKey(name);
    }

    @Override
    public String[] getPropertyNames() {
        return StringUtils.toStringArray(this.source.keySet());
    }
}
