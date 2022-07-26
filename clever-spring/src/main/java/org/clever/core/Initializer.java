package org.clever.core;

/**
 * 组件初始化抽象类，初始化代码最多只能执行一次
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/26 17:50 <br/>
 */
public abstract class Initializer {
    protected volatile boolean initialized = false;

    /**
     * 初始化组件
     */
    public void init() throws InitializerException {
        if (initialized) {
            return;
        }
        initialized = true;
        try {
            doInit();
        } catch (Exception e) {
            throw new InitializerException(e);
        }
    }

    /**
     * 不直接调用，应该调用 {@link #init()}
     */
    protected abstract void doInit() throws Exception;
}
