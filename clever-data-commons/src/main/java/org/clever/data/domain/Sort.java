package org.clever.data.domain;

import org.clever.data.util.Streamable;
import org.clever.util.Assert;
import org.clever.util.StringUtils;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 查询的排序选项。您必须至少提供一个要排序的属性列表，该列表不得包含 {@literal null} 或空字符串。
 * 方向默认为 {@link Sort#DEFAULT_DIRECTION}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/27 13:20 <br/>
 */
public class Sort implements Streamable<org.clever.data.domain.Sort.Order>, Serializable {
    private static final long serialVersionUID = 5737186511678863905L;
    private static final Sort UNSORTED = Sort.by(new Order[0]);
    public static final Direction DEFAULT_DIRECTION = Direction.ASC;

    private final List<Order> orders;

    protected Sort(List<Order> orders) {
        this.orders = orders;
    }

    /**
     * 创建一个新的 {@link Sort} 实例
     *
     * @param direction  默认为 {@link Sort#DEFAULT_DIRECTION} （也适用于 {@literal null} 情况）
     * @param properties 不得为 {@literal null} 或包含 {@literal null} 或空字符串
     */
    private Sort(Direction direction, List<String> properties) {
        if (properties == null || properties.isEmpty()) {
            throw new IllegalArgumentException("You have to provide at least one property to sort by");
        }
        this.orders = properties.stream().map(it -> new Order(direction, it)).collect(Collectors.toList());
    }

    /**
     * 为给定的属性创建一个新的 {@link Sort}
     *
     * @param properties 不能是 {@literal null}
     */
    public static Sort by(String... properties) {
        Assert.notNull(properties, "Properties must not be null");
        return properties.length == 0 ? Sort.unsorted() : new Sort(DEFAULT_DIRECTION, Arrays.asList(properties));
    }

    /**
     * 为给定的 {@link Order} 创建一个新的 {@link Sort}
     *
     * @param orders 不能是 {@literal null}
     */
    public static Sort by(List<Order> orders) {
        Assert.notNull(orders, "Orders must not be null");
        return orders.isEmpty() ? Sort.unsorted() : new Sort(orders);
    }

    /**
     * 为给定的 {@link Order} 创建一个新的 {@link Sort}
     *
     * @param orders 不能是 {@literal null}
     */
    public static Sort by(Order... orders) {
        Assert.notNull(orders, "Orders must not be null");
        return new Sort(Arrays.asList(orders));
    }

    /**
     * 为给定的 {@link Order} 创建一个新的 {@link Sort}
     *
     * @param direction  不能是 {@literal null}
     * @param properties 不能是 {@literal null}
     */
    public static Sort by(Direction direction, String... properties) {
        Assert.notNull(direction, "Direction must not be null");
        Assert.notNull(properties, "Properties must not be null");
        Assert.isTrue(properties.length > 0, "At least one property must be given");
        return Sort.by(Arrays.stream(properties).map(it -> new Order(direction, it)).collect(Collectors.toList()));
    }

    /**
     * 返回一个 {@link Sort} 实例，表示根本没有排序设置
     */
    public static Sort unsorted() {
        return UNSORTED;
    }

    /**
     * 返回具有当前设置但降序方向的新 {@link Sort}
     */
    public Sort descending() {
        return withDirection(Direction.DESC);
    }

    /**
     * 返回具有当前设置但升序方向的新 {@link Sort}
     */
    public Sort ascending() {
        return withDirection(Direction.ASC);
    }

    public boolean isSorted() {
        return !isEmpty();
    }

    @Override
    public boolean isEmpty() {
        return orders.isEmpty();
    }

    public boolean isUnsorted() {
        return !isSorted();
    }

    /**
     * 返回一个新的 {@link Sort}，由当前 {@link Sort} 的 {@link Order} 与给定的组合组成
     *
     * @param sort 不能是 {@literal null}.
     */
    public Sort and(Sort sort) {
        Assert.notNull(sort, "Sort must not be null");
        ArrayList<Order> these = new ArrayList<>(this.toList());
        for (Order order : sort) {
            these.add(order);
        }
        return Sort.by(these);
    }

    /**
     * 返回为给定属性注册的订单
     */
    public Order getOrderFor(String property) {
        for (Order order : this) {
            if (order.getProperty().equals(property)) {
                return order;
            }
        }
        return null;
    }

    public Iterator<Order> iterator() {
        return this.orders.iterator();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Sort)) {
            return false;
        }
        Sort that = (Sort) obj;
        return toList().equals(that.toList());
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + orders.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return isEmpty() ? "UNSORTED" : StringUtils.collectionToCommaDelimitedString(orders);
    }

    /**
     * 使用当前设置但给定的顺序方向创建一个新的 {@link Sort}
     */
    private Sort withDirection(Direction direction) {
        return Sort.by(stream().map(it -> it.with(direction)).collect(Collectors.toList()));
    }

    /**
     * 排序方向的枚举
     */
    public enum Direction {
        ASC, DESC;

        /**
         * 返回方向是否为升序
         */
        public boolean isAscending() {
            return this.equals(ASC);
        }

        /**
         * 返回方向是否下降
         */
        public boolean isDescending() {
            return this.equals(DESC);
        }

        /**
         * 返回给定 {@link String} 值的 {@link Direction} 枚举
         *
         * @throws IllegalArgumentException 如果无法将给定值解析为枚举值
         */
        public static Direction fromString(String value) {
            try {
                return Direction.valueOf(value.toUpperCase(Locale.US));
            } catch (Exception e) {
                throw new IllegalArgumentException(String.format("Invalid value '%s' for orders given; Has to be either 'desc' or 'asc' (case insensitive)", value), e);
            }
        }

        /**
         * 返回给定 {@link String} 的 {@link Direction} 枚举，如果无法解析为枚举值，则返回 null
         */
        public static Optional<Direction> fromOptionalString(String value) {
            try {
                return Optional.of(fromString(value));
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }
        }
    }

    /**
     * 可在 {@link Order} 表达式中使用的空处理提示的枚举
     */
    public enum NullHandling {
        /**
         * 让数据存储决定如何处理空值
         */
        NATIVE,
        /**
         * 提示使用的数据存储在非空条目之前对具有空值的条目进行排序
         */
        NULLS_FIRST,
        /**
         * 提示使用的数据存储在非空条目之后对具有空值的条目进行排序
         */
        NULLS_LAST;
    }

    /**
     * PropertyPath 实现了 {@link Direction} 和属性的配对。它用于为 {@link Sort} 提供输入
     */
    public static class Order implements Serializable {
        private static final long serialVersionUID = 1522511010900108987L;
        private static final boolean DEFAULT_IGNORE_CASE = false;
        private static final NullHandling DEFAULT_NULL_HANDLING = NullHandling.NATIVE;

        private final Direction direction;
        private final String property;
        private final boolean ignoreCase;
        private final NullHandling nullHandling;

        /**
         * 创建一个新的 {@link Order} 实例。如果订单是 {@literal null} 那么订单默认为 {@link Sort#DEFAULT_DIRECTION}
         *
         * @param direction 可以是{@literal null}，默认为{@link Sort#DEFAULT_DIRECTION}
         * @param property  不能是 {@literal null} 或空
         */
        public Order(Direction direction, String property) {
            this(direction, property, DEFAULT_IGNORE_CASE, DEFAULT_NULL_HANDLING);
        }

        /**
         * 创建一个新的 {@link Order} 实例。如果订单是 {@literal null} 那么订单默认为 {@link Sort#DEFAULT_DIRECTION}
         *
         * @param direction        可以是{@literal null}，默认为{@link Sort#DEFAULT_DIRECTION}
         * @param property         不能是 {@literal null} 或空
         * @param nullHandlingHint 不能是 {@literal null}
         */
        public Order(Direction direction, String property, NullHandling nullHandlingHint) {
            this(direction, property, DEFAULT_IGNORE_CASE, nullHandlingHint);
        }

        /**
         * 创建一个新的 {@link Order} 实例。获取单个属性。方向默认为 {@link Sort#DEFAULT_DIRECTION}
         *
         * @param property 不能是 {@literal null} 或空
         */
        public static Order by(String property) {
            return new Order(DEFAULT_DIRECTION, property);
        }

        /**
         * 创建一个新的 {@link Order} 实例。获取单个属性。方向是 {@link Direction#ASC} 和 NullHandling {@link NullHandling#NATIVE}
         *
         * @param property 不能是 {@literal null} 或空
         */
        public static Order asc(String property) {
            return new Order(Direction.ASC, property, DEFAULT_NULL_HANDLING);
        }

        /**
         * 创建一个新的 {@link Order} 实例。获取单个属性。方向是 {@link Direction#DESC} 和 NullHandling {@link NullHandling#NATIVE}
         *
         * @param property 不能是 {@literal null} 或空
         */
        public static Order desc(String property) {
            return new Order(Direction.DESC, property, DEFAULT_NULL_HANDLING);
        }

        /**
         * 创建一个新的 {@link Order} 实例。如果订单是 {@literal null} 那么订单默认为 {@link Sort#DEFAULT_DIRECTION}
         *
         * @param direction    可以是{@literal null}，默认为{@link Sort#DEFAULT_DIRECTION}
         * @param property     不能是 {@literal null} 或空
         * @param ignoreCase   如果排序不区分大小写，则为真。如果排序应区分大小写，则为 false
         * @param nullHandling 不能是 {@literal null}
         */
        private Order(Direction direction, String property, boolean ignoreCase, NullHandling nullHandling) {
            if (!StringUtils.hasText(property)) {
                throw new IllegalArgumentException("Property must not be null or empty");
            }
            this.direction = direction == null ? DEFAULT_DIRECTION : direction;
            this.property = property;
            this.ignoreCase = ignoreCase;
            this.nullHandling = nullHandling;
        }

        /**
         * 返回属性应排序的顺序
         */
        public Direction getDirection() {
            return direction;
        }

        /**
         * 返回要订购的属性
         */
        public String getProperty() {
            return property;
        }

        /**
         * 返回此属性的排序是否应升序
         */
        public boolean isAscending() {
            return this.direction.isAscending();
        }

        /**
         * 返回此属性的排序是否应降序
         */
        public boolean isDescending() {
            return this.direction.isDescending();
        }

        /**
         * 返回排序是区分大小写还是不区分大小写
         */
        public boolean isIgnoreCase() {
            return ignoreCase;
        }

        /**
         * 返回具有给定 {@link Direction} 的新 {@link Order}
         */
        public Order with(Direction direction) {
            return new Order(direction, this.property, this.ignoreCase, this.nullHandling);
        }

        /**
         * 返回一个新的{@link Order
         *
         * @param property 不能是 {@literal null} or empty.
         */
        public Order withProperty(String property) {
            return new Order(this.direction, property, this.ignoreCase, this.nullHandling);
        }

        /**
         * 为给定的属性返回一个新的 {@link Sort} 实例
         */
        public Sort withProperties(String... properties) {
            return Sort.by(this.direction, properties);
        }

        /**
         * 返回一个启用了不区分大小写排序的新 {@link Order}
         */
        public Order ignoreCase() {
            return new Order(direction, property, true, nullHandling);
        }

        /**
         * 返回具有给定 {@link NullHandling} 的 {@link Order}
         *
         * @param nullHandling 可以是 {@literal null}
         */
        public Order with(NullHandling nullHandling) {
            return new Order(direction, this.property, ignoreCase, nullHandling);
        }

        /**
         * 返回带有 {@link NullHandling#NULLS_FIRST} 作为空处理提示的 {@link Order}
         */
        public Order nullsFirst() {
            return with(NullHandling.NULLS_FIRST);
        }

        /**
         * 返回带有 {@link NullHandling#NULLS_LAST} 作为空处理提示的 {@link Order}
         */
        public Order nullsLast() {
            return with(NullHandling.NULLS_LAST);
        }

        /**
         * 返回带有 {@link NullHandling#NATIVE} 作为空处理提示的 {@link Order}
         */
        public Order nullsNative() {
            return with(NullHandling.NATIVE);
        }

        /**
         * 返回已使用的 {@link NullHandling} 提示，已使用的数据存储可以但可能不会遵守该提示
         */
        public NullHandling getNullHandling() {
            return nullHandling;
        }

        @Override
        public int hashCode() {
            int result = 17;
            result = 31 * result + direction.hashCode();
            result = 31 * result + property.hashCode();
            result = 31 * result + (ignoreCase ? 1 : 0);
            result = 31 * result + nullHandling.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Order)) {
                return false;
            }
            Order that = (Order) obj;
            return this.direction.equals(that.direction)
                    && this.property.equals(that.property)
                    && this.ignoreCase == that.ignoreCase
                    && this.nullHandling.equals(that.nullHandling);
        }

        @Override
        public String toString() {
            String result = String.format("%s: %s", property, direction);
            if (!NullHandling.NATIVE.equals(nullHandling)) {
                result += ", " + nullHandling;
            }
            if (ignoreCase) {
                result += ", ignoring case";
            }
            return result;
        }
    }
}
