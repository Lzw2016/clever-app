package org.clever.js.api.folder;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.exception.ExceptionUtils;
import org.clever.core.io.Resource;
import org.clever.core.io.support.PathMatchingResourcePatternResolver;
import org.clever.util.ResourceUtils;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/08/29 19:04 <br/>
 */
@Slf4j
public class ClassPathFolder implements Folder {
    public static final String JAR_URL_SEPARATOR = "!";
    /**
     * 加载Resource实现对象
     */
    private static final PathMatchingResourcePatternResolver Path_Matching_Resolver = new PathMatchingResourcePatternResolver();
    /**
     * 文件资源集合{@code Map<locationPattern, Set<资源路径>>}
     */
    private static final ConcurrentHashMap<String, CopyOnWriteArraySet<String>> Multiple_Resource_Map = new ConcurrentHashMap<>(8);

    /**
     * 当前应用的Class Path路径
     */
    public static final String Current_ClassPath;
    /**
     * 当前应用的classpath是否是jar包里的文件路径
     */
    public static final boolean Current_ClassPath_Is_Jar_File;

    static {
        String classPath;
        try {
            // jar:file:/D:/SourceCode/clever/hinny-spring-example/hinny-spring-example-1.0.0-SNAPSHOT.jar!/BOOT-INF/classes!/
            // file:/D:/SourceCode/clever/clever-hinny/clever-hinny-api/target/test-classes/
            classPath = ResourceUtils.getURL("classpath:").toExternalForm();
        } catch (FileNotFoundException e) {
            throw ExceptionUtils.unchecked(e);
        }
        if (classPath.endsWith(Folder.Path_Separate)) {
            classPath = classPath.substring(0, classPath.length() - 1);
        }
        Current_ClassPath = classPath;
        Current_ClassPath_Is_Jar_File = Current_ClassPath.startsWith(ResourceUtils.JAR_URL_PREFIX);
    }

    /**
     * classpath路径模式
     */
    protected final String locationPattern;
    /**
     * classpath 基础路径(以根路径"/"开头)
     */
    protected final String basePath;
    /**
     * 当前路径的逻辑绝对路径(使用了统一的分隔符，以根路径"/"开头)<br />
     * <b>结束字符不是路径分隔符，除了根路径</b>
     */
    protected final String fullPath;
    /**
     * 当前路径的物理绝对路径(结束字符不是路径分隔符)
     */
    protected final String absolutePath;

    /**
     * @param locationPattern classpath路径模式
     * @param basePath        基础路径
     */
    private ClassPathFolder(String locationPattern, String basePath) {
        basePath = concatPath(Folder.Root_Path, basePath);
        this.locationPattern = locationPattern;
        this.basePath = StringUtils.isBlank(basePath) ? Folder.Root_Path : basePath;
        this.fullPath = Folder.Root_Path;
        init();
        this.absolutePath = Current_ClassPath;
    }

    /**
     * @param locationPattern classpath路径模式
     * @param basePath        基础路径
     * @param path            当前路径(相当路径或者绝对路径)
     */
    private ClassPathFolder(String locationPattern, String basePath, String path) {
        basePath = concatPath(Folder.Root_Path, basePath);
        path = concatPath(Folder.Root_Path, path);
        this.locationPattern = locationPattern;
        this.basePath = StringUtils.isBlank(basePath) ? Folder.Root_Path : basePath;
        this.fullPath = StringUtils.isBlank(path) ? Folder.Root_Path : path;
        init();
        this.absolutePath = getAbsolutePath(locationPattern, this.basePath, this.fullPath);
    }

    @Override
    public Folder getRoot() {
        return new ClassPathFolder(locationPattern, basePath);
    }

    @Override
    public Folder getParent() {
        return this.concat(Folder.Parent_Path);
    }

    @Override
    public String getName() {
        return FilenameUtils.getName(fullPath);
    }

    @Override
    public String getFullPath() {
        return fullPath;
    }

    @Override
    public String getAbsolutePath() {
        return absolutePath;
    }

    @Override
    public boolean exists() {
        return StringUtils.isNotBlank(absolutePath);
    }

    @Override
    public boolean isFile() {
        if (StringUtils.isBlank(absolutePath)) {
            return false;
        }
        Resource resource = Path_Matching_Resolver.getResource(absolutePath);
        if (resource.isFile()) {
            try {
                if (!resource.getFile().isFile()) {
                    return false;
                }
            } catch (Exception e) {
                log.warn("Resource.getFile异常", e);
            }
        }
        return resourceExists(resource);
    }

    @Override
    public boolean isDir() {
        if (StringUtils.isBlank(absolutePath)) {
            return false;
        }
        Resource resource = Path_Matching_Resolver.getResource(absolutePath);
        if (resource.isFile()) {
            try {
                if (!resource.getFile().isDirectory()) {
                    return false;
                }
            } catch (Exception e) {
                log.warn("Resource.getFile异常", e);
            }
        }
        return !resourceExists(resource);
    }

    @SneakyThrows
    @Override
    public String getFileContent(String name) {
        String filePath = concatPath(this.absolutePath, name);
        if (StringUtils.isNotBlank(filePath)) {
            return null;
        }
        Resource resource = Path_Matching_Resolver.getResource(filePath);
        if (resourceExists(resource)) {
            try (InputStream inputStream = resource.getInputStream()) {
                return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    @SneakyThrows
    @Override
    public String getFileContent() {
        if (StringUtils.isNotBlank(absolutePath)) {
            Resource resource = Path_Matching_Resolver.getResource(absolutePath);
            if (resourceExists(resource)) {
                try (InputStream inputStream = resource.getInputStream()) {
                    return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
                }
            }
        }
        return null;
    }

    @Override
    public List<Folder> getChildren() {
        List<String> children = getChildren(this);
        if (children == null) {
            return null;
        }
        List<Folder> folders = new ArrayList<>(children.size());
        for (String child : children) {
            Folder folder = this.create(child);
            folders.add(folder);
        }
        return folders;
    }

    @Override
    public Folder concat(String... paths) {
        String fullPath = concatPath(this.fullPath, paths);
        if (fullPath == null) {
            return null;
        }
        return this.create(fullPath);
    }

    @Override
    public Folder create(String path) {
        return new ClassPathFolder(locationPattern, basePath, path);
    }

    /**
     * @param locationPattern classpath路径模式
     * @param basePath        基础路径
     */
    public static ClassPathFolder createRootPath(String locationPattern, String basePath) {
        return new ClassPathFolder(locationPattern, basePath);
    }

    // --------------------------------------------------------------------------------------------------------------------------------------------------------

    @SneakyThrows
    protected void init() {
        synchronized (Multiple_Resource_Map) {
            if (Multiple_Resource_Map.containsKey(locationPattern)) {
                return;
            }
            // 加载资源
            Resource[] resources = Path_Matching_Resolver.getResources(locationPattern);
            CopyOnWriteArraySet<String> resourceSet = new CopyOnWriteArraySet<>();
            for (Resource resource : resources) {
                if (resource.exists()) {
                    resourceSet.add(resource.getURL().toExternalForm());
                }
            }
            Multiple_Resource_Map.put(locationPattern, resourceSet);
            log.info("Resource加载完成! locationPattern={} | size={}", locationPattern, resourceSet.size());
        }
    }

    protected static List<String> getChildren(ClassPathFolder parent) {
        if (parent == null) {
            return null;
        }
        Set<String> children = new HashSet<>();
        if (StringUtils.isNotBlank(parent.absolutePath)) {
            Set<String> resourceSet = Multiple_Resource_Map.get(parent.locationPattern);
            if (resourceSet == null || resourceSet.isEmpty()) {
                return Collections.emptyList();
            }
            for (String resource : resourceSet) {
                if (resource.startsWith(parent.absolutePath)) {
                    String childPath = resource.substring(parent.absolutePath.length());
                    String[] childArr = StringUtils.split(childPath, Folder.Path_Separate);
                    if (childArr.length > 0) {
                        children.add(childArr[0]);
                    }
                }
            }
        }
        return new ArrayList<>(children);
    }

    protected static String getAbsolutePath(String locationPattern, String basePath, String path) {
        Set<String> resourceSet = Multiple_Resource_Map.get(locationPattern);
        if (resourceSet == null || resourceSet.isEmpty()) {
            return null;
        }
        List<String> pathMatchResourceList = new ArrayList<>(1);
        if (!Current_ClassPath_Is_Jar_File) {
            // 不是jar包
            String classPathFullPath = concatPath(Current_ClassPath, Folder.Current + basePath, Folder.Current + path);
            for (String resource : resourceSet) {
                if (resource.startsWith(classPathFullPath)) {
                    pathMatchResourceList.add(classPathFullPath);
                    break;
                }
            }
        }
        // jar包
        String fullPath = concatPath(basePath, Folder.Current + path);
        if (fullPath == null) {
            return null;
        }
        String jarFullPath = JAR_URL_SEPARATOR + fullPath;
        for (String resource : resourceSet) {
            if (resource.contains(jarFullPath)) {
                final int endIndex = resource.lastIndexOf(jarFullPath) + jarFullPath.length();
                String pathMatchResource = resource.substring(0, endIndex);
                if (pathMatchResource.length() > 1 && pathMatchResource.endsWith(Folder.Path_Separate)) {
                    pathMatchResource = pathMatchResource.substring(0, basePath.length() - 1);
                }
                if (!pathMatchResourceList.contains(pathMatchResource)) {
                    pathMatchResourceList.add(pathMatchResource);
                }
            }
        }
        if (pathMatchResourceList.isEmpty()) {
            return null;
        }
        if (pathMatchResourceList.size() > 1) {
            StringBuilder sb = new StringBuilder();
            for (String resource : pathMatchResourceList) {
                sb.append("\t").append(resource).append("\n");
            }
            log.warn("匹配到{}个资源文件 | locationPattern={} | basePath={} | path={} \n{}", pathMatchResourceList.size(), locationPattern, basePath, path, sb.toString());
        }
        return pathMatchResourceList.get(0);
    }

    /**
     * 连接路径，超出路径范围返回null(结束字符不是路径分隔符，除了根路径)
     */
    protected static String concatPath(String basePath, String... paths) {
        if (StringUtils.isBlank(basePath)) {
            basePath = Folder.Root_Path;
        }
        if (paths != null) {
            for (String path : paths) {
                path = StringUtils.trim(path);
                if (StringUtils.isBlank(path)) {
                    continue;
                }
                basePath = FilenameUtils.concat(basePath, path);
            }
        }
        if (basePath != null) {
            basePath = replaceSeparate(basePath);
            if (basePath.length() > 1 && basePath.endsWith(Folder.Path_Separate)) {
                basePath = basePath.substring(0, basePath.length() - 1);
            }
        }
        return basePath;
    }

    /**
     * 处理路径分隔符，使用统一的分隔符
     */
    protected static String replaceSeparate(String path) {
        if (StringUtils.isBlank(path)) {
            return path;
        }
        return path.replaceAll("\\\\", Folder.Path_Separate);
    }

    protected static boolean resourceExists(Resource resource) {
        boolean exists = resource != null && resource.exists() && resource.isReadable();
        if (!exists) {
            return false;
        }
        long contentLength = -1;
        try {
            contentLength = resource.contentLength();
        } catch (Exception ignored) {
        }
        return contentLength > 0;
    }
}
