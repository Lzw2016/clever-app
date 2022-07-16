package org.clever.core;

import org.clever.util.Assert;

/**
 * 用于子线程获取父线程的本地变量场景的ThreadLocal，支持自定义ThreadLocal名字
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 17:03 <br/>
 */
public class NamedInheritableThreadLocal<T> extends InheritableThreadLocal<T> {
    /**
     * 自定义ThreadLocal名字
     */
    private final String name;

    public NamedInheritableThreadLocal(String name) {
        Assert.hasText(name, "Name must not be empty");
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
