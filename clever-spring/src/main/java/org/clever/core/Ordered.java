package org.clever.core;

/**
 * 用于集合元素排序的接口，order值越小优先级越高
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 15:02 <br/>
 */
public interface Ordered {
    /**
     * 最高的优先级
     */
    double HIGHEST_PRECEDENCE = Double.MIN_VALUE;
    /**
     * 最低的优先级
     */
    double LOWEST_PRECEDENCE = Double.MAX_VALUE;

    /**
     * 优先级，值越小优先级越高
     */
    double getOrder();
}
