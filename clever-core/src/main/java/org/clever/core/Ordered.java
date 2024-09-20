package org.clever.core;

import java.util.Comparator;
import java.util.List;

/**
 * 用于集合元素排序的接口，order值越小优先级越高，
 * 比 {@link org.springframework.core.Ordered} 有更灵活的排序能力
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

    /**
     * 集合排序
     */
    static <T extends Ordered> void sort(List<T> callbacks) {
        Assert.notNull(callbacks, "参数 callbacks 不能为 null");
        callbacks.sort(Comparator.comparingDouble(Ordered::getOrder));
    }
}
