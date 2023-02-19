package org.clever.data.redis.stream;

import org.clever.data.redis.connection.stream.Record;

/**
 * 用于接收 {@link Record messages} 传递的监听器接口
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/02/18 11:08 <br/>
 *
 * @param <K> 流键和流字段类型
 * @param <V> 流值类型
 */
@FunctionalInterface
public interface StreamListener<K, V extends Record<K, ?>> {
    /**
     * 收到 {@link Record} 时调用的回调
     *
     * @param message 从不为 {@literal null}
     */
    void onMessage(V message);
}
