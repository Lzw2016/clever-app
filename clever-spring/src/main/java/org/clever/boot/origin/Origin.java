package org.clever.boot.origin;

import java.io.File;
import java.util.*;

/**
 * 唯一表示项目原点的接口。例如，从{@link File}加载的项目的原点可能由文件名和行/列号组成。
 * <p>实现必须提供合理的{@code hashCode()}, {@code equals(...)}和{@code #toString()}实现。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/03 17:54 <br/>
 *
 * @see OriginProvider
 * @see TextResourceOrigin
 */
public interface Origin {
    /**
     * 返回此实例的父原点（如果有）。父原点提供创建此项的项的原点。
     *
     * @return 父原点或null
     * @see Origin#parentsFrom(Object)
     */
    default Origin getParent() {
        return null;
    }

    /**
     * 查找对象来源的{@link Origin}。检查源对象是{@link Origin}还是{@link OriginProvider}，并搜索异常堆栈。
     *
     * @param source 源对象或null
     * @return 可选 {@link Origin}
     */
    static Origin from(Object source) {
        if (source instanceof Origin) {
            return (Origin) source;
        }
        Origin origin = null;
        if (source instanceof OriginProvider) {
            origin = ((OriginProvider) source).getOrigin();
        }
        if (origin == null && source instanceof Throwable) {
            return from(((Throwable) source).getCause());
        }
        return origin;
    }

    /**
     * 查找对象来源的{@link Origin}的父对象。
     * 检查源对象是{@link Origin}还是{@link OriginProvider}，并搜索异常堆栈。
     * 提供截至根{@link Origin}的所有父级的列表，从最直接的父级开始。
     *
     * @param source 源对象或null
     * @return 父项列表或空列表（如果源为null、没有原点或没有父项）
     */
    static List<Origin> parentsFrom(Object source) {
        Origin origin = from(source);
        if (origin == null) {
            return Collections.emptyList();
        }
        Set<Origin> parents = new LinkedHashSet<>();
        origin = origin.getParent();
        while (origin != null && !parents.contains(origin)) {
            parents.add(origin);
            origin = origin.getParent();
        }
        return Collections.unmodifiableList(new ArrayList<>(parents));
    }
}
