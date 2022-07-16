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
    int HIGHEST_PRECEDENCE = Integer.MIN_VALUE;
    /**
     * 最低的优先级
     */
    int LOWEST_PRECEDENCE = Integer.MAX_VALUE;

    /**
     * 优先级，值越小优先级越高
     */
    int getOrder();
}
