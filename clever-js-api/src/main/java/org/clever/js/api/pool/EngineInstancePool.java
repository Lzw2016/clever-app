package org.clever.js.api.pool;

import org.clever.js.api.ScriptEngineInstance;

import java.io.Closeable;

/**
 * 脚本引擎池
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2020/08/24 19:00 <br/>
 *
 * @param <E> script引擎类型
 * @param <T> script引擎对象类型
 */
public interface EngineInstancePool<E, T> extends Closeable {
    /**
     * 从池中获取引擎对象。使用完后必须使用 returnObject 方法返还获取的对象
     */
    ScriptEngineInstance<E, T> borrowObject() throws Exception;

    /**
     * 将引擎对象返还到池中。对象必须是从 borrowObject 方法获取到的
     */
    void returnObject(ScriptEngineInstance<E, T> obj) throws Exception;

    /**
     * 使池中的引擎对象失效，当获取到的对象被确定无效时（由于异常或其他问题），应该调用该方法
     */
    void invalidateObject(ScriptEngineInstance<E, T> obj) throws Exception;

    /**
     * 池中当前闲置的引擎对象数量
     */
    int getNumIdle();

    /**
     * 当前从池中借出的引擎对象的数量
     */
    int getNumActive();

    /**
     * 清除池中闲置的引擎对象
     */
    void clear() throws Exception;

    /**
     * 关闭这个池，并释放与之相关的资源
     */
    void close();
}
