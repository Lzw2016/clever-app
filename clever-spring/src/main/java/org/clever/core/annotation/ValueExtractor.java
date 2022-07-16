package org.clever.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * 用于从给定源对象，通常是{@link Annotation}、{@link Map}或{@link TypeMappedAnnotation}，中提取注解属性值的策略API
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 13:47 <br/>
 */
@FunctionalInterface
interface ValueExtractor {
    /**
     * 从{@link Object}中提取{@link Method}表示的注解属性
     */
    Object extract(Method attribute, Object object);
}
