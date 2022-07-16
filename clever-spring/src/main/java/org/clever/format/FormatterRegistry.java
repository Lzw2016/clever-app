package org.clever.format;

import org.clever.core.convert.converter.ConverterRegistry;

import java.lang.annotation.Annotation;

/**
 * 字段格式化注册表
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 16:40 <br/>
 */
public interface FormatterRegistry extends ConverterRegistry {
    /**
     * 注册Printer，尝试去参数化类型中提取Type
     *
     * @param printer Printer实例
     * @see #addFormatter(Formatter)
     */
    void addPrinter(Printer<?> printer);

    /**
     * 注册Parser，尝试去参数化类型中提取Type
     *
     * @param parser Parser实例
     * @see #addFormatter(Formatter)
     */
    void addParser(Parser<?> parser);

    /**
     * 注册Formatter，尝试去参数化类型中提取Type
     *
     * @param formatter Formatter实例
     * @see #addFormatterForFieldType(Class, Formatter)
     */
    void addFormatter(Formatter<?> formatter);

    /**
     * 注册Formatter，并指定支持的Type
     *
     * @param fieldType 指定的Type
     * @param formatter Formatter实例
     */
    void addFormatterForFieldType(Class<?> fieldType, Formatter<?> formatter);

    /**
     * 注册Printer和Parser，并指定支持的Type
     *
     * @param fieldType 指定的Type
     * @param printer   Printer实例
     * @param parser    Parser实例
     */
    void addFormatterForFieldType(Class<?> fieldType, Printer<?> printer, Parser<?> parser);

    /**
     * 注册AnnotationFormatterFactory
     *
     * @param annotationFormatterFactory 要添加的注解格式化工厂
     */
    void addFormatterForFieldAnnotation(AnnotationFormatterFactory<? extends Annotation> annotationFormatterFactory);
}