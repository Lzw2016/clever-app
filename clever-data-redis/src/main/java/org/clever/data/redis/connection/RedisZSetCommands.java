package org.clever.data.redis.connection;

import org.clever.data.redis.core.Cursor;
import org.clever.data.redis.core.ScanOptions;
import org.clever.util.Assert;
import org.clever.util.ObjectUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

/**
 * Redis 支持的 ZSet(SortedSet) 特定命令
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 17:17 <br/>
 */
public interface RedisZSetCommands {
    /**
     * 排序聚合操作
     */
    enum Aggregate {
        SUM, MIN, MAX
    }

    /**
     * 值对象，封装每个输入排序集的乘法因子。
     * 这意味着，在传递给聚合函数之前，每个输入排序集合中每个元素的得分都会乘以该因子。
     */
    class Weights {
        private final List<Double> weights;

        private Weights(List<Double> weights) {
            this.weights = weights;
        }

        /**
         * 创建新的 {@link RedisZSetCommands.Weights} 给定 {@code weights} 作为 {@code int}
         *
         * @param weights 不能是 {@literal null}
         * @return {@link RedisZSetCommands.Weights} 用于 {@code weights}
         */
        public static RedisZSetCommands.Weights of(int... weights) {
            Assert.notNull(weights, "Weights must not be null!");
            return new RedisZSetCommands.Weights(Arrays.stream(weights).mapToDouble(value -> value).boxed().collect(Collectors.toList()));
        }

        /**
         * 给定 {@code weights} 作为 {@code double} 创建新的 {@link RedisZSetCommands.Weights}
         *
         * @param weights 不能是 {@literal null}
         * @return {@link RedisZSetCommands.Weights} 用于 {@code weights}
         */
        public static RedisZSetCommands.Weights of(double... weights) {
            Assert.notNull(weights, "Weights must not be null!");
            return new RedisZSetCommands.Weights(DoubleStream.of(weights).boxed().collect(Collectors.toList()));
        }

        /**
         * 为权重为 1 的多个输入集 {@code count} 创建相等的 {@link RedisZSetCommands.Weights}
         *
         * @param count 输入集的数量。必须大于或等于零
         * @return 等于 {@link RedisZSetCommands.Weights} 对于许多权重为 1 的输入集
         */
        public static RedisZSetCommands.Weights fromSetCount(int count) {
            Assert.isTrue(count >= 0, "Count of input sorted sets must be greater or equal to zero!");
            return new RedisZSetCommands.Weights(IntStream.range(0, count).mapToDouble(value -> 1).boxed().collect(Collectors.toList()));
        }

        /**
         * 创建一个新的 {@link RedisZSetCommands.Weights} 对象，其中包含乘以 {@code multiplier} 的所有权重
         *
         * @param multiplier 用于乘以每个权重的乘数
         * @return 等于 {@link RedisZSetCommands.Weights} 对于许多权重为 1 的输入集
         */
        public RedisZSetCommands.Weights multiply(int multiplier) {
            return apply(it -> it * multiplier);
        }

        /**
         * 创建一个新的 {@link RedisZSetCommands.Weights} 对象，其中包含乘以 {@code multiplier} 的所有权重
         *
         * @param multiplier 用于乘以每个权重的乘数
         * @return 等于 {@link RedisZSetCommands.Weights} 对于许多权重为 1 的输入集
         */
        public RedisZSetCommands.Weights multiply(double multiplier) {
            return apply(it -> it * multiplier);
        }

        /**
         * 创建一个新的 {@link RedisZSetCommands.Weights} 对象，其中包含应用了 {@link Function} 的所有权重
         *
         * @param operator 运算符函数
         * @return 应用了 {@link DoubleUnaryOperator} 的新 {@link RedisZSetCommands.Weights}
         */
        public RedisZSetCommands.Weights apply(Function<Double, Double> operator) {
            return new RedisZSetCommands.Weights(weights.stream().map(operator).collect(Collectors.toList()));
        }

        /**
         * 在 {@code index} 检索权重
         *
         * @param index 权重指数
         * @return {@code index} 处的权重
         * @throws IndexOutOfBoundsException 如果索引超出范围
         */
        public double getWeight(int index) {
            return weights.get(index);
        }

        /**
         * @return 重量的数量
         */
        public int size() {
            return weights.size();
        }

        /**
         * @return 一个数组，其中包含此列表中按正确顺序（从第一个元素到最后一个元素）的所有权重
         */
        public double[] toArray() {
            return weights.stream().mapToDouble(Double::doubleValue).toArray();
        }

        /**
         * @return {@link List} 包含此列表中按正确顺序（从第一个元素到最后一个元素）的所有权重
         */
        public List<Double> toList() {
            return Collections.unmodifiableList(weights);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof RedisZSetCommands.Weights)) {
                return false;
            }
            RedisZSetCommands.Weights that = (RedisZSetCommands.Weights) o;
            return ObjectUtils.nullSafeEquals(this.weights, that.weights);
        }

        @Override
        public int hashCode() {
            return ObjectUtils.nullSafeHashCode(weights);
        }
    }

    /**
     * ZSet 元组
     */
    interface Tuple extends Comparable<Double> {
        /**
         * @return 成员的原始值
         */
        byte[] getValue();

        /**
         * @return 用于排序的成员分数值
         */
        Double getScore();
    }

    /**
     * {@link RedisZSetCommands.Range} 定义 {@literal min} 和 {@literal max} 值以从 {@literal ZSET} 检索
     */
    class Range {
        RedisZSetCommands.Range.Boundary min;
        RedisZSetCommands.Range.Boundary max;

        /**
         * @return 新的 {@link RedisZSetCommands.Range}
         */
        public static RedisZSetCommands.Range range() {
            return new RedisZSetCommands.Range();
        }

        /**
         * @return new {@link RedisZSetCommands.Range} with {@literal min} and {@literal max} set to {@link RedisZSetCommands.Range.Boundary#infinite()}.
         */
        public static RedisZSetCommands.Range unbounded() {
            RedisZSetCommands.Range range = new RedisZSetCommands.Range();
            range.min = RedisZSetCommands.Range.Boundary.infinite();
            range.max = RedisZSetCommands.Range.Boundary.infinite();
            return range;
        }

        /**
         * 大于等于
         *
         * @param min 不能是 {@literal null}
         * @return this.
         */
        public RedisZSetCommands.Range gte(Object min) {
            Assert.notNull(min, "Min already set for range.");
            this.min = new RedisZSetCommands.Range.Boundary(min, true);
            return this;
        }

        /**
         * 大于
         *
         * @param min 不能是 {@literal null}
         * @return this.
         */
        public RedisZSetCommands.Range gt(Object min) {
            Assert.notNull(min, "Min already set for range.");
            this.min = new RedisZSetCommands.Range.Boundary(min, false);
            return this;
        }

        /**
         * 小于等于
         *
         * @param max 不能是 {@literal null}
         * @return this.
         */
        public RedisZSetCommands.Range lte(Object max) {
            Assert.notNull(max, "Max already set for range.");
            this.max = new RedisZSetCommands.Range.Boundary(max, true);
            return this;
        }

        /**
         * 少于
         *
         * @param max 不能是 {@literal null}
         * @return this.
         */
        public RedisZSetCommands.Range lt(Object max) {

            Assert.notNull(max, "Max already set for range.");
            this.max = new RedisZSetCommands.Range.Boundary(max, false);
            return this;
        }

        /**
         * @return {@literal null} 如果未设置
         */
        public RedisZSetCommands.Range.Boundary getMin() {
            return min;
        }

        /**
         * @return {@literal null} 如果未设置
         */
        public RedisZSetCommands.Range.Boundary getMax() {
            return max;
        }

        public static class Boundary {
            Object value;
            boolean including;

            static RedisZSetCommands.Range.Boundary infinite() {
                return new RedisZSetCommands.Range.Boundary(null, true);
            }

            Boundary(Object value, boolean including) {
                this.value = value;
                this.including = including;
            }

            public Object getValue() {
                return value;
            }

            public boolean isIncluding() {
                return including;
            }
        }
    }

    class Limit {
        private static final RedisZSetCommands.Limit UNLIMITED = new RedisZSetCommands.Limit() {
            @Override
            public int getCount() {
                return -1;
            }

            @Override
            public int getOffset() {
                return super.getOffset();
            }
        };

        int offset;
        int count;

        public static RedisZSetCommands.Limit limit() {
            return new RedisZSetCommands.Limit();
        }

        public RedisZSetCommands.Limit offset(int offset) {
            this.offset = offset;
            return this;
        }

        public RedisZSetCommands.Limit count(int count) {
            this.count = count;
            return this;
        }

        public int getCount() {
            return count;
        }

        public int getOffset() {
            return offset;
        }

        public boolean isUnlimited() {
            return this.equals(UNLIMITED);
        }

        /**
         * @return 新的 {@link RedisZSetCommands.Limit} 表示没有限制
         */
        public static RedisZSetCommands.Limit unlimited() {
            return UNLIMITED;
        }
    }

    /**
     * {@code ZADD} 具体参数。 <br />
     * 寻找 {@code INCR} 标志？请改用 {@code ZINCRBY} 操作。
     *
     * @see <a href="https://redis.io/commands/zadd">Redis 文档: ZADD</a>
     */
    class ZAddArgs {
        private static final RedisZSetCommands.ZAddArgs NONE = new RedisZSetCommands.ZAddArgs(EnumSet.noneOf(RedisZSetCommands.ZAddArgs.Flag.class));
        private final Set<RedisZSetCommands.ZAddArgs.Flag> flags;

        private ZAddArgs(Set<RedisZSetCommands.ZAddArgs.Flag> flags) {
            this.flags = flags;
        }

        /**
         * @return {@link RedisZSetCommands.ZAddArgs} 的新实例，没有设置任何标志。
         */
        public static RedisZSetCommands.ZAddArgs empty() {
            return new RedisZSetCommands.ZAddArgs(EnumSet.noneOf(RedisZSetCommands.ZAddArgs.Flag.class));
        }

        /**
         * @return 没有设置 {@link RedisZSetCommands.ZAddArgs.Flag#NX} 的 {@link RedisZSetCommands.ZAddArgs} 的新实例
         */
        public static RedisZSetCommands.ZAddArgs ifNotExists() {
            return empty().nx();
        }

        /**
         * @return 没有设置 {@link RedisZSetCommands.ZAddArgs.Flag#NX} 的 {@link RedisZSetCommands.ZAddArgs} 的新实例
         */
        public static RedisZSetCommands.ZAddArgs ifExists() {
            return empty().xx();
        }

        /**
         * 仅更新已存在的元素
         *
         * @return this.
         */
        public RedisZSetCommands.ZAddArgs nx() {
            flags.add(RedisZSetCommands.ZAddArgs.Flag.NX);
            return this;
        }

        /**
         * 不要更新现有的元素
         *
         * @return this.
         */
        public RedisZSetCommands.ZAddArgs xx() {
            flags.add(RedisZSetCommands.ZAddArgs.Flag.XX);
            return this;
        }

        /**
         * 仅当新分数小于当前分数时更新现有元素
         *
         * @return this.
         */
        public RedisZSetCommands.ZAddArgs lt() {
            flags.add(RedisZSetCommands.ZAddArgs.Flag.LT);
            return this;
        }

        /**
         * 仅当新得分大于当前得分时更新现有元素
         *
         * @return this.
         */
        public RedisZSetCommands.ZAddArgs gt() {
            flags.add(RedisZSetCommands.ZAddArgs.Flag.GT);
            return this;
        }

        /**
         * 仅更新已存在的元素
         *
         * @return this.
         */
        public RedisZSetCommands.ZAddArgs ch() {
            flags.add(RedisZSetCommands.ZAddArgs.Flag.CH);
            return this;
        }

        /**
         * 仅更新已存在的元素
         *
         * @return this.
         */
        public boolean contains(RedisZSetCommands.ZAddArgs.Flag flag) {
            return flags.contains(flag);
        }

        /**
         * @return {@literal true} 如果没有设置标志
         */
        public boolean isEmpty() {
            return !flags.isEmpty();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            RedisZSetCommands.ZAddArgs zAddArgs = (RedisZSetCommands.ZAddArgs) o;
            return ObjectUtils.nullSafeEquals(flags, zAddArgs.flags);
        }

        @Override
        public int hashCode() {
            return ObjectUtils.nullSafeHashCode(flags);
        }

        public enum Flag {
            /**
             * 仅更新已存在的元素
             */
            XX,
            /**
             * 不要更新现有的元素
             */
            NX,
            /**
             * 仅当新得分大于当前得分时更新现有元素
             */
            GT,
            /**
             * 仅当新分数小于当前分数时更新现有元素
             */
            LT,
            /**
             * 将返回值从添加的新元素数修改为更改的元素总数
             */
            CH
        }
    }

    /**
     * 将 {@code value} 添加到 {@code key} 的排序集，或者更新它的 {@code score}（如果它已经存在）
     *
     * @param key   不能是 {@literal null}
     * @param score score
     * @param value value
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zadd">Redis 文档: ZADD</a>
     */
    default Boolean zAdd(byte[] key, double score, byte[] value) {
        return zAdd(key, score, value, RedisZSetCommands.ZAddArgs.NONE);
    }

    /**
     * 将 {@code value} 添加到 {@code key} 处的已排序集合，或根据给定的 {@link RedisZSetCommands.ZAddArgs args} 更新其 {@code score}
     *
     * @param key   不能是 {@literal null}
     * @param score score
     * @param value value
     * @param args  不能为 {@literal null} ，请改用 {@link RedisZSetCommands.ZAddArgs#empty()}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zadd">Redis 文档: ZADD</a>
     */
    Boolean zAdd(byte[] key, double score, byte[] value, RedisZSetCommands.ZAddArgs args);

    /**
     * 将 {@code tuples} 添加到 {@code key} 处的排序集，或者更新其 {@code score} （如果它已经存在）
     *
     * @param key    不能是 {@literal null}
     * @param tuples 不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zadd">Redis 文档: ZADD</a>
     */
    default Long zAdd(byte[] key, Set<RedisZSetCommands.Tuple> tuples) {
        return zAdd(key, tuples, RedisZSetCommands.ZAddArgs.NONE);
    }

    /**
     * 将 {@code tuples} 添加到 {@code key} 处的排序集，或根据给定的 {@link RedisZSetCommands.ZAddArgs args} 更新其 {@code score}
     *
     * @param key    不能是 {@literal null}
     * @param tuples 不能是 {@literal null}
     * @param args   不能为 {@literal null} ，请改用 {@link RedisZSetCommands.ZAddArgs#empty()}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zadd">Redis 文档: ZADD</a>
     */
    Long zAdd(byte[] key, Set<RedisZSetCommands.Tuple> tuples, RedisZSetCommands.ZAddArgs args);

    /**
     * 从排序集中删除 {@code value} 。返回已删除元素的数量。
     *
     * @param key    不能是 {@literal null}
     * @param values 不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zrem">Redis 文档: ZREM</a>
     */
    Long zRem(byte[] key, byte[]... values);

    /**
     * 按 {@code Increment} 递增排序集中的 {@code value} 元素的分数
     *
     * @param key       不能是 {@literal null}
     * @param increment 不能是 {@literal null}
     * @param value     value
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zincrby">Redis 文档: ZINCRBY</a>
     */
    Double zIncrBy(byte[] key, double increment, byte[] value);

    /**
     * 从 {@code key} 处的排序集中获取随机元素
     *
     * @param key 不能是 {@literal null}
     * @return 可以是 {@literal null}
     * @see <a href="https://redis.io/commands/zrandmember">Redis 文档: ZRANDMEMBER</a>
     */
    byte[] zRandMember(byte[] key);

    /**
     * 从 {@code key} 处的排序集中获取 {@code count} 个随机元素
     *
     * @param key   不能是 {@literal null}
     * @param count 如果提供的 {@code count} 参数为正，则返回一个不同字段的列表，以 {@code count} 或设置大小为上限。如果 {@code count} 为负，则行为将发生变化，并且允许命令多次返回相同的值。在这种情况下，返回值的数量是指定计数的绝对值。
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zrandmember">Redis 文档: ZRANDMEMBER</a>
     */
    List<byte[]> zRandMember(byte[] key, long count);

    /**
     * 从 {@code key} 处的排序集中获取随机元素
     *
     * @param key 不能是 {@literal null}
     * @return 可以是 {@literal null}
     * @see <a href="https://redis.io/commands/zrandmember">Redis 文档: ZRANDMEMBER</a>
     */
    RedisZSetCommands.Tuple zRandMemberWithScore(byte[] key);

    /**
     * 从 {@code key} 处的排序集中获取 {@code count} 个随机元素
     *
     * @param key   不能是 {@literal null}
     * @param count 如果提供的 {@code count} 参数为正，则返回一个不同字段的列表，以 {@code count} 或设置大小为上限。如果 {@code count} 为负，则行为将发生变化，并且允许命令多次返回相同的值。在这种情况下，返回值的数量是指定计数的绝对值
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zrandmember">Redis 文档: ZRANDMEMBER</a>
     */
    List<RedisZSetCommands.Tuple> zRandMemberWithScore(byte[] key, long count);

    /**
     * 确定排序集中具有 {@code value} 的元素的索引
     *
     * @param key   不能是 {@literal null}
     * @param value value. 不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zrank">Redis 文档: ZRANK</a>
     */
    Long zRank(byte[] key, byte[] value);

    /**
     * 当得分从高到低时，确定排序集中具有 {@code value} 的元素的索引
     *
     * @param key   不能是 {@literal null}
     * @param value value
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zrevrank">Redis 文档: ZREVRANK</a>
     */
    Long zRevRank(byte[] key, byte[] value);

    /**
     * 从排序集中获取 {@code start} 和 {@code end} 之间的元素
     *
     * @param key   不能是 {@literal null}
     * @param start 不能是 {@literal null}
     * @param end   不能是 {@literal null}
     * @return 当键不存在或范围中没有成员时，为空 {@link Set} 。在管道/事务中使用时 {@literal null} 。
     * @see <a href="https://redis.io/commands/zrange">Redis 文档: ZRANGE</a>
     */
    Set<byte[]> zRange(byte[] key, long start, long end);

    /**
     * 从排序集获取 {@code start} 和 {@code end} 之间的 {@link RedisZSetCommands.Tuple}
     *
     * @param key   不能是 {@literal null}
     * @param start 不能是 {@literal null}
     * @param end   不能是 {@literal null}
     * @return 当键不存在或范围中没有成员时，为空 {@link Set} 。在管道/事务中使用时 {@literal null}
     * @see <a href="https://redis.io/commands/zrange">Redis 文档: ZRANGE</a>
     */
    Set<RedisZSetCommands.Tuple> zRangeWithScores(byte[] key, long start, long end);

    /**
     * 从排序集中获取分数介于 {@code min} 和 {@code max} 之间的元素
     *
     * @param key 不能是 {@literal null}
     * @param min 不能是 {@literal null}
     * @param max 不能是 {@literal null}
     * @return 当键不存在或范围中没有成员时，为空 {@link Set} 。在管道/事务中使用时 {@literal null}
     * @see <a href="https://redis.io/commands/zrangebyscore">Redis 文档: ZRANGEBYSCORE</a>
     */
    default Set<byte[]> zRangeByScore(byte[] key, double min, double max) {
        return zRangeByScore(key, new RedisZSetCommands.Range().gte(min).lte(max));
    }

    /**
     * 获取一组 {@link RedisZSetCommands.Tuple} ，其中分数在排序集的 {@code Range#min} 和 {@code Range#max} 之间
     *
     * @param key   不能是 {@literal null}
     * @param range 不能是 {@literal null}
     * @return 当键不存在或范围中没有成员时，为空 {@link Set} 。在管道/事务中使用时 {@literal null}
     * @see <a href="https://redis.io/commands/zrangebyscore">Redis 文档: ZRANGEBYSCORE</a>
     */
    default Set<RedisZSetCommands.Tuple> zRangeByScoreWithScores(byte[] key, RedisZSetCommands.Range range) {
        return zRangeByScoreWithScores(key, range, RedisZSetCommands.Limit.unlimited());
    }

    /**
     * 获取一组 {@link RedisZSetCommands.Tuple} ，其中分数在排序集的 {@code min} 和 {@code max} 之间
     *
     * @param key 不能是 {@literal null}
     * @param min 不能是 {@literal null}
     * @param max 不能是 {@literal null}
     * @return 当键不存在或范围中没有成员时，为空 {@link Set} 。在管道/事务中使用时 {@literal null}
     * @see <a href="https://redis.io/commands/zrangebyscore">Redis 文档: ZRANGEBYSCORE</a>
     */
    default Set<RedisZSetCommands.Tuple> zRangeByScoreWithScores(byte[] key, double min, double max) {
        return zRangeByScoreWithScores(key, new RedisZSetCommands.Range().gte(min).lte(max));
    }

    /**
     * 从排序集中获取范围从 {@code start} 到 {@code end} 的元素，其中得分介于 {@code min} 和 {@code max} 之间
     *
     * @param key    不能是 {@literal null}
     * @param min    不能是 {@literal null}
     * @param max    不能是 {@literal null}
     * @param offset 不能是 {@literal null}
     * @param count  不能是 {@literal null}
     * @return 当键不存在或范围中没有成员时，为空 {@link Set} 。在管道/事务中使用时 {@literal null}
     * @see <a href="https://redis.io/commands/zrangebyscore">Redis 文档: ZRANGEBYSCORE</a>
     */
    default Set<byte[]> zRangeByScore(byte[] key, double min, double max, long offset, long count) {
        return zRangeByScore(
                key,
                new RedisZSetCommands.Range().gte(min).lte(max),
                new RedisZSetCommands.Limit().offset(Long.valueOf(offset).intValue()).count(Long.valueOf(count).intValue())
        );
    }

    /**
     * 获取范围从 {@code start} 到 {@code end} 的 {@link RedisZSetCommands.Tuple} 集合，其中分数介于排序集合中的 {@code min} 和 {@code max} 之间
     *
     * @param key    不能是 {@literal null}
     * @param min    不能是 {@literal null}
     * @param max    不能是 {@literal null}
     * @param offset 不能是 {@literal null}
     * @param count  不能是 {@literal null}
     * @return 当键不存在或范围中没有成员时，为空 {@link Set} 。在管道/事务中使用时 {@literal null}
     * @see <a href="https://redis.io/commands/zrangebyscore">Redis 文档: ZRANGEBYSCORE</a>
     */
    default Set<RedisZSetCommands.Tuple> zRangeByScoreWithScores(byte[] key, double min, double max, long offset, long count) {
        return zRangeByScoreWithScores(
                key,
                new RedisZSetCommands.Range().gte(min).lte(max),
                new RedisZSetCommands.Limit().offset(Long.valueOf(offset).intValue()).count(Long.valueOf(count).intValue())
        );
    }

    /**
     * 获取范围从 {@code Limit#offset} 到 {@code Limit#offset + Limit#count} 的 {@link RedisZSetCommands.Tuple} 集合，其中分数介于排序集合中的 {@code Range#min} 和 {@code Range#max} 之间。
     *
     * @param key   不能是 {@literal null}
     * @param range 不能是 {@literal null}
     * @param limit 不能是 {@literal null}
     * @return 当键不存在或范围中没有成员时，为空 {@link Set} 。在管道/事务中使用时 {@literal null}
     * @see <a href="https://redis.io/commands/zrangebyscore">Redis 文档: ZRANGEBYSCORE</a>
     */
    Set<RedisZSetCommands.Tuple> zRangeByScoreWithScores(byte[] key, RedisZSetCommands.Range range, RedisZSetCommands.Limit limit);

    /**
     * 从从高到低排序的集合中获取范围从 {@code start} 到 {@code end} 的元素
     *
     * @param key   不能是 {@literal null}
     * @param start 不能是 {@literal null}
     * @param end   不能是 {@literal null}
     * @return 当键不存在或范围中没有成员时，为空 {@link Set} 。在管道/事务中使用时 {@literal null}
     * @see <a href="https://redis.io/commands/zrevrange">Redis 文档: ZREVRANGE</a>
     */
    Set<byte[]> zRevRange(byte[] key, long start, long end);

    /**
     * 获取范围从 {@code start} 到 {@code end} 的 {@link RedisZSetCommands.Tuple} 集合，从高到低排序
     *
     * @param key   不能是 {@literal null}
     * @param start 不能是 {@literal null}
     * @param end   不能是 {@literal null}
     * @return 当键不存在或范围中没有成员时，为空 {@link Set} 。在管道/事务中使用时 {@literal null}
     * @see <a href="https://redis.io/commands/zrevrange">Redis 文档: ZREVRANGE</a>
     */
    Set<RedisZSetCommands.Tuple> zRevRangeWithScores(byte[] key, long start, long end);

    /**
     * 从从高到低排序的集合中获取得分介于{@code min}和{@code max}之间的元素
     *
     * @param key 不能是 {@literal null}
     * @param min 不能是 {@literal null}
     * @param max 不能是 {@literal null}
     * @return 当键不存在或范围中没有成员时，为空 {@link Set} 。在管道/事务中使用时 {@literal null}
     * @see <a href="https://redis.io/commands/zrevrange">Redis 文档: ZREVRANGE</a>
     */
    default Set<byte[]> zRevRangeByScore(byte[] key, double min, double max) {
        return zRevRangeByScore(key, new RedisZSetCommands.Range().gte(min).lte(max));
    }

    /**
     * 从从高到低排序的集合中获取分数介于 {@code Range#min} 和 {@code Range#max} 之间的元素
     *
     * @param key   不能是 {@literal null}
     * @param range 不能是 {@literal null}
     * @return 当键不存在或范围中没有成员时，为空 {@link Set} 。在管道/事务中使用时 {@literal null}
     * @see <a href="https://redis.io/commands/zrevrangebyscore">Redis 文档: ZREVRANGEBYSCORE</a>
     */
    default Set<byte[]> zRevRangeByScore(byte[] key, RedisZSetCommands.Range range) {
        return zRevRangeByScore(key, range, RedisZSetCommands.Limit.unlimited());
    }

    /**
     * 获取一组 {@link RedisZSetCommands.Tuple} ，其中得分介于 {@code min} 和 {@code max} 之间，从高到低排序
     *
     * @param key 不能是 {@literal null}
     * @param min 不能是 {@literal null}
     * @param max 不能是 {@literal null}
     * @return 当键不存在或范围中没有成员时，为空 {@link Set} 。在管道/事务中使用时 {@literal null}
     * @see <a href="https://redis.io/commands/zrevrangebyscore">Redis 文档: ZREVRANGEBYSCORE</a>
     */
    default Set<RedisZSetCommands.Tuple> zRevRangeByScoreWithScores(byte[] key, double min, double max) {
        return zRevRangeByScoreWithScores(key, new RedisZSetCommands.Range().gte(min).lte(max), RedisZSetCommands.Limit.unlimited());
    }

    /**
     * 获取范围从 {@code start} 到 {@code end} 的元素，其中得分介于 {@code min} 和 {@code max} 之间，排序顺序为 高->低
     *
     * @param key    不能是 {@literal null}
     * @param min    不能是 {@literal null}
     * @param max    不能是 {@literal null}
     * @param offset 不能是 {@literal null}
     * @param count  不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zrevrangebyscore">Redis 文档: ZREVRANGEBYSCORE</a>
     */
    default Set<byte[]> zRevRangeByScore(byte[] key, double min, double max, long offset, long count) {
        return zRevRangeByScore(
                key,
                new RedisZSetCommands.Range().gte(min).lte(max),
                new RedisZSetCommands.Limit().offset(Long.valueOf(offset).intValue()).count(Long.valueOf(count).intValue())
        );
    }

    /**
     * 获取范围从 {@code Limit#offset} 到 {@code Limit#offset + Limit#count} 的元素，其中得分介于 {@code Range#min} 和 {@code Range#max} 之间，排序顺序为高->低。
     *
     * @param key   不能是 {@literal null}
     * @param range 不能是 {@literal null}
     * @param limit 不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zrevrangebyscore">Redis 文档: ZREVRANGEBYSCORE</a>
     */
    Set<byte[]> zRevRangeByScore(byte[] key, RedisZSetCommands.Range range, RedisZSetCommands.Limit limit);

    /**
     * 获取范围从 {@code start} 到 {@code end} 的 {@link RedisZSetCommands.Tuple} 集合，其中得分介于 {@code min} 和 {@code max} 之间，排序顺序为高->低。
     *
     * @param key    不能是 {@literal null}
     * @param min    不能是 {@literal null}
     * @param max    不能是 {@literal null}
     * @param offset 不能是 {@literal null}
     * @param count  不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zrevrangebyscore">Redis 文档: ZREVRANGEBYSCORE</a>
     */
    default Set<RedisZSetCommands.Tuple> zRevRangeByScoreWithScores(byte[] key, double min, double max, long offset, long count) {
        return zRevRangeByScoreWithScores(
                key,
                new RedisZSetCommands.Range().gte(min).lte(max),
                new RedisZSetCommands.Limit().offset(Long.valueOf(offset).intValue()).count(Long.valueOf(count).intValue())
        );
    }

    /**
     * 获取 {@link RedisZSetCommands.Tuple} 的集合，其中得分介于 {@code Range#min} 和 {@code Range#max} 之间，从高到低排序
     *
     * @param key   不能是 {@literal null}
     * @param range 不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zrevrangebyscore">Redis 文档: ZREVRANGEBYSCORE</a>
     */
    default Set<RedisZSetCommands.Tuple> zRevRangeByScoreWithScores(byte[] key, RedisZSetCommands.Range range) {
        return zRevRangeByScoreWithScores(key, range, RedisZSetCommands.Limit.unlimited());
    }

    /**
     * 获取范围从 {@code Limit#offset} 到 {@code Limit#count} 的 {@link RedisZSetCommands.Tuple} 集合，其中分数介于 {@code Range#min} 和 {@code Range#max} 之间，排序顺序为高->低
     *
     * @param key   不能是 {@literal null}
     * @param range 不能是 {@literal null}
     * @param limit 不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zrevrangebyscore">Redis 文档: ZREVRANGEBYSCORE</a>
     */
    Set<RedisZSetCommands.Tuple> zRevRangeByScoreWithScores(byte[] key, RedisZSetCommands.Range range, RedisZSetCommands.Limit limit);

    /**
     * 计数排序集中得分介于 {@code min} 和 {@code max} 之间的元素数
     *
     * @param key 不能是 {@literal null}
     * @param min 不能是 {@literal null}
     * @param max 不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zcount">Redis 文档: ZCOUNT</a>
     */
    default Long zCount(byte[] key, double min, double max) {
        return zCount(key, new RedisZSetCommands.Range().gte(min).lte(max));
    }

    /**
     * 计数排序集中得分介于 {@code Range#min} 和 {@code Range#max} 之间的元素数
     *
     * @param key   不能是 {@literal null}
     * @param range 不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zcount">Redis 文档: ZCOUNT</a>
     */
    Long zCount(byte[] key, RedisZSetCommands.Range range);

    /**
     * 应用字典排序，计算排序集内值介于 {@code Range#min} 和 {@code Range#max} 之间的元素数
     *
     * @param key   不能是 {@literal null}
     * @param range 不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zlexcount">Redis 文档: ZLEXCOUNT</a>
     */
    Long zLexCount(byte[] key, RedisZSetCommands.Range range);

    /**
     * 移除并返回在 {@code key} 处排序集中得分最低的值
     *
     * @param key 不能是 {@literal null}
     * @return 当排序集为空或在管道/事务中使用时， {@literal null}
     * @see <a href="https://redis.io/commands/zpopmin">Redis 文档: ZPOPMIN</a>
     */
    RedisZSetCommands.Tuple zPopMin(byte[] key);

    /**
     * 删除并返回 {@code count} 值，其中 {@code key} 处的排序集的得分最低
     *
     * @param key   不能是 {@literal null}
     * @param count 要弹出的元素数
     * @return 当排序集为空或在管道/事务中使用时， {@literal null}
     * @see <a href="https://redis.io/commands/zpopmin">Redis 文档: ZPOPMIN</a>
     */
    Set<RedisZSetCommands.Tuple> zPopMin(byte[] key, long count);

    /**
     * 移除并返回在 {@code key} 处排序集中得分最低的值。 <br />
     * <b>阻止连接<b>，直到元素可用或达到 {@code timeout}
     *
     * @param key     不能是 {@literal null}
     * @param timeout 不能是 {@literal null}
     * @param unit    不能是 {@literal null}
     * @return 可以是 {@literal null}
     * @see <a href="https://redis.io/commands/bzpopmin">Redis 文档: BZPOPMIN</a>
     */
    RedisZSetCommands.Tuple bZPopMin(byte[] key, long timeout, TimeUnit unit);

    /**
     * 从 {@code key} 处的排序集中删除并返回得分最高的值
     *
     * @param key 不能是 {@literal null}
     * @return 当排序集为空或在管道/事务中使用时， {@literal null}
     * @see <a href="https://redis.io/commands/zpopmax">Redis 文档: ZPOPMAX</a>
     */
    RedisZSetCommands.Tuple zPopMax(byte[] key);

    /**
     * 删除并返回 {@code count} 值，其中 {@code key} 处的排序集的得分最高
     *
     * @param key   不能是 {@literal null}
     * @param count 要弹出的元素数
     * @return 当排序集为空或在管道/事务中使用时， {@literal null}
     * @see <a href="https://redis.io/commands/zpopmax">Redis 文档: ZPOPMAX</a>
     */
    Set<RedisZSetCommands.Tuple> zPopMax(byte[] key, long count);

    /**
     * 从 {@code key} 处的排序集中删除并返回得分最高的值。 <br />
     * <b>阻止连接<b>，直到元素可用或达到 {@code timeout}
     *
     * @param key     不能是 {@literal null}
     * @param timeout 不能是 {@literal null}
     * @param unit    不能是 {@literal null}
     * @return 可以是 {@literal null}
     * @see <a href="https://redis.io/commands/bzpopmax">Redis 文档: BZPOPMAX</a>
     */
    RedisZSetCommands.Tuple bZPopMax(byte[] key, long timeout, TimeUnit unit);

    /**
     * 使用 {@code key} 获取排序集的大小
     *
     * @param key 不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zcard">Redis 文档: ZCARD</a>
     */
    Long zCard(byte[] key);

    /**
     * 从关键字为 {@code key} 的排序集中获取具有 {@code value} 的元素的分数
     *
     * @param key   不能是 {@literal null}
     * @param value value
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zscore">Redis 文档: ZSCORE</a>
     */
    Double zScore(byte[] key, byte[] value);

    /**
     * 从关键字为 {@code key} 的排序集中获取具有 {@code values} 的元素的分数
     *
     * @param key    不能是 {@literal null}
     * @param values the values.
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zmscore">Redis 文档: ZMSCORE</a>
     */
    List<Double> zMScore(byte[] key, byte[]... values);

    /**
     * 使用 {@code key} 从排序集中删除 {@code start} 和 {@code end} 之间的元素
     *
     * @param key   不能是 {@literal null}
     * @param start 不能是 {@literal null}
     * @param end   不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zremrangebyrank">Redis 文档: ZREMRANGEBYRANK</a>
     */
    Long zRemRange(byte[] key, long start, long end);

    /**
     * 删除词典化的 {@link RedisZSetCommands.Range} 之间的所有元素
     *
     * @param key   不能是 {@literal null}
     * @param range 不能是 {@literal null}
     * @return 移除的元件数量，或 {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zremrangebylex">Redis 文档: ZREMRANGEBYLEX</a>
     */
    Long zRemRangeByLex(byte[] key, RedisZSetCommands.Range range);

    /**
     * 从带有 {@code key} 的排序集中删除分数介于 {@code min} 和 {@code max} 之间的元素
     *
     * @param key 不能是 {@literal null}
     * @param min 不能是 {@literal null}
     * @param max 不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zremrangebyscore">Redis 文档: ZREMRANGEBYSCORE</a>
     */
    default Long zRemRangeByScore(byte[] key, double min, double max) {
        return zRemRangeByScore(key, new RedisZSetCommands.Range().gte(min).lte(max));
    }

    /**
     * 从带有 {@code key} 的排序集中删除分数介于 {@code Range#min} 和 {@code Range#max} 之间的元素
     *
     * @param key   不能是 {@literal null}
     * @param range 不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zremrangebyscore">Redis 文档: ZREMRANGEBYSCORE</a>
     */
    Long zRemRangeByScore(byte[] key, RedisZSetCommands.Range range);

    /**
     * 差异排序 {@code sets}
     *
     * @param sets 不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zdiff">Redis 文档: ZDIFF</a>
     */
    Set<byte[]> zDiff(byte[]... sets);

    /**
     * 差异排序 {@code sets}
     *
     * @param sets 不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zdiff">Redis 文档: ZDIFF</a>
     */
    Set<RedisZSetCommands.Tuple> zDiffWithScores(byte[]... sets);

    /**
     * 区分排序的 {@code sets} 并将结果存储在目标 {@code destKey} 中
     *
     * @param destKey 不能是 {@literal null}
     * @param sets    不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zdiffstore">Redis 文档: ZDIFFSTORE</a>
     */
    Long zDiffStore(byte[] destKey, byte[]... sets);

    /**
     * 相交排序的 {@code sets}
     *
     * @param sets 不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zinter">Redis 文档: ZINTER</a>
     */
    Set<byte[]> zInter(byte[]... sets);

    /**
     * 相交排序 {@code sets}
     *
     * @param sets 不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zinter">Redis 文档: ZINTER</a>
     */
    Set<RedisZSetCommands.Tuple> zInterWithScores(byte[]... sets);

    /**
     * 相交排序 {@code sets}
     *
     * @param aggregate 不能是 {@literal null}
     * @param weights   不能是 {@literal null}
     * @param sets      不能是 {@literal null}
     * @see <a href="https://redis.io/commands/zinter">Redis 文档: ZINTER</a>
     */
    default Set<RedisZSetCommands.Tuple> zInterWithScores(RedisZSetCommands.Aggregate aggregate, int[] weights, byte[]... sets) {
        return zInterWithScores(aggregate, RedisZSetCommands.Weights.of(weights), sets);
    }

    /**
     * 相交排序 {@code sets}
     *
     * @param aggregate 不能是 {@literal null}
     * @param weights   不能是 {@literal null}
     * @param sets      不能是 {@literal null}
     * @see <a href="https://redis.io/commands/zinter">Redis 文档: ZINTER</a>
     */
    Set<RedisZSetCommands.Tuple> zInterWithScores(RedisZSetCommands.Aggregate aggregate, RedisZSetCommands.Weights weights, byte[]... sets);

    /**
     * 将排序的 {@code sets} 相交并将结果存储在目标 {@code destKey} 中
     *
     * @param destKey 不能是 {@literal null}
     * @param sets    不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zinterstore">Redis 文档: ZINTERSTORE</a>
     */
    Long zInterStore(byte[] destKey, byte[]... sets);

    /**
     * 将排序的 {@code sets} 相交并将结果存储在目标 {@code destKey} 中
     *
     * @param destKey   不能是 {@literal null}
     * @param aggregate 不能是 {@literal null}
     * @param weights   不能是 {@literal null}
     * @param sets      不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zinterstore">Redis 文档: ZINTERSTORE</a>
     */
    default Long zInterStore(byte[] destKey, RedisZSetCommands.Aggregate aggregate, int[] weights, byte[]... sets) {
        return zInterStore(destKey, aggregate, RedisZSetCommands.Weights.of(weights), sets);
    }

    /**
     * 将排序的 {@code sets} 相交并将结果存储在目标 {@code destKey} 中
     *
     * @param destKey   不能是 {@literal null}
     * @param aggregate 不能是 {@literal null}
     * @param weights   不能是 {@literal null}
     * @param sets      不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zinterstore">Redis 文档: ZINTERSTORE</a>
     */
    Long zInterStore(byte[] destKey, RedisZSetCommands.Aggregate aggregate, RedisZSetCommands.Weights weights, byte[]... sets);

    /**
     * 联合排序 {@code sets}
     *
     * @param sets 不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zunion">Redis 文档: ZUNION</a>
     */
    Set<byte[]> zUnion(byte[]... sets);

    /**
     * 联合排序 {@code sets}
     *
     * @param sets 不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zunion">Redis 文档: ZUNION</a>
     */
    Set<RedisZSetCommands.Tuple> zUnionWithScores(byte[]... sets);

    /**
     * 联合排序 {@code sets}
     *
     * @param aggregate 不能是 {@literal null}
     * @param weights   不能是 {@literal null}
     * @param sets      不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zunion">Redis 文档: ZUNION</a>
     */
    default Set<RedisZSetCommands.Tuple> zUnionWithScores(RedisZSetCommands.Aggregate aggregate, int[] weights, byte[]... sets) {
        return zUnionWithScores(aggregate, RedisZSetCommands.Weights.of(weights), sets);
    }

    /**
     * 联合排序 {@code sets}
     *
     * @param aggregate 不能是 {@literal null}
     * @param weights   不能是 {@literal null}
     * @param sets      不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zunion">Redis 文档: ZUNION</a>
     */
    Set<RedisZSetCommands.Tuple> zUnionWithScores(RedisZSetCommands.Aggregate aggregate, RedisZSetCommands.Weights weights, byte[]... sets);

    /**
     * 联合排序 {@code sets}
     *
     * @param sets 不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zunionstore">Redis 文档: ZUNIONSTORE</a>
     */
    Long zUnionStore(byte[] destKey, byte[]... sets);

    /**
     * 联合排序的 {@code sets} 并将结果存储在目标 {@code destKey} 中
     *
     * @param destKey   不能是 {@literal null}
     * @param aggregate 不能是 {@literal null}
     * @param weights   不能是 {@literal null}
     * @param sets      不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zunionstore">Redis 文档: ZUNIONSTORE</a>
     */
    default Long zUnionStore(byte[] destKey, RedisZSetCommands.Aggregate aggregate, int[] weights, byte[]... sets) {
        return zUnionStore(destKey, aggregate, RedisZSetCommands.Weights.of(weights), sets);
    }

    /**
     * 联合排序的 {@code sets} 并将结果存储在目标 {@code destKey} 中
     *
     * @param destKey   不能是 {@literal null}
     * @param aggregate 不能是 {@literal null}
     * @param weights   不能是 {@literal null}
     * @param sets      不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zunionstore">Redis 文档: ZUNIONSTORE</a>
     */
    Long zUnionStore(byte[] destKey, RedisZSetCommands.Aggregate aggregate, RedisZSetCommands.Weights weights, byte[]... sets);

    /**
     * 使用 {@link Cursor} 在 {@code key} 处的排序集中迭代元素
     *
     * @param key     不能是 {@literal null}
     * @param options 不能是 {@literal null}
     * @see <a href="https://redis.io/commands/zscan">Redis 文档: ZSCAN</a>
     */
    Cursor<RedisZSetCommands.Tuple> zScan(byte[] key, ScanOptions options);

    /**
     * 从排序集中获取分数介于 {@code min} 和 {@code max} 之间的元素
     *
     * @param key 不能是 {@literal null}
     * @param min 不能是 {@literal null}
     * @param max 不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zrangebyscore">Redis 文档: ZRANGEBYSCORE</a>
     */
    default Set<byte[]> zRangeByScore(byte[] key, String min, String max) {
        return zRangeByScore(key, new RedisZSetCommands.Range().gte(min).lte(max));
    }

    /**
     * 从排序集中获取分数介于 {@code Range#min} 和 {@code Range#max} 之间的元素
     *
     * @param key   不能是 {@literal null}
     * @param range 不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zrangebyscore">Redis 文档: ZRANGEBYSCORE</a>
     */
    default Set<byte[]> zRangeByScore(byte[] key, RedisZSetCommands.Range range) {
        return zRangeByScore(key, range, RedisZSetCommands.Limit.unlimited());
    }

    /**
     * 从排序集中获取范围从 {@code start} 到 {@code end} 的元素，其中得分介于 {@code min} 和 {@code max} 之间
     *
     * @param key    不能是 {@literal null}
     * @param min    不能是 {@literal null}
     * @param max    不能是 {@literal null}
     * @param offset 不能是 {@literal null}
     * @param count  不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zrangebyscore">Redis 文档: ZRANGEBYSCORE</a>
     */
    Set<byte[]> zRangeByScore(byte[] key, String min, String max, long offset, long count);

    /**
     * 获取范围从 {@code Limit#count} 到 {@code Limit#offset} 的元素，其中分数在排序集的 {@code Range#min} 和 {@code Range#max} 之间
     *
     * @param key   不能是 {@literal null}
     * @param range 不能是 {@literal null}
     * @param limit 不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zrangebyscore">Redis 文档: ZRANGEBYSCORE</a>
     */
    Set<byte[]> zRangeByScore(byte[] key, RedisZSetCommands.Range range, RedisZSetCommands.Limit limit);

    /**
     * 按字典顺序在 {@literal key} 处获取排序集中的所有元素
     *
     * @param key 不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zrangebylex">Redis 文档: ZRANGEBYLEX</a>
     */
    default Set<byte[]> zRangeByLex(byte[] key) {
        return zRangeByLex(key, RedisZSetCommands.Range.unbounded());
    }

    /**
     * 按字典顺序从 {@literal key} 处的排序集获取 {@link RedisZSetCommands.Range} 中的所有元素
     *
     * @param key   不能是 {@literal null}
     * @param range 不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zrangebylex">Redis 文档: ZRANGEBYLEX</a>
     */
    default Set<byte[]> zRangeByLex(byte[] key, RedisZSetCommands.Range range) {
        return zRangeByLex(key, range, RedisZSetCommands.Limit.unlimited());
    }

    /**
     * 按字典顺序从 {@literal key} 处的排序集获取 {@link RedisZSetCommands.Range} 中的所有元素。通过 {@link RedisZSetCommands.Limit} 限制结果
     *
     * @param key   不能是 {@literal null}
     * @param range 不能是 {@literal null}
     * @param limit 不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zrangebylex">Redis 文档: ZRANGEBYLEX</a>
     */
    Set<byte[]> zRangeByLex(byte[] key, RedisZSetCommands.Range range, RedisZSetCommands.Limit limit);

    /**
     * 在 {@literal key} 处以反向字典顺序获取排序集中的所有元素
     *
     * @param key 不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zrevrangebylex">Redis 文档: ZREVRANGEBYLEX</a>
     */
    default Set<byte[]> zRevRangeByLex(byte[] key) {
        return zRevRangeByLex(key, RedisZSetCommands.Range.unbounded());
    }

    /**
     * 从 {@literal key} 处的已排序集合中以反向字典顺序获取 {@link RedisZSetCommands.Range} 中的所有元素
     *
     * @param key   不能是 {@literal null}
     * @param range 不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zrevrangebylex">Redis 文档: ZREVRANGEBYLEX</a>
     */
    default Set<byte[]> zRevRangeByLex(byte[] key, RedisZSetCommands.Range range) {
        return zRevRangeByLex(key, range, RedisZSetCommands.Limit.unlimited());
    }

    /**
     * 从 {@literal key} 处的已排序集合中以反向字典顺序获取 {@link RedisZSetCommands.Range} 中的所有元素。
     * 通过 {@link RedisZSetCommands.Limit} 限制结果。
     *
     * @param key   不能是 {@literal null}
     * @param range 不能是 {@literal null}
     * @param limit 不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/zrevrangebylex">Redis 文档: ZREVRANGEBYLEX</a>
     */
    Set<byte[]> zRevRangeByLex(byte[] key, RedisZSetCommands.Range range, RedisZSetCommands.Limit limit);
}
