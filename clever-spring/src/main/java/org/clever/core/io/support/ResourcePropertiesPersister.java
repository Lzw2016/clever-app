package org.clever.core.io.support;

import org.clever.util.DefaultPropertiesPersister;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/**
 * 纯{@link DefaultPropertiesPersister}的支持的子类，通过共享的“clever.xml.ignore”属性为禁用的XML支持添加了条件检查。
 *
 * <p>这是资源支持中使用的标准实现。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/03 18:07 <br/>
 */
public class ResourcePropertiesPersister extends DefaultPropertiesPersister {
    /**
     * 默认{@code ResourcePropertiesPersister}实例的一个方便常数，用于公共资源支持。
     */
    public static final ResourcePropertiesPersister INSTANCE = new ResourcePropertiesPersister();

    /**
     * 由{@code clever.xml.ignore}系统属性控制的布尔标志，指示框架忽略XML，即不初始化与XML相关的基础结构。
     * <p>默认值为 "false".
     */
    public static boolean SHOULD_IGNORE_XML = false;

    @Override
    public void loadFromXml(Properties props, InputStream is) throws IOException {
        if (SHOULD_IGNORE_XML) {
            throw new UnsupportedOperationException("XML support disabled");
        }
        super.loadFromXml(props, is);
    }

    @Override
    public void storeToXml(Properties props, OutputStream os, String header) throws IOException {
        if (SHOULD_IGNORE_XML) {
            throw new UnsupportedOperationException("XML support disabled");
        }
        super.storeToXml(props, os, header);
    }

    @Override
    public void storeToXml(Properties props, OutputStream os, String header, String encoding) throws IOException {
        if (SHOULD_IGNORE_XML) {
            throw new UnsupportedOperationException("XML support disabled");
        }
        super.storeToXml(props, os, header, encoding);
    }
}
