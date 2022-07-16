package org.clever.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.*;

/**
 * 用于将资源位置解析为文件系统中的文件的实用方法。
 * 主要用于框架内的内部使用。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 14:14 <br/>
 *
 * @see org.clever.core.io.Resource
 * @see org.clever.core.io.ClassPathResource
 * @see org.clever.core.io.FileSystemResource
 * @see org.clever.core.io.UrlResource
 * @see org.clever.core.io.ResourceLoader
 */
public abstract class ResourceUtils {
    /**
     * 用于从类路径加载的伪URL前缀："classpath:"
     */
    public static final String CLASSPATH_URL_PREFIX = "classpath:";
    /**
     * 用于从文件系统加载的URL前缀："file:"
     */
    public static final String FILE_URL_PREFIX = "file:";
    /**
     * 用于从jar文件加载的URL前缀："jar:"
     */
    public static final String JAR_URL_PREFIX = "jar:";
    /**
     * 用于从Tomcat上的war文件加载的URL前缀："war:"
     */
    public static final String WAR_URL_PREFIX = "war:";
    /**
     * 文件系统中文件的URL协议："file"
     */
    public static final String URL_PROTOCOL_FILE = "file";
    /**
     * jar文件条目的URL协议："jar"
     */
    public static final String URL_PROTOCOL_JAR = "jar";
    /**
     * war文件条目的URL协议："war"
     */
    public static final String URL_PROTOCOL_WAR = "war";
    /**
     * zip文件中条目的URL协议："zip"
     */
    public static final String URL_PROTOCOL_ZIP = "zip";
    /**
     * WebSphere jar文件项的URL协议："wsjar"
     */
    public static final String URL_PROTOCOL_WSJAR = "wsjar";
    /**
     * JBoss jar文件项的URL协议："vfszip"
     */
    public static final String URL_PROTOCOL_VFSZIP = "vfszip";
    /**
     * JBoss文件系统资源的URL协议："vfsfile"
     */
    public static final String URL_PROTOCOL_VFSFILE = "vfsfile";
    /**
     * 通用JBoss VFS资源的URL协议："vfs"
     */
    public static final String URL_PROTOCOL_VFS = "vfs";
    /**
     * 常规jar文件的文件扩展名：".jar"
     */
    public static final String JAR_FILE_EXTENSION = ".jar";
    /**
     * JAR URL和JAR内文件路径之间的分隔符："!/"
     */
    public static final String JAR_URL_SEPARATOR = "!/";
    /**
     * Tomcat上WAR URL和jar部分之间的特殊分隔符
     */
    public static final String WAR_URL_SEPARATOR = "*/";

    /**
     * 返回给定的资源位置是否为URL：特殊的"classpath"伪URL或标准URL
     *
     * @see #CLASSPATH_URL_PREFIX
     * @see java.net.URL
     */
    public static boolean isUrl(String resourceLocation) {
        if (resourceLocation == null) {
            return false;
        }
        if (resourceLocation.startsWith(CLASSPATH_URL_PREFIX)) {
            return true;
        }
        try {
            new URL(resourceLocation);
            return true;
        } catch (MalformedURLException ex) {
            return false;
        }
    }

    /**
     * 将给定的资源位置解析为{@code java.net.URL}。不检查URL是否实际存在；只返回给定位置对应的URL
     *
     * @throws FileNotFoundException 如果资源无法解析为URL
     */
    public static URL getURL(String resourceLocation) throws FileNotFoundException {
        Assert.notNull(resourceLocation, "Resource location must not be null");
        if (resourceLocation.startsWith(CLASSPATH_URL_PREFIX)) {
            String path = resourceLocation.substring(CLASSPATH_URL_PREFIX.length());
            ClassLoader cl = ClassUtils.getDefaultClassLoader();
            URL url = (cl != null ? cl.getResource(path) : ClassLoader.getSystemResource(path));
            if (url == null) {
                String description = "class path resource [" + path + "]";
                throw new FileNotFoundException(description + " cannot be resolved to URL because it does not exist");
            }
            return url;
        }
        try {
            // try URL
            return new URL(resourceLocation);
        } catch (MalformedURLException ex) {
            // no URL -> treat as file path
            try {
                return new File(resourceLocation).toURI().toURL();
            } catch (MalformedURLException ex2) {
                throw new FileNotFoundException("Resource location [" + resourceLocation + "] is neither a URL not a well-formed file path");
            }
        }
    }

    /**
     * 将给定的资源位置解析为{@code java.io.File}，即文件系统中的文件。
     * 不检查文件是否实际存在；只返回给定位置对应的File对象
     *
     * @throws FileNotFoundException 如果资源无法解析为文件系统中的文件
     */
    public static File getFile(String resourceLocation) throws FileNotFoundException {
        Assert.notNull(resourceLocation, "Resource location must not be null");
        if (resourceLocation.startsWith(CLASSPATH_URL_PREFIX)) {
            String path = resourceLocation.substring(CLASSPATH_URL_PREFIX.length());
            String description = "class path resource [" + path + "]";
            ClassLoader cl = ClassUtils.getDefaultClassLoader();
            URL url = (cl != null ? cl.getResource(path) : ClassLoader.getSystemResource(path));
            if (url == null) {
                throw new FileNotFoundException(description + " cannot be resolved to absolute file path because it does not exist");
            }
            return getFile(url, description);
        }
        try {
            // try URL
            return getFile(new URL(resourceLocation));
        } catch (MalformedURLException ex) {
            // no URL -> treat as file path
            return new File(resourceLocation);
        }
    }

    /**
     * 将给定的资源位置解析为{@code java.io.File}，即文件系统中的文件。
     *
     * @throws FileNotFoundException 如果URL无法解析为文件系统中的文件
     */
    public static File getFile(URL resourceUrl) throws FileNotFoundException {
        return getFile(resourceUrl, "URL");
    }

    /**
     * 将给定的资源URL解析为 {@code java.io.File},
     *
     * @param resourceUrl 要解析的资源URL
     * @param description 为其创建URL的原始资源的描述（例如，类路径位置）
     * @return 对应的文件对象
     * @throws FileNotFoundException 如果URL无法解析为文件系统中的文件
     */
    public static File getFile(URL resourceUrl, String description) throws FileNotFoundException {
        Assert.notNull(resourceUrl, "Resource URL must not be null");
        if (!URL_PROTOCOL_FILE.equals(resourceUrl.getProtocol())) {
            throw new FileNotFoundException(
                    description + " cannot be resolved to absolute file path " + "because it does not reside in the file system: " + resourceUrl
            );
        }
        try {
            return new File(toURI(resourceUrl).getSchemeSpecificPart());
        } catch (URISyntaxException ex) {
            // Fallback for URLs that are not valid URIs (should hardly ever happen).
            return new File(resourceUrl.getFile());
        }
    }

    /**
     * 将给定的资源URI解析为{@code java.io.File},
     *
     * @param resourceUri 要解析的资源URI
     * @return 对应的文件对象
     * @throws FileNotFoundException 如果URL无法解析为文件系统中的文件
     */
    public static File getFile(URI resourceUri) throws FileNotFoundException {
        return getFile(resourceUri, "URI");
    }

    /**
     * 将给定的资源URI解析为 {@code java.io.File}
     *
     * @param resourceUri 要解析的资源URI
     * @param description 为其创建URI的原始资源的描述（例如，类路径位置）
     * @return 对应的File对象
     * @throws FileNotFoundException 如果URL无法解析为文件系统中的文件
     */
    public static File getFile(URI resourceUri, String description) throws FileNotFoundException {
        Assert.notNull(resourceUri, "Resource URI must not be null");
        if (!URL_PROTOCOL_FILE.equals(resourceUri.getScheme())) {
            throw new FileNotFoundException(
                    description + " cannot be resolved to absolute file path " + "because it does not reside in the file system: " + resourceUri
            );
        }
        return new File(resourceUri.getSchemeSpecificPart());
    }

    /**
     * 确定给定的URL是否指向文件系统中的资源，
     * 具有协议 "file", "vfsfile" 或 "vfs".
     *
     * @param url 要检查的URL
     * @return URL是否已标识为文件系统URL
     */
    public static boolean isFileURL(URL url) {
        String protocol = url.getProtocol();
        return URL_PROTOCOL_FILE.equals(protocol)
                || URL_PROTOCOL_VFSFILE.equals(protocol)
                || URL_PROTOCOL_VFS.equals(protocol);
    }

    /**
     * 确定给定的URL是否指向jar文件中的资源。
     * 具有协议"jar", "war, ""zip", "vfszip" 或 "wsjar"
     *
     * @param url 要检查的URL
     * @return URL是否已标识为JAR URL
     */
    public static boolean isJarURL(URL url) {
        String protocol = url.getProtocol();
        return URL_PROTOCOL_JAR.equals(protocol)
                || URL_PROTOCOL_WAR.equals(protocol)
                || URL_PROTOCOL_ZIP.equals(protocol)
                || URL_PROTOCOL_VFSZIP.equals(protocol)
                || URL_PROTOCOL_WSJAR.equals(protocol);
    }

    /**
     * 确定给定的URL是否指向jar文件本身，即是否具有协议“file”，并以“.jar”扩展名结尾
     *
     * @param url 要检查的URL
     * @return URL是否已标识为JAR文件URL
     */
    public static boolean isJarFileURL(URL url) {
        return (URL_PROTOCOL_FILE.equals(url.getProtocol()) && url.getPath().toLowerCase().endsWith(JAR_FILE_EXTENSION));
    }

    /**
     * 从给定的URL（可能指向jar文件中的资源或jar文件本身）中提取实际jar文件的URL
     *
     * @param jarUrl the original URL
     * @return 实际jar文件的URL
     * @throws MalformedURLException 如果无法提取有效的jar文件URL
     */
    public static URL extractJarFileURL(URL jarUrl) throws MalformedURLException {
        String urlFile = jarUrl.getFile();
        int separatorIndex = urlFile.indexOf(JAR_URL_SEPARATOR);
        if (separatorIndex != -1) {
            String jarFile = urlFile.substring(0, separatorIndex);
            try {
                return new URL(jarFile);
            } catch (MalformedURLException ex) {
                // Probably no protocol in original jar URL, like "jar:C:/mypath/myjar.jar".
                // This usually indicates that the jar file resides in the file system.
                if (!jarFile.startsWith("/")) {
                    jarFile = "/" + jarFile;
                }
                return new URL(FILE_URL_PREFIX + jarFile);
            }
        } else {
            return jarUrl;
        }
    }

    /**
     * 从给定的jar/war URL（可能指向jar文件中的资源或jar文件本身）中提取最外层存档的URL。
     * 对于嵌套在war文件中的jar文件，这将返回war文件的URL，因为这是文件系统中可以解析的URL
     *
     * @param jarUrl 原始URL
     * @return 实际jar文件的URL
     * @throws MalformedURLException 如果无法提取有效的jar文件URL
     * @see #extractJarFileURL(URL)
     */
    public static URL extractArchiveURL(URL jarUrl) throws MalformedURLException {
        String urlFile = jarUrl.getFile();
        int endIndex = urlFile.indexOf(WAR_URL_SEPARATOR);
        if (endIndex != -1) {
            // Tomcat's "war:file:...mywar.war*/WEB-INF/lib/myjar.jar!/myentry.txt"
            String warFile = urlFile.substring(0, endIndex);
            if (URL_PROTOCOL_WAR.equals(jarUrl.getProtocol())) {
                return new URL(warFile);
            }
            int startIndex = warFile.indexOf(WAR_URL_PREFIX);
            if (startIndex != -1) {
                return new URL(warFile.substring(startIndex + WAR_URL_PREFIX.length()));
            }
        }
        // Regular "jar:file:...myjar.jar!/myentry.txt"
        return extractJarFileURL(jarUrl);
    }

    /**
     * 为给定URL创建URI实例，首先用“%20”URI编码替换空格
     *
     * @param url 要转换为URI实例的url
     * @throws URISyntaxException 如果URL不是有效的URI
     * @see java.net.URL#toURI()
     */
    public static URI toURI(URL url) throws URISyntaxException {
        return toURI(url.toString());
    }

    /**
     * 为给定的位置字符串创建URI实例，首先用“%20”URI编码替换空格
     *
     * @param location 要转换为URI实例的位置字符串
     * @throws URISyntaxException 如果位置不是有效的URI
     */
    public static URI toURI(String location) throws URISyntaxException {
        return new URI(StringUtils.replace(location, " ", "%20"));
    }

    /**
     * 在给定连接上设置{@link URLConnection#setUseCaches "useCaches"}标志，
     * 对于基于JNLP的资源，首选false，但将该标志保留为true
     *
     * @param con 要在其上设置标志的URLConnection
     */
    public static void useCachesIfNecessary(URLConnection con) {
        con.setUseCaches(con.getClass().getSimpleName().startsWith("JNLP"));
    }
}
