package org.clever.util;

import java.io.*;
import java.util.Properties;

/**
 * PropertiesPersister接口的默认实现。遵循{@code java.util.Properties}的本机解析。
 *
 * <p>允许从任何读取器读取和写入任何写入器，例如为属性文件指定字符集。
 * 不幸的是，直到JDK 5之前，标准{@code java.util.Properties}都缺少这一功能：您只能使用ISO-8859-1字符集加载文件。
 *
 * <p>从流加载并存储到流代表{@code Properties.load}和属性。
 * 存储分别与JDK Properties类实现的Unicode转换完全兼容。
 * 从JDK 6开始，{@code Properties.load/store}也用于Readers/Writer，有效地将此类转变为一个简单的向后兼容适配器。
 *
 * <p>与ReaderWriter一起使用的持久性代码遵循JDK的解析策略，但不实现Unicode转换，因为Reader/Writer应该已经应用了适当的字符解码/编码。
 * 如果您希望在属性文件中转义unicode字符，请不要为ReaderWriter指定编码（如可重新加载的ResourceBundleMessageSource的“defaultEncoding”和“fileEncodings”属性）。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/03 18:08 <br/>
 *
 * @see java.util.Properties
 * @see java.util.Properties#load
 * @see java.util.Properties#store
 * @see org.clever.core.io.support.ResourcePropertiesPersister
 */
public class DefaultPropertiesPersister implements PropertiesPersister {
    @Override
    public void load(Properties props, InputStream is) throws IOException {
        props.load(is);
    }

    @Override
    public void load(Properties props, Reader reader) throws IOException {
        props.load(reader);
    }

    @Override
    public void store(Properties props, OutputStream os, String header) throws IOException {
        props.store(os, header);
    }

    @Override
    public void store(Properties props, Writer writer, String header) throws IOException {
        props.store(writer, header);
    }

    @Override
    public void loadFromXml(Properties props, InputStream is) throws IOException {
        props.loadFromXML(is);
    }

    @Override
    public void storeToXml(Properties props, OutputStream os, String header) throws IOException {
        props.storeToXML(os, header);
    }

    @Override
    public void storeToXml(Properties props, OutputStream os, String header, String encoding) throws IOException {
        props.storeToXML(os, header, encoding);
    }
}
