package org.clever.data.redis.core;

import java.util.List;

/**
 * 映射器将Redis批量值响应（通常由排序查询返回）转换为实际对象。
 * 此接口的实现不必担心异常或连接处理。
 * <p>
 * 通常由 {@link RedisTemplate} {@code sort} 方法使用。
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 10:27 <br/>
 */
public interface BulkMapper<T, V> {
    T mapBulk(List<V> tuple);
}
