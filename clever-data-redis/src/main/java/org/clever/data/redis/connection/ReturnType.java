package org.clever.data.redis.connection;

import org.clever.util.ClassUtils;

import java.util.List;


/**
 * 表示从Redis返回的数据类型，当前用于表示Redis脚本命令的预期返回类型
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 22:31 <br/>
 */
public enum ReturnType {
    /**
     * 返回为 boolean
     */
    BOOLEAN,

    /**
     * 返回为 {@link Long}
     */
    INTEGER,

    /**
     * 返回为 {@link List<Object>}
     */
    MULTI,

    /**
     * 返回为 {@literal byte[]}
     */
    STATUS,

    /**
     * 返回为 {@literal byte[]}
     */
    VALUE;

    /**
     * @param javaType 可以是 {@literal null} ，它转换为 {@link ReturnType#STATUS}
     * @return 从不为 {@literal null}
     */
    public static ReturnType fromJavaType(Class<?> javaType) {
        if (javaType == null) {
            return ReturnType.STATUS;
        }
        if (ClassUtils.isAssignable(List.class, javaType)) {
            return ReturnType.MULTI;
        }
        if (ClassUtils.isAssignable(Boolean.class, javaType)) {
            return ReturnType.BOOLEAN;
        }
        if (ClassUtils.isAssignable(Long.class, javaType)) {
            return ReturnType.INTEGER;
        }
        return ReturnType.VALUE;
    }
}
