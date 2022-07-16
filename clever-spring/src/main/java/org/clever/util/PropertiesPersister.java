package org.clever.util;

import java.io.*;
import java.util.Properties;

/**
 * 用于持久化{@code java.util.Properties}的策略接口，允许插入解析策略。
 *
 * <p>默认实现是DefaultPropertiesPersister，提供{@code java.util.Properties}的本机解析，
 * 但允许从任何读取器读取和写入任何写入器（允许为属性文件指定编码）。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/03 18:09 <br/>
 *
 * @see DefaultPropertiesPersister
 * @see org.clever.core.io.support.ResourcePropertiesPersister
 * @see java.util.Properties
 */
public interface PropertiesPersister {
    /**
     * 将属性从给定的InputStream加载到给定的properties对象中。
     *
     * @param props 要加载到的Properties对象
     * @param is    要从中加载的InputStream
     * @throws IOException 在IO错误的情况下
     * @see java.util.Properties#load
     */
    void load(Properties props, InputStream is) throws IOException;

    /**
     * 将属性从给定读取器加载到给定属性对象中。
     *
     * @param props  要加载到的Properties对象
     * @param reader 要从中加载的读取器
     * @throws IOException 在IO错误的情况下
     */
    void load(Properties props, Reader reader) throws IOException;

    /**
     * 将给定Properties对象的内容写入给定的OutputStream。
     *
     * @param props  要存储的属性对象
     * @param os     要写入的输出流
     * @param header 属性列表的描述
     * @throws IOException 在IO错误的情况下
     * @see java.util.Properties#store
     */
    void store(Properties props, OutputStream os, String header) throws IOException;

    /**
     * 将给定属性对象的内容写入给定的编写器。
     *
     * @param props  要存储的属性对象
     * @param writer 要写信给的作者
     * @param header 属性列表的描述
     * @throws IOException 在IO错误的情况下
     */
    void store(Properties props, Writer writer, String header) throws IOException;

    /**
     * 将属性从给定的XML InputStream加载到给定的properties对象中。
     *
     * @param props 要加载到的Properties对象
     * @param is    要从中加载的InputStream
     * @throws IOException 在IO错误的情况下
     * @see java.util.Properties#loadFromXML(java.io.InputStream)
     */
    void loadFromXml(Properties props, InputStream is) throws IOException;

    /**
     * 将给定Properties对象的内容写入给定的XML OutputStream。
     *
     * @param props  要存储的属性对象
     * @param os     要写入的输出流
     * @param header 属性列表的描述
     * @throws IOException 在IO错误的情况下
     * @see java.util.Properties#storeToXML(java.io.OutputStream, String)
     */
    void storeToXml(Properties props, OutputStream os, String header) throws IOException;

    /**
     * 将给定Properties对象的内容写入给定的XML OutputStream。
     *
     * @param props    要存储的属性对象
     * @param os       要写入的输出流
     * @param encoding 要使用的编码
     * @param header   属性列表的描述
     * @throws IOException 在IO错误的情况下
     * @see java.util.Properties#storeToXML(java.io.OutputStream, String, String)
     */
    void storeToXml(Properties props, OutputStream os, String header, String encoding) throws IOException;
}
