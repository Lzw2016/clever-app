package org.clever.validation.annotation;

import org.clever.core.annotation.AnnotationUtils;

import java.lang.annotation.Annotation;

/**
 * 用于处理验证注解的实用程序类。主要供框架内部使用。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/06/08 22:10 <br/>
 */
public abstract class ValidationAnnotationUtils {
    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    /**
     * 通过给定的注解确定任何验证提示。
     * <p>此实现检查 {@code @javax.validation.Valid}、{@link org.clever.validation.annotation.Validated} 以及名称以“Valid”开头的自定义注解。
     *
     * @param ann 注解（可能是验证注解）
     * @return 要应用的验证提示（可能是一个空数组），如果此注解不触发任何验证，则为 {@code null}
     */
    public static Object[] determineValidationHints(Annotation ann) {
        Class<? extends Annotation> annotationType = ann.annotationType();
        String annotationName = annotationType.getName();
        if ("javax.validation.Valid".equals(annotationName)) {
            return EMPTY_OBJECT_ARRAY;
        }
        Validated validatedAnn = AnnotationUtils.getAnnotation(ann, Validated.class);
        if (validatedAnn != null) {
            Object hints = validatedAnn.value();
            return convertValidationHints(hints);
        }
        if (annotationType.getSimpleName().startsWith("Valid")) {
            Object hints = AnnotationUtils.getValue(ann);
            return convertValidationHints(hints);
        }
        return null;
    }

    private static Object[] convertValidationHints(Object hints) {
        if (hints == null) {
            return EMPTY_OBJECT_ARRAY;
        }
        return (hints instanceof Object[] ? (Object[]) hints : new Object[]{hints});
    }
}
