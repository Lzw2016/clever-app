package org.clever.core.annotation;

import java.lang.annotation.Annotation;

/**
 * 注解过滤器接口
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 13:07 <br/>
 */
@FunctionalInterface
public interface AnnotationFilter {
    /**
     * 匹配"java.lang"和"org.clever.lang"包的注解过滤器
     */
    AnnotationFilter PLAIN = packages("java.lang", "org.clever.lang");
    /**
     * 匹配"java"和"javax"包的注解过滤器
     */
    AnnotationFilter JAVA = packages("java", "javax");
    /**
     * 匹配所有注解的注解过滤器
     */
    AnnotationFilter ALL = new AnnotationFilter() {
        @Override
        public boolean matches(Annotation annotation) {
            return true;
        }

        @Override
        public boolean matches(Class<?> type) {
            return true;
        }

        @Override
        public boolean matches(String typeName) {
            return true;
        }

        @Override
        public String toString() {
            return "All annotations filtered";
        }
    };

    /**
     * 是否匹配，返回{@code false}就会过滤注解
     */
    default boolean matches(Annotation annotation) {
        return matches(annotation.annotationType());
    }

    /**
     * 是否匹配，返回{@code false}就会过滤注解
     */
    default boolean matches(Class<?> type) {
        return matches(type.getName());
    }

    /**
     * 是否匹配，返回{@code false}就会过滤注解
     */
    boolean matches(String typeName);

    /**
     * 创建一个根据包前缀过滤的注解过滤器
     *
     * @param packages 能匹配的包前缀
     */
    static AnnotationFilter packages(String... packages) {
        return new PackagesAnnotationFilter(packages);
    }
}
