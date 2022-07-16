package org.clever.boot.context.properties.bind;

import org.clever.boot.context.properties.bind.Binder.Context;
import org.clever.boot.context.properties.source.ConfigurationPropertyName;
import org.clever.boot.context.properties.source.ConfigurationPropertySource;

import java.util.function.Supplier;

/**
 * {@link Binder} 用于绑定聚合的内部策略 (Maps, Lists, Arrays).
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 23:16 <br/>
 */
abstract class AggregateBinder<T> {
    private final Context context;

    AggregateBinder(Context context) {
        this.context = context;
    }

    /**
     * 确定是否支持递归绑定。
     *
     * @param source 所有源的配置属性source或null。
     * @return 如果支持递归绑定
     */
    protected abstract boolean isAllowRecursiveBinding(ConfigurationPropertySource source);

    /**
     * 对聚合执行绑定。
     *
     * @param name          要绑定的配置属性名称
     * @param target        要绑定的目标
     * @param elementBinder 元素粘合剂
     * @return 绑定聚合或null
     */
    @SuppressWarnings("unchecked")
    final Object bind(ConfigurationPropertyName name, Bindable<?> target, AggregateElementBinder elementBinder) {
        Object result = bindAggregate(name, target, elementBinder);
        Supplier<?> value = target.getValue();
        if (result == null || value == null) {
            return result;
        }
        return merge((Supplier<T>) value, (T) result);
    }

    /**
     * 执行实际聚合绑定。
     *
     * @param name          要绑定的配置属性名称
     * @param target        要绑定的目标
     * @param elementBinder 元素粘合剂
     * @return 约束结果
     */
    protected abstract Object bindAggregate(ConfigurationPropertyName name,
                                            Bindable<?> target,
                                            AggregateElementBinder elementBinder);

    /**
     * 将任何其他元素合并到现有聚合中。
     *
     * @param existing   现有价值的供应商
     * @param additional 要合并的其他元素
     * @return 合并结果
     */
    protected abstract T merge(Supplier<T> existing, T additional);

    /**
     * 返回此活页夹正在使用的上下文。
     *
     * @return 上下文
     */
    protected final Context getContext() {
        return this.context;
    }

    /**
     * 用于提供聚合和缓存值的内部类。
     *
     * @param <T> 聚合类型
     */
    protected static class AggregateSupplier<T> {
        private final Supplier<T> supplier;
        private T supplied;

        public AggregateSupplier(Supplier<T> supplier) {
            this.supplier = supplier;
        }

        public T get() {
            if (this.supplied == null) {
                this.supplied = this.supplier.get();
            }
            return this.supplied;
        }

        public boolean wasSupplied() {
            return this.supplied != null;
        }
    }
}
