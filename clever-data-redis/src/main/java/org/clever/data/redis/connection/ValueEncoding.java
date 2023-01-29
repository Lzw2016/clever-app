package org.clever.data.redis.connection;

import org.clever.util.ObjectUtils;

import java.util.Optional;

/**
 * {@link ValueEncoding} 用于Redis内部数据表示，用于存储与键关联的值。 <br />
 * <dl>
 * <dt>Strings</dt>
 * <dd>{@link RedisValueEncoding#RAW} or {@link RedisValueEncoding#INT}</dd>
 * <dt>Lists</dt>
 * <dd>{@link RedisValueEncoding#ZIPLIST} or {@link RedisValueEncoding#LINKEDLIST}</dd>
 * <dt>Sets</dt>
 * <dd>{@link RedisValueEncoding#INTSET} or {@link RedisValueEncoding#HASHTABLE}</dd>
 * <dt>Hashes</dt>
 * <dd>{@link RedisValueEncoding#ZIPLIST} or {@link RedisValueEncoding#HASHTABLE}</dd>
 * <dt>Sorted Sets</dt>
 * <dd>{@link RedisValueEncoding#ZIPLIST} or {@link RedisValueEncoding#SKIPLIST}</dd>
 * <dt>Absent keys</dt>
 * <dd>{@link RedisValueEncoding#VACANT}</dd>
 * </dl>
 * <p>
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/26 23:22 <br/>
 */
public interface ValueEncoding {
    String raw();

    /**
     * 获取给定 {@code encoding} 的 {@link ValueEncoding}
     *
     * @param encoding 可以是 {@literal null}
     * @return 从不为 {@literal null}
     */
    static ValueEncoding of(String encoding) {
        return RedisValueEncoding.lookup(encoding).orElse(() -> encoding);
    }

    /**
     * Redis中使用的编码的默认 {@link ValueEncoding} 实现
     */
    enum RedisValueEncoding implements ValueEncoding {
        /**
         * 正常字符串编码
         */
        RAW("raw"),
        /**
         * 64位有符号间隔表示整数的字符串
         */
        INT("int"),
        /**
         * 小列表、哈希和排序集的空间节省表示
         */
        ZIPLIST("ziplist"),
        /**
         * 大型列表的编码
         */
        LINKEDLIST("linkedlist"),
        /**
         * 仅包含整数的小集合的空间节省表示法
         */
        INTSET("intset"),
        /**
         * 大型哈希的编码
         */
        HASHTABLE("hashtable"),
        /**
         * 任何大小的排序集的编码
         */
        SKIPLIST("skiplist"),
        /**
         * 由于密钥不存在，因此不存在编码
         */
        VACANT(null);

        private final String raw;

        RedisValueEncoding(String raw) {
            this.raw = raw;
        }

        @Override
        public String raw() {
            return raw;
        }

        static Optional<ValueEncoding> lookup(String encoding) {
            for (ValueEncoding valueEncoding : values()) {
                if (ObjectUtils.nullSafeEquals(valueEncoding.raw(), encoding)) {
                    return Optional.of(valueEncoding);
                }
            }
            return Optional.empty();
        }
    }
}
