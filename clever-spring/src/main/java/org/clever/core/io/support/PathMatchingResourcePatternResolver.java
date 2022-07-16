package org.clever.core.io.support;

import org.clever.core.io.*;
import org.clever.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipException;

/**
 * 一种{@link ResourcePatternResolver}实现，
 * 能够将指定的资源位置路径解析为一个或多个匹配的资源。
 * 源路径可以是一个简单的路径，它具有到目标资源的一对一映射，
 * 或者可以包含特殊的“classpath*:”前缀和或内部Ant样式的正则表达式(使用AntPathMatcher实用程序进行匹配)
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 22:19 <br/>
 *
 * @see #CLASSPATH_ALL_URL_PREFIX
 * @see org.clever.util.AntPathMatcher
 * @see org.clever.core.io.ResourceLoader#getResource(String)
 * @see ClassLoader#getResources(String)
 */
public class PathMatchingResourcePatternResolver implements ResourcePatternResolver {
    private static final Logger logger = LoggerFactory.getLogger(PathMatchingResourcePatternResolver.class);

    private static Method equinoxResolveMethod;

    static {
        try {
            // Detect Equinox OSGi (e.g. on WebSphere 6.1)
            Class<?> fileLocatorClass = ClassUtils.forName(
                    "org.eclipse.core.runtime.FileLocator",
                    PathMatchingResourcePatternResolver.class.getClassLoader()
            );
            equinoxResolveMethod = fileLocatorClass.getMethod("resolve", URL.class);
            logger.trace("Found Equinox FileLocator for OSGi bundle URL resolution");
        } catch (Throwable ex) {
            equinoxResolveMethod = null;
        }
    }

    private final ResourceLoader resourceLoader;
    private PathMatcher pathMatcher = new AntPathMatcher();

    /**
     * 使用DefaultResourceLoader创建新的PathMatchingResourcePatternResolver。
     * <p>类加载器访问将通过线程上下文类加载器进行。
     *
     * @see org.clever.core.io.DefaultResourceLoader
     */
    public PathMatchingResourcePatternResolver() {
        this.resourceLoader = new DefaultResourceLoader();
    }

    /**
     * 创建新的PathMatchingResourcePatternResolver。
     * <p>类加载器访问将通过线程上下文类加载器进行。
     *
     * @param resourceLoader 用于加载根目录和实际资源的ResourceLoader
     */
    public PathMatchingResourcePatternResolver(ResourceLoader resourceLoader) {
        Assert.notNull(resourceLoader, "ResourceLoader must not be null");
        this.resourceLoader = resourceLoader;
    }

    /**
     * 使用DefaultResourceLoader创建新的PathMatchingResourcePatternResolver
     *
     * @param classLoader 加载类路径资源的类加载器，或在实际资源访问时使用线程上下文类加载器的类加载器为null
     * @see org.clever.core.io.DefaultResourceLoader
     */
    public PathMatchingResourcePatternResolver(ClassLoader classLoader) {
        this.resourceLoader = new DefaultResourceLoader(classLoader);
    }

    /**
     * 返回此模式解析器使用的ResourceLoader
     */
    public ResourceLoader getResourceLoader() {
        return this.resourceLoader;
    }

    @Override
    public ClassLoader getClassLoader() {
        return getResourceLoader().getClassLoader();
    }

    /**
     * 设置用于此资源模式解析程序的PathMatcher实现。默认值为AntPathMatcher
     *
     * @see org.clever.util.AntPathMatcher
     */
    public void setPathMatcher(PathMatcher pathMatcher) {
        Assert.notNull(pathMatcher, "PathMatcher must not be null");
        this.pathMatcher = pathMatcher;
    }

    /**
     * 返回此资源模式解析程序使用的路径匹配器
     */
    public PathMatcher getPathMatcher() {
        return this.pathMatcher;
    }

    @Override
    public Resource getResource(String location) {
        return getResourceLoader().getResource(location);
    }

    @Override
    public Resource[] getResources(String locationPattern) throws IOException {
        Assert.notNull(locationPattern, "Location pattern must not be null");
        if (locationPattern.startsWith(CLASSPATH_ALL_URL_PREFIX)) {
            // a class path resource (multiple resources for same name possible)
            if (getPathMatcher().isPattern(locationPattern.substring(CLASSPATH_ALL_URL_PREFIX.length()))) {
                // a class path resource pattern
                return findPathMatchingResources(locationPattern);
            } else {
                // all class path resources with the given name
                return findAllClassPathResources(locationPattern.substring(CLASSPATH_ALL_URL_PREFIX.length()));
            }
        } else {
            // Generally only look for a pattern after a prefix here,
            // and on Tomcat only after the "*/" separator for its "war:" protocol.
            int prefixEnd = (locationPattern.startsWith("war:") ?
                    locationPattern.indexOf("*/") + 1 :
                    locationPattern.indexOf(':') + 1);
            if (getPathMatcher().isPattern(locationPattern.substring(prefixEnd))) {
                // a file pattern
                return findPathMatchingResources(locationPattern);
            } else {
                // a single resource with the given name
                return new Resource[]{getResourceLoader().getResource(locationPattern)};
            }
        }
    }

    /**
     * Find all class location resources with the given location via the ClassLoader.
     * Delegates to {@link #doFindAllClassPathResources(String)}.
     *
     * @param location 类路径中的绝对路径
     * @return 结果作为资源数组
     * @throws IOException 如果IO错误
     * @see java.lang.ClassLoader#getResources
     * @see #convertClassLoaderURL
     */
    protected Resource[] findAllClassPathResources(String location) throws IOException {
        String path = location;
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        Set<Resource> result = doFindAllClassPathResources(path);
        if (logger.isTraceEnabled()) {
            logger.trace("Resolved classpath location [" + location + "] to resources " + result);
        }
        return result.toArray(new Resource[0]);
    }

    /**
     * 通过类加载器查找具有给定路径的所有类位置资源。
     *
     * @param path 类路径中的绝对路径(从不使用前导斜杠)
     * @return 一组可变的匹配资源实例
     */
    protected Set<Resource> doFindAllClassPathResources(String path) throws IOException {
        Set<Resource> result = new LinkedHashSet<>(16);
        ClassLoader cl = getClassLoader();
        Enumeration<URL> resourceUrls = (cl != null ? cl.getResources(path) : ClassLoader.getSystemResources(path));
        while (resourceUrls.hasMoreElements()) {
            URL url = resourceUrls.nextElement();
            result.add(convertClassLoaderURL(url));
        }
        if (!StringUtils.hasLength(path)) {
            // The above result is likely to be incomplete, i.e. only containing file system references.
            // We need to have pointers to each of the jar files on the classpath as well...
            addAllClassLoaderJarRoots(cl, result);
        }
        return result;
    }

    /**
     * 将从类加载器返回的给定URL转换为{@link Resource}.
     * <p>默认实现只是创建{@link UrlResource}实例
     *
     * @param url 从类加载器返回的URL
     * @return 对应的资源对象
     * @see java.lang.ClassLoader#getResources
     * @see org.clever.core.io.Resource
     */
    protected Resource convertClassLoaderURL(URL url) {
        return new UrlResource(url);
    }

    /**
     * 在所有URLClassLoader URL中搜索jar文件引用，并以指向jar文件内容根的指针的形式将它们添加到给定的资源集中
     *
     * @param classLoader 要搜索的类加载器(包括其祖先)
     * @param result      要向其中添加jar根的资源集
     */
    protected void addAllClassLoaderJarRoots(ClassLoader classLoader, Set<Resource> result) {
        if (classLoader instanceof URLClassLoader) {
            try {
                for (URL url : ((URLClassLoader) classLoader).getURLs()) {
                    try {
                        UrlResource jarResource = (ResourceUtils.URL_PROTOCOL_JAR.equals(url.getProtocol()) ?
                                new UrlResource(url) :
                                new UrlResource(ResourceUtils.JAR_URL_PREFIX + url + ResourceUtils.JAR_URL_SEPARATOR));
                        if (jarResource.exists()) {
                            result.add(jarResource);
                        }
                    } catch (MalformedURLException ex) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Cannot search for matching files underneath [" + url +
                                    "] because it cannot be converted to a valid 'jar:' URL: " +
                                    ex.getMessage()
                            );
                        }
                    }
                }
            } catch (Exception ex) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Cannot introspect jar files since ClassLoader [" +
                            classLoader + "] does not support 'getURLs()': " + ex
                    );
                }
            }
        }
        if (classLoader == ClassLoader.getSystemClassLoader()) {
            // "java.class.path" manifest evaluation...
            addClassPathManifestEntries(result);
        }
        if (classLoader != null) {
            try {
                // Hierarchy traversal...
                addAllClassLoaderJarRoots(classLoader.getParent(), result);
            } catch (Exception ex) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Cannot introspect jar files in parent ClassLoader since [" +
                            classLoader + "] does not support 'getParent()': " + ex
                    );
                }
            }
        }
    }

    /**
     * 从“java.class.path”确定jar文件引用属性并以指向jar文件内容根的指针的形式将它们添加到给定的资源集中
     *
     * @param result 要向其中添加jar根的资源集
     */
    protected void addClassPathManifestEntries(Set<Resource> result) {
        try {
            String javaClassPathProperty = System.getProperty("java.class.path");
            for (String path : StringUtils.delimitedListToStringArray(
                    javaClassPathProperty, System.getProperty("path.separator"))) {
                try {
                    String filePath = new File(path).getAbsolutePath();
                    int prefixIndex = filePath.indexOf(':');
                    if (prefixIndex == 1) {
                        // Possibly "c:" drive prefix on Windows, to be upper-cased for proper duplicate detection
                        filePath = StringUtils.capitalize(filePath);
                    }
                    // # can appear in directories/filenames, java.net.URL should not treat it as a fragment
                    filePath = StringUtils.replace(filePath, "#", "%23");
                    // Build URL that points to the root of the jar file
                    UrlResource jarResource = new UrlResource(ResourceUtils.JAR_URL_PREFIX +
                            ResourceUtils.FILE_URL_PREFIX + filePath + ResourceUtils.JAR_URL_SEPARATOR);
                    // Potentially overlapping with URLClassLoader.getURLs() result above!
                    if (!result.contains(jarResource) && !hasDuplicate(filePath, result) && jarResource.exists()) {
                        result.add(jarResource);
                    }
                } catch (MalformedURLException ex) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Cannot search for matching files underneath [" + path +
                                "] because it cannot be converted to a valid 'jar:' URL: " + ex.getMessage()
                        );
                    }
                }
            }
        } catch (Exception ex) {
            if (logger.isDebugEnabled()) {
                logger.debug("Failed to evaluate 'java.class.path' manifest entries: " + ex);
            }
        }
    }

    /**
     * 检查给定文件路径在现有结果中是否有重复但结构不同的条目，即是否有前导斜杠
     *
     * @param filePath 文件路径(带或不带前导斜杠)
     * @param result   当前结果
     * @return 如果存在重复项（即忽略给定的文件路径），则为true；如果为false，则继续向当前结果添加相应的资源
     */
    private boolean hasDuplicate(String filePath, Set<Resource> result) {
        if (result.isEmpty()) {
            return false;
        }
        String duplicatePath = (filePath.startsWith("/") ? filePath.substring(1) : "/" + filePath);
        try {
            return result.contains(new UrlResource(ResourceUtils.JAR_URL_PREFIX + ResourceUtils.FILE_URL_PREFIX +
                    duplicatePath + ResourceUtils.JAR_URL_SEPARATOR));
        } catch (MalformedURLException ex) {
            // Ignore: just for testing against duplicate.
            return false;
        }
    }

    /**
     * 通过Ant样式的路径匹配器查找与给定位置模式匹配的所有资源。支持jar文件、zip文件和文件系统中的资源
     *
     * @param locationPattern 要匹配的位置模式
     * @return 结果作为资源数组
     * @throws IOException 如果IO错误
     * @see #doFindPathMatchingJarResources
     * @see #doFindPathMatchingFileResources
     * @see org.clever.util.PathMatcher
     */
    protected Resource[] findPathMatchingResources(String locationPattern) throws IOException {
        String rootDirPath = determineRootDir(locationPattern);
        String subPattern = locationPattern.substring(rootDirPath.length());
        Resource[] rootDirResources = getResources(rootDirPath);
        Set<Resource> result = new LinkedHashSet<>(16);
        for (Resource rootDirResource : rootDirResources) {
            rootDirResource = resolveRootDirResource(rootDirResource);
            URL rootDirUrl = rootDirResource.getURL();
            if (equinoxResolveMethod != null && rootDirUrl.getProtocol().startsWith("bundle")) {
                URL resolvedUrl = (URL) ReflectionUtils.invokeMethod(equinoxResolveMethod, null, rootDirUrl);
                if (resolvedUrl != null) {
                    rootDirUrl = resolvedUrl;
                }
                rootDirResource = new UrlResource(rootDirUrl);
            }
            if (rootDirUrl.getProtocol().startsWith(ResourceUtils.URL_PROTOCOL_VFS)) {
                result.addAll(VfsResourceMatchingDelegate.findMatchingResources(rootDirUrl, subPattern, getPathMatcher()));
            } else if (ResourceUtils.isJarURL(rootDirUrl) || isJarResource(rootDirResource)) {
                result.addAll(doFindPathMatchingJarResources(rootDirResource, rootDirUrl, subPattern));
            } else {
                result.addAll(doFindPathMatchingFileResources(rootDirResource, subPattern));
            }
        }
        if (logger.isTraceEnabled()) {
            logger.trace("Resolved location pattern [" + locationPattern + "] to resources " + result);
        }
        return result.toArray(new Resource[0]);
    }

    /**
     * 确定给定位置的根目录。
     * 用于确定文件匹配的起点，将根目录位置解析为{@code java.io.File}并将其传递到{@code retrieveMatchingFiles}中，其余位置作为模式。
     * 例如，将为模式"/WEB-INF/*.xml"返回"/WEB-INF/"
     *
     * @param location 要检查的位置
     * @return 表示根目录的位置部分
     * @see #retrieveMatchingFiles
     */
    protected String determineRootDir(String location) {
        int prefixEnd = location.indexOf(':') + 1;
        int rootDirEnd = location.length();
        while (rootDirEnd > prefixEnd && getPathMatcher().isPattern(location.substring(prefixEnd, rootDirEnd))) {
            rootDirEnd = location.lastIndexOf('/', rootDirEnd - 2) + 1;
        }
        if (rootDirEnd == 0) {
            rootDirEnd = prefixEnd;
        }
        return location.substring(0, rootDirEnd);
    }

    /**
     * 解析用于路径匹配的指定资源。
     * 默认情况下，Equinox OSGi"bundleresource:" / "bundleentry:"URL将解析为标准jar文件URL，
     * 该URL将使用clever的标准jar文件遍历算法进行遍历。
     * 对于前面的任何自定义解析，请重写此方法并相应地替换资源句柄
     *
     * @param original 要解决的资源
     * @return 已解析的资源(可能与传入的资源相同)
     * @throws IOException 如果解决失败
     */
    protected Resource resolveRootDirResource(Resource original) throws IOException {
        return original;
    }

    /**
     * 返回给定的资源句柄是否指示{@code doFindPathMatchingJarResources}方法可以处理的jar资源。
     * 默认情况下，URL协议“jar”、“zip”、“vfszip”和“wsjar”将被视为jar资源。
     * 此模板方法允许检测更多类型的类似jar的资源，例如通过资源句柄类型的instanceof检查
     *
     * @param resource 要检查的资源句柄(通常是开始路径匹配的根目录)
     * @see #doFindPathMatchingJarResources
     * @see org.clever.util.ResourceUtils#isJarURL
     */
    protected boolean isJarResource(Resource resource) throws IOException {
        return false;
    }

    /**
     * 通过Ant样式的路径匹配器在jar文件中查找与给定位置模式匹配的所有资源
     *
     * @param rootDirResource 根目录作为资源
     * @param rootDirURL      预解析的根目录URL
     * @param subPattern      要匹配的子模式(根目录下)
     * @return 一组可变的匹配资源实例
     * @throws IOException 如果IO错误
     * @see java.net.JarURLConnection
     * @see org.clever.util.PathMatcher
     */
    protected Set<Resource> doFindPathMatchingJarResources(Resource rootDirResource,
                                                           URL rootDirURL,
                                                           String subPattern) throws IOException {
        URLConnection con = rootDirURL.openConnection();
        JarFile jarFile;
        String jarFileUrl;
        String rootEntryPath;
        boolean closeJarFile;
        if (con instanceof JarURLConnection) {
            // Should usually be the case for traditional JAR files.
            JarURLConnection jarCon = (JarURLConnection) con;
            ResourceUtils.useCachesIfNecessary(jarCon);
            jarFile = jarCon.getJarFile();
            jarFileUrl = jarCon.getJarFileURL().toExternalForm();
            JarEntry jarEntry = jarCon.getJarEntry();
            rootEntryPath = (jarEntry != null ? jarEntry.getName() : "");
            closeJarFile = !jarCon.getUseCaches();
        } else {
            // No JarURLConnection -> need to resort to URL file parsing.
            // We'll assume URLs of the format "jar:path!/entry", with the protocol
            // being arbitrary as long as following the entry format.
            // We'll also handle paths with and without leading "file:" prefix.
            String urlFile = rootDirURL.getFile();
            try {
                int separatorIndex = urlFile.indexOf(ResourceUtils.WAR_URL_SEPARATOR);
                if (separatorIndex == -1) {
                    separatorIndex = urlFile.indexOf(ResourceUtils.JAR_URL_SEPARATOR);
                }
                if (separatorIndex != -1) {
                    jarFileUrl = urlFile.substring(0, separatorIndex);
                    rootEntryPath = urlFile.substring(separatorIndex + 2);  // both separators are 2 chars
                    jarFile = getJarFile(jarFileUrl);
                } else {
                    jarFile = new JarFile(urlFile);
                    jarFileUrl = urlFile;
                    rootEntryPath = "";
                }
                closeJarFile = true;
            } catch (ZipException ex) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Skipping invalid jar classpath entry [" + urlFile + "]");
                }
                return Collections.emptySet();
            }
        }
        try {
            if (logger.isTraceEnabled()) {
                logger.trace("Looking for matching resources in jar file [" + jarFileUrl + "]");
            }
            if (StringUtils.hasLength(rootEntryPath) && !rootEntryPath.endsWith("/")) {
                // Root entry path must end with slash to allow for proper matching.
                // The Sun JRE does not return a slash here, but BEA JRockit does.
                rootEntryPath = rootEntryPath + "/";
            }
            Set<Resource> result = new LinkedHashSet<>(8);
            for (Enumeration<JarEntry> entries = jarFile.entries(); entries.hasMoreElements(); ) {
                JarEntry entry = entries.nextElement();
                String entryPath = entry.getName();
                if (entryPath.startsWith(rootEntryPath)) {
                    String relativePath = entryPath.substring(rootEntryPath.length());
                    if (getPathMatcher().match(subPattern, relativePath)) {
                        result.add(rootDirResource.createRelative(relativePath));
                    }
                }
            }
            return result;
        } finally {
            if (closeJarFile) {
                jarFile.close();
            }
        }
    }

    /**
     * 将给定的jar文件URL解析为JarFile对象
     */
    protected JarFile getJarFile(String jarFileUrl) throws IOException {
        if (jarFileUrl.startsWith(ResourceUtils.FILE_URL_PREFIX)) {
            try {
                return new JarFile(ResourceUtils.toURI(jarFileUrl).getSchemeSpecificPart());
            } catch (URISyntaxException ex) {
                // Fallback for URLs that are not valid URIs (should hardly ever happen).
                return new JarFile(jarFileUrl.substring(ResourceUtils.FILE_URL_PREFIX.length()));
            }
        } else {
            return new JarFile(jarFileUrl);
        }
    }

    /**
     * 通过Ant样式的路径匹配器查找文件系统中与给定位置模式匹配的所有资源
     *
     * @param rootDirResource 根目录作为资源
     * @param subPattern      要匹配的子模式(根目录下)
     * @return 一组可变的匹配资源实例
     * @throws IOException 如果IO错误
     * @see #retrieveMatchingFiles
     * @see org.clever.util.PathMatcher
     */
    protected Set<Resource> doFindPathMatchingFileResources(Resource rootDirResource, String subPattern) throws IOException {
        File rootDir;
        try {
            rootDir = rootDirResource.getFile().getAbsoluteFile();
        } catch (FileNotFoundException ex) {
            if (logger.isDebugEnabled()) {
                logger.debug("Cannot search for matching files underneath " +
                        rootDirResource + " in the file system: " + ex.getMessage()
                );
            }
            return Collections.emptySet();
        } catch (Exception ex) {
            if (logger.isInfoEnabled()) {
                logger.info("Failed to resolve " + rootDirResource + " in the file system: " + ex);
            }
            return Collections.emptySet();
        }
        return doFindMatchingFileSystemResources(rootDir, subPattern);
    }

    /**
     * 通过Ant样式的路径匹配器查找文件系统中与给定位置模式匹配的所有资源
     *
     * @param rootDir    文件系统中的根目录
     * @param subPattern 要匹配的子模式(根目录下)
     * @return 一组可变的匹配资源实例
     * @throws IOException 如果IO错误
     * @see #retrieveMatchingFiles
     * @see org.clever.util.PathMatcher
     */
    protected Set<Resource> doFindMatchingFileSystemResources(File rootDir, String subPattern) throws IOException {
        if (logger.isTraceEnabled()) {
            logger.trace("Looking for matching resources in directory tree [" + rootDir.getPath() + "]");
        }
        Set<File> matchingFiles = retrieveMatchingFiles(rootDir, subPattern);
        Set<Resource> result = new LinkedHashSet<>(matchingFiles.size());
        for (File file : matchingFiles) {
            result.add(new FileSystemResource(file));
        }
        return result;
    }

    /**
     * 检索与给定路径模式匹配的文件，检查给定目录及其子目录
     *
     * @param rootDir 起始目录
     * @param pattern 要匹配的模式，相对于根目录
     * @return 一组可变的匹配资源实例
     * @throws IOException 如果无法检索目录内容
     */
    protected Set<File> retrieveMatchingFiles(File rootDir, String pattern) throws IOException {
        if (!rootDir.exists()) {
            // Silently skip non-existing directories.
            if (logger.isDebugEnabled()) {
                logger.debug("Skipping [" + rootDir.getAbsolutePath() + "] because it does not exist");
            }
            return Collections.emptySet();
        }
        if (!rootDir.isDirectory()) {
            // Complain louder if it exists but is no directory.
            if (logger.isInfoEnabled()) {
                logger.info("Skipping [" + rootDir.getAbsolutePath() + "] because it does not denote a directory");
            }
            return Collections.emptySet();
        }
        if (!rootDir.canRead()) {
            if (logger.isInfoEnabled()) {
                logger.info("Skipping search for matching files underneath directory [" + rootDir.getAbsolutePath() +
                        "] because the application is not allowed to read the directory"
                );
            }
            return Collections.emptySet();
        }
        String fullPattern = StringUtils.replace(rootDir.getAbsolutePath(), File.separator, "/");
        if (!pattern.startsWith("/")) {
            fullPattern += "/";
        }
        fullPattern = fullPattern + StringUtils.replace(pattern, File.separator, "/");
        Set<File> result = new LinkedHashSet<>(8);
        doRetrieveMatchingFiles(fullPattern, rootDir, result);
        return result;
    }

    /**
     * 递归检索与给定模式匹配的文件，并将其添加到给定的结果列表中
     *
     * @param fullPattern 要匹配的模式，带有前置根目录路径
     * @param dir         当前目录
     * @param result      要添加到的匹配文件实例集
     * @throws IOException 如果无法检索目录内容
     */
    protected void doRetrieveMatchingFiles(String fullPattern, File dir, Set<File> result) throws IOException {
        if (logger.isTraceEnabled()) {
            logger.trace("Searching directory [" + dir.getAbsolutePath() +
                    "] for files matching pattern [" + fullPattern + "]"
            );
        }
        for (File content : listDirectory(dir)) {
            String currPath = StringUtils.replace(content.getAbsolutePath(), File.separator, "/");
            if (content.isDirectory() && getPathMatcher().matchStart(fullPattern, currPath + "/")) {
                if (!content.canRead()) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Skipping subdirectory [" + dir.getAbsolutePath() +
                                "] because the application is not allowed to read the directory"
                        );
                    }
                } else {
                    doRetrieveMatchingFiles(fullPattern, content, result);
                }
            }
            if (getPathMatcher().match(fullPattern, currPath)) {
                result.add(content);
            }
        }
    }

    /**
     * 确定给定目录中文件的排序列表
     *
     * @param dir 要内省的目录
     * @return 已排序的文件列表(默认情况下 ， 按字母顺序)
     * @see File#listFiles()
     */
    protected File[] listDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            if (logger.isInfoEnabled()) {
                logger.info("Could not retrieve contents of directory [" + dir.getAbsolutePath() + "]");
            }
            return new File[0];
        }
        Arrays.sort(files, Comparator.comparing(File::getName));
        return files;
    }

    /**
     * 内部委托类，避免运行时出现硬JBoss VFS API依赖
     */
    private static class VfsResourceMatchingDelegate {
        public static Set<Resource> findMatchingResources(URL rootDirURL,
                                                          String locationPattern,
                                                          PathMatcher pathMatcher) throws IOException {
            Object root = VfsPatternUtils.findRoot(rootDirURL);
            PatternVirtualFileVisitor visitor = new PatternVirtualFileVisitor(
                    VfsPatternUtils.getPath(root), locationPattern, pathMatcher
            );
            VfsPatternUtils.visit(root, visitor);
            return visitor.getResources();
        }
    }

    /**
     * 用于路径匹配目的的VFS访问者
     */
    private static class PatternVirtualFileVisitor implements InvocationHandler {
        private final String subPattern;
        private final PathMatcher pathMatcher;
        private final String rootPath;
        private final Set<Resource> resources = new LinkedHashSet<>();

        public PatternVirtualFileVisitor(String rootPath, String subPattern, PathMatcher pathMatcher) {
            this.subPattern = subPattern;
            this.pathMatcher = pathMatcher;
            this.rootPath = (rootPath.isEmpty() || rootPath.endsWith("/") ? rootPath : rootPath + "/");
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            if (Object.class == method.getDeclaringClass()) {
                if (methodName.equals("equals")) {
                    // Only consider equal when proxies are identical.
                    return (proxy == args[0]);
                } else if (methodName.equals("hashCode")) {
                    return System.identityHashCode(proxy);
                }
            } else if ("getAttributes".equals(methodName)) {
                return getAttributes();
            } else if ("visit".equals(methodName)) {
                visit(args[0]);
                return null;
            } else if ("toString".equals(methodName)) {
                return toString();
            }
            throw new IllegalStateException("Unexpected method invocation: " + method);
        }

        public void visit(Object vfsResource) {
            if (this.pathMatcher.match(this.subPattern, VfsPatternUtils.getPath(vfsResource).substring(this.rootPath.length()))) {
                this.resources.add(new VfsResource(vfsResource));
            }
        }

        public Object getAttributes() {
            return VfsPatternUtils.getVisitorAttributes();
        }

        public Set<Resource> getResources() {
            return this.resources;
        }

        public int size() {
            return this.resources.size();
        }

        @Override
        public String toString() {
            return "sub-pattern: " + this.subPattern + ", resources: " + this.resources;
        }
    }
}
