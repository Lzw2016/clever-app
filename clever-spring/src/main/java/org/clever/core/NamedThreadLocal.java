package org.clever.core;

import org.clever.util.Assert;

/**
 * 带有自定义名称的ThreadLocal
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 17:02 <br/>
 */
public class NamedThreadLocal<T> extends ThreadLocal<T> {
    /**
     * 自定义ThreadLocal名称
     */
    private final String name;

    public NamedThreadLocal(String name) {
        Assert.hasText(name, "Name must not be empty");
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
