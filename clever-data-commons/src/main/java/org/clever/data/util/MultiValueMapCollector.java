package org.clever.data.util;

import org.clever.util.CollectionUtils;
import org.clever.util.MultiValueMap;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * 用于从 {@link java.util.stream.Stream} 构建 {@link MultiValueMap} 的 {@link Collector}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 22:26 <br/>
 */
class MultiValueMapCollector<T, K, V> implements Collector<T, MultiValueMap<K, V>, MultiValueMap<K, V>> {
    private final Function<T, K> keyFunction;
    private final Function<T, V> valueFunction;

    private MultiValueMapCollector(Function<T, K> keyFunction, Function<T, V> valueFunction) {
        this.keyFunction = keyFunction;
        this.valueFunction = valueFunction;
    }

    static <T, K, V> MultiValueMapCollector<T, K, V> of(Function<T, K> keyFunction, Function<T, V> valueFunction) {
        return new MultiValueMapCollector<>(keyFunction, valueFunction);
    }

    @Override
    public Supplier<MultiValueMap<K, V>> supplier() {
        return () -> CollectionUtils.toMultiValueMap(new HashMap<>());
    }

    @Override
    public BiConsumer<MultiValueMap<K, V>, T> accumulator() {
        return (map, t) -> map.add(keyFunction.apply(t), valueFunction.apply(t));
    }

    @Override
    public BinaryOperator<MultiValueMap<K, V>> combiner() {
        return (map1, map2) -> {
            for (K key : map2.keySet()) {
                map1.addAll(key, map2.get(key));
            }
            return map1;
        };
    }

    @Override
    public Function<MultiValueMap<K, V>, MultiValueMap<K, V>> finisher() {
        return Function.identity();
    }

    @Override
    public Set<Characteristics> characteristics() {
        return EnumSet.of(Characteristics.IDENTITY_FINISH, Characteristics.UNORDERED);
    }
}
