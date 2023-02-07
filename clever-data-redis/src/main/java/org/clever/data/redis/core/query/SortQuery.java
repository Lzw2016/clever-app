package org.clever.data.redis.core.query;

import org.clever.data.redis.connection.RedisConnection;
import org.clever.data.redis.connection.SortParameters;
import org.clever.data.redis.connection.SortParameters.Order;
import org.clever.data.redis.connection.SortParameters.Range;
import org.clever.data.redis.core.RedisTemplate;

import java.util.List;

/**
 * Redis SORT 的高级抽象（相当于 {@link SortParameters}）。
 * 与 {@link RedisTemplate} 一起使用（就像 {@link SortParameters} 被 {@link RedisConnection} 使用一样）。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 10:15 <br/>
 */
public interface SortQuery<K> {
    /**
     * 返回排序的目标 key
     *
     * @return 目标 key
     */
    K getKey();

    /**
     * 返回排序顺序。如果未指定任何内容，则可以为 null
     *
     * @return 排序顺序
     */
    Order getOrder();

    /**
     * 指示排序是数字（默认）还是字母（字典顺序）。如果未指定任何内容，则可以为 null
     *
     * @return 排序的类型
     */
    Boolean isAlphabetic();

    /**
     * 返回排序限制（范围或分页）。如果未指定任何内容，则可以为 null
     *
     * @return 排序限制/范围
     */

    Range getLimit();

    /**
     * 返回用于排序的外部key的模式
     *
     * @return external key pattern
     */

    String getBy();

    /**
     * 返回其值由排序返回的外部键。
     *
     * @return 用于 GET 的（列表）key
     */
    List<String> getGetPattern();
}
