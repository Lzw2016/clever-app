package org.clever.core.io.support;

import org.clever.core.io.Resource;
import org.clever.util.Assert;
import org.clever.util.ClassUtils;
import org.clever.util.PropertiesPersister;
import org.clever.util.ResourceUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Properties;

/**
 * 加载{@code java.util.Properties}、执行输入流标准处理的方便实用方法。
 *
 * <p>对于更多可配置的属性加载，包括自定义编码选项，请考虑使用PropertiesLoaderSupport类。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/03 18:07 <br/>
 */
public abstract class PropertiesLoaderUtils {
    private static final String XML_FILE_EXTENSION = ".xml";

    /**
     * 由{@code clever.xml.ignore}系统属性控制的布尔标志，指示框架忽略XML，即不初始化与XML相关的基础结构。
     * <p>默认值为 "false".
     */
    private static final boolean shouldIgnoreXml = false; // SpringProperties.getFlag("clever.xml.ignore");

    /**
     * 从给定的EncodedResource加载属性，可能会为属性文件定义特定的编码。
     *
     * @see #fillProperties(java.util.Properties, EncodedResource)
     */
    public static Properties loadProperties(EncodedResource resource) throws IOException {
        Properties props = new Properties();
        fillProperties(props, resource);
        return props;
    }

    /**
     * 从给定的EncodedResource填充给定的属性，可能会为属性文件定义特定的编码。
     *
     * @param props    要加载到的属性实例
     * @param resource 要从中加载的资源
     * @throws IOException 在IO错误的情况下
     */
    public static void fillProperties(Properties props, EncodedResource resource) throws IOException {
        fillProperties(props, resource, ResourcePropertiesPersister.INSTANCE);
    }

    /**
     * 实际上，将属性从给定的EncodedResource加载到给定的properties实例中。
     *
     * @param props     要加载到的属性实例
     * @param resource  要从中加载的资源
     * @param persister 要使用的PropertiesPersister
     * @throws IOException 在IO错误的情况下
     */
    @SuppressWarnings("SameParameterValue")
    static void fillProperties(Properties props, EncodedResource resource, PropertiesPersister persister) throws IOException {
        InputStream stream = null;
        Reader reader = null;
        try {
            String filename = resource.getResource().getFilename();
            if (filename != null && filename.endsWith(XML_FILE_EXTENSION)) {
                if (shouldIgnoreXml) {
                    throw new UnsupportedOperationException("XML support disabled");
                }
                stream = resource.getInputStream();
                persister.loadFromXml(props, stream);
            } else if (resource.requiresReader()) {
                reader = resource.getReader();
                persister.load(props, reader);
            } else {
                stream = resource.getInputStream();
                persister.load(props, stream);
            }
        } finally {
            if (stream != null) {
                stream.close();
            }
            if (reader != null) {
                reader.close();
            }
        }
    }

    /**
     * 从给定资源加载属性（ISO-8859-1 编码）。
     *
     * @param resource 要从中加载的资源
     * @return 填充的属性实例
     * @throws IOException 如果加载失败
     * @see #fillProperties(java.util.Properties, Resource)
     */
    public static Properties loadProperties(Resource resource) throws IOException {
        Properties props = new Properties();
        fillProperties(props, resource);
        return props;
    }

    /**
     * 填写给定资源中的给定属性（ISO-8859-1 编码）。
     *
     * @param props    要填充的属性实例
     * @param resource 要从中加载的资源
     * @throws IOException 如果加载失败
     */
    public static void fillProperties(Properties props, Resource resource) throws IOException {
        try (InputStream is = resource.getInputStream()) {
            String filename = resource.getFilename();
            if (filename != null && filename.endsWith(XML_FILE_EXTENSION)) {
                if (shouldIgnoreXml) {
                    throw new UnsupportedOperationException("XML support disabled");
                }
                props.loadFromXML(is);
            } else {
                props.load(is);
            }
        }
    }

    /**
     * 使用默认类加载器从指定的类路径资源（ISO-8859-1 编码）加载所有属性。
     * <p>如果在类路径中找到多个同名资源，则合并属性。
     *
     * @param resourceName 类路径资源的名称
     * @return 填充的属性实例
     * @throws IOException 如果加载失败
     */
    public static Properties loadAllProperties(String resourceName) throws IOException {
        return loadAllProperties(resourceName, null);
    }

    /**
     * 使用给定的类加载器从指定的类路径资源（ISO-8859-1 编码）加载所有属性。
     * <p>如果在类路径中找到多个同名资源，则合并属性。
     *
     * @param resourceName 类路径资源的名称
     * @param classLoader  用于加载的类加载器（或null以使用默认类加载器）
     * @return 填充的属性实例
     * @throws IOException 如果加载失败
     */
    public static Properties loadAllProperties(String resourceName, ClassLoader classLoader) throws IOException {
        Assert.notNull(resourceName, "Resource name must not be null");
        ClassLoader classLoaderToUse = classLoader;
        if (classLoaderToUse == null) {
            classLoaderToUse = ClassUtils.getDefaultClassLoader();
        }
        Enumeration<URL> urls = (classLoaderToUse != null ?
                classLoaderToUse.getResources(resourceName)
                : ClassLoader.getSystemResources(resourceName)
        );
        Properties props = new Properties();
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            URLConnection con = url.openConnection();
            ResourceUtils.useCachesIfNecessary(con);
            try (InputStream is = con.getInputStream()) {
                if (resourceName.endsWith(XML_FILE_EXTENSION)) {
                    if (shouldIgnoreXml) {
                        throw new UnsupportedOperationException("XML support disabled");
                    }
                    props.loadFromXML(is);
                } else {
                    props.load(is);
                }
            }
        }
        return props;
    }
}
