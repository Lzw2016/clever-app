package org.clever.spring.shim;

/**
 * 垫片接口，对Spring类的改造，以适应底层框架需要
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2024/09/20 12:05 <br/>
 */
public interface Shim<T> {
    /**
     * 返回原始的对象值
     */
    T getRawObj();
}
