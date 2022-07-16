package org.clever.format;

import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * 支持格式化特定注解字段值的工厂，用来支持使用注解配置格式化行为<br/>
 * 例如{@code DateTimeFormatAnnotationFormatterFactory}，对用{@code @DateTimeFormat}注解标注的{@code Date}类型的字段进行格式化
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 16:42 <br/>
 */
public interface AnnotationFormatterFactory<A extends Annotation> {
    /**
     * <pre>{@code 可以使用 <A> 注解进行标注的字段类型}</pre>
     */
    Set<Class<?>> getFieldTypes();

    /**
     * 根据 A 注释实例和字段类型获取Printer实例
     *
     * @param annotation 注释实例
     * @param fieldType  已注释字段的类型
     * @return Printer实例
     */
    Printer<?> getPrinter(A annotation, Class<?> fieldType);

    /**
     * 根据 A 注释实例和字段类型获取Parser实例
     *
     * @param annotation 注释实例
     * @param fieldType  已注释字段的类型
     * @return Parser实例
     */
    Parser<?> getParser(A annotation, Class<?> fieldType);
}
