package org.clever.data.redis.connection;

/**
 * 包含SORT操作参数的实体
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/26 23:18 <br/>
 */
public interface SortParameters {
    /**
     * 排序顺序
     */
    enum Order {
        ASC, DESC
    }

    /**
     * 实用程序类包装“LIMIT”设置
     */
    class Range {
        private final long start;
        private final long count;

        public Range(long start, long count) {
            this.start = start;
            this.count = count;
        }

        public long getStart() {
            return start;
        }

        public long getCount() {
            return count;
        }
    }

    /**
     * 返回排序顺序。如果未指定任何内容，则可以为空
     *
     * @return 排序顺序。{@literal null}（如果未设置）
     */
    SortParameters.Order getOrder();

    /**
     * 指示排序是数字（默认）还是字母（词典）。如果未指定任何内容，则可以为空。
     *
     * @return 排序类型。 {@literal null} （如果未设置）
     */
    Boolean isAlphabetic();

    /**
     * 返回按外部键排序的模式（如果已设置）（ {@code by} ）。如果未指定任何内容，则可以为空
     *
     * @return {@code BY} 模式。 {@literal null} （如果未设置）
     */
    byte[] getByPattern();

    /**
     * 返回用于检索外部键（ {@code GET} ）的模式（如果已设置）。如果未指定任何内容，则可以为空
     *
     * @return {@code GET} 模式。 {@literal null} （如果未设置）
     */
    byte[][] getGetPattern();

    /**
     * 返回排序限制（范围或分页）。如果未指定任何内容，则可以为空
     *
     * @return 排序/限制范围。 {@literal null} （如果未设置）
     */
    SortParameters.Range getLimit();
}
