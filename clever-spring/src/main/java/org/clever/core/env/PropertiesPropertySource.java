package org.clever.core.env;

import java.util.Map;
import java.util.Properties;

/**
 * 从{@link java.util.Properties}对象提取属性的{@link PropertySource}实现。
 * 请注意，由于{@code Properties}对象在技术上是一个{@link java.util.Hashtable}，因此可能包含非字符串键或值。
 * 但是，此实现仅限于访问基于字符串的键和值，方式与Properties相同{@link Properties#getProperty}和{@link Properties#setProperty}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 21:12 <br/>
 */
public class PropertiesPropertySource extends MapPropertySource {
    @SuppressWarnings({"unchecked", "rawtypes"})
    public PropertiesPropertySource(String name, Properties source) {
        super(name, (Map) source);
    }

    protected PropertiesPropertySource(String name, Map<String, Object> source) {
        super(name, source);
    }

    @Override
    public String[] getPropertyNames() {
        synchronized (this.source) {
            return super.getPropertyNames();
        }
    }
}
