package org.clever.data.redis.connection.stream;

import org.clever.util.Assert;

import java.util.Map;

/**
 * 流中的单个条目，由 {@link RecordId entry-id} 和实际条目值（通常是 {@link MapRecord 字段值对} 的集合）组成.
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 21:50 <br/>
 *
 * @see <a href="https://redis.io/topics/streams-intro#streams-basics">Redis 文档 - Stream Basics</a>
 */
public interface Record<S, V> {
    /**
     * 流的 ID（又名 Redis 中的 {@literal key}）
     *
     * @return can be {@literal null}.
     */
    S getStream();

    /**
     * 流中条目的 ID
     *
     * @return 从不为 {@literal null}
     */
    RecordId getId();

    /**
     * @return 实际内容。从不为 {@literal null}
     */
    V getValue();

    /**
     * 创建一个新的 {@link MapRecord} 实例，该实例由持有 {@literal field/value} 对的给定 {@link Map} 支持. <br />
     * 您可能想使用通过 {@link StreamRecords} 提供的构建器。
     *
     * @param map 原始map
     * @param <K> 给定 {@link Map} 的键类型
     * @param <V> 给定 {@link Map} 的值类型
     * @return {@link MapRecord} 的新实例
     */
    static <S, K, V> MapRecord<S, K, V> of(Map<K, V> map) {
        Assert.notNull(map, "Map must not be null!");
        return StreamRecords.mapBacked(map);
    }

    /**
     * 创建一个由给定的 {@literal value} 支持的新 {@link ObjectRecord} 实例。该值可以是简单类型，如 {@link String} 或复杂类型 <br />
     * 您可能想使用通过 {@link StreamRecords} 提供的构建器
     *
     * @param value 值
     * @param <V>   支持值的类型
     * @return {@link MapRecord} 的新实例
     */
    static <S, V> ObjectRecord<S, V> of(V value) {
        Assert.notNull(value, "Value must not be null!");
        return StreamRecords.objectBacked(value);
    }

    /**
     * 使用给定的 {@link RecordId} 创建 {@link Record} 的新实例
     *
     * @param id 不能是 {@literal null}
     * @return {@link Record} 的新实例
     */
    Record<S, V> withId(RecordId id);

    /**
     * 使用给定的 {@literal key} 创建 {@link Record} 的新实例以存储记录
     *
     * @param key 标识流的 Redis 键
     * @return {@link Record} 的新实例
     */
    <SK> Record<SK, V> withStreamKey(SK key);
}
