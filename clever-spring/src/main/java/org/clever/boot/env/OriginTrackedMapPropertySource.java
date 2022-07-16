package org.clever.boot.env;

import org.clever.boot.origin.Origin;
import org.clever.boot.origin.OriginLookup;
import org.clever.boot.origin.OriginTrackedValue;
import org.clever.core.env.MapPropertySource;

import java.util.Map;

/**
 * {@link OriginLookup}由包含{@link OriginTrackedValue}的{@link Map}支持。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/03 18:01 <br/>
 *
 * @see OriginTrackedValue
 */
public final class OriginTrackedMapPropertySource extends MapPropertySource implements OriginLookup<String> {
    private final boolean immutable;

    /**
     * 创建新的 {@link OriginTrackedMapPropertySource}
     *
     * @param name   属性源名称
     * @param source 底层地图源
     */
    @SuppressWarnings("rawtypes")
    public OriginTrackedMapPropertySource(String name, Map source) {
        this(name, source, false);
    }

    /**
     * 创建新的 {@link OriginTrackedMapPropertySource}
     *
     * @param name      属性源名称
     * @param source    底层地图源
     * @param immutable 如果基础源是不变的并且保证不会更改
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public OriginTrackedMapPropertySource(String name, Map source, boolean immutable) {
        super(name, source);
        this.immutable = immutable;
    }

    @Override
    public Object getProperty(String name) {
        Object value = super.getProperty(name);
        if (value instanceof OriginTrackedValue) {
            return ((OriginTrackedValue) value).getValue();
        }
        return value;
    }

    @Override
    public Origin getOrigin(String name) {
        Object value = super.getProperty(name);
        if (value instanceof OriginTrackedValue) {
            return ((OriginTrackedValue) value).getOrigin();
        }
        return null;
    }

    @Override
    public boolean isImmutable() {
        return this.immutable;
    }
}
