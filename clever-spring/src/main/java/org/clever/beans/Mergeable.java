package org.clever.beans;

/**
 * 表示其值集可以与父对象的值集合并的对象的接口
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/05/26 15:24 <br/>
 */
public interface Mergeable {
    /**
     * 是否为此特定实例启用合并
     */
    boolean isMergeEnabled();

    /**
     * 将当前值集与提供的对象的值集合并。所提供的对象被视为父对象，被调用方的值集中的值必须覆盖所提供对象的值
     *
     * @param parent 要与之合并的对象
     * @return 合并操作的结果
     * @throws IllegalArgumentException 如果提供的父项为null
     * @throws IllegalStateException   如果未为此实例启用合并(即，mergeEnabled等于false)
     */
    Object merge(Object parent);
}
