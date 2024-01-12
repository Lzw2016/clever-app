package org.clever.data.jdbc.mybatis;

/**
 * 作者：lizw <br/>
 * 创建时间：2024/01/12 17:03 <br/>
 */
@FunctionalInterface
public interface CreateObject<T> {
    T create() throws Throwable;
}
