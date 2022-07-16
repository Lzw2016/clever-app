package org.clever.boot.origin;

/**
 * 可以由对象实现的接口，该对象可以从给定的键中查找{@link Origin}信息。
 * 可以用于向现有类添加源支持。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/03 18:01 <br/>
 */
@FunctionalInterface
public interface OriginLookup<K> {
    /**
     * 返回给定键的原点，如果无法确定原点，则返回null。
     *
     * @param key 查找的key
     * @return 键或null的原点
     */
    Origin getOrigin(K key);

    /**
     * 如果此查找是不可变的，并且其内容永远不会更改，则返回true。
     * change.
     *
     * @return 如果查找是不可变的
     */
    default boolean isImmutable() {
        return false;
    }

    /**
     * 返回执行查找时应用的隐式前缀，如果未使用前缀，则返回null。
     * 前缀可用于消除否则会发生冲突的关键点的歧义。
     * 例如，如果多个应用程序在同一台机器上运行，则可以在每个应用程序上设置不同的前缀，以确保使用不同的环境变量。
     *
     * @return 查找类应用的前缀或null
     */
    default String getPrefix() {
        return null;
    }

    /**
     * 尝试从给定源查找原点。如果源不是{@link OriginLookup}，或者在查找过程中发生异常，则返回null。
     *
     * @param source 源对象
     * @param key    查找的key
     * @param <K>    key类型
     * @return an {@link Origin} or {@code null}
     */
    @SuppressWarnings("unchecked")
    static <K> Origin getOrigin(Object source, K key) {
        if (!(source instanceof OriginLookup)) {
            return null;
        }
        try {
            return ((OriginLookup<K>) source).getOrigin(key);
        } catch (Throwable ex) {
            return null;
        }
    }
}
