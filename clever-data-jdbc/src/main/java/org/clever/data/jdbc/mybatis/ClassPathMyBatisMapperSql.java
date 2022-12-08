package org.clever.data.jdbc.mybatis;

import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.io.FilenameUtils;
import org.clever.core.io.Resource;
import org.clever.core.io.support.PathMatchingResourcePatternResolver;
import org.clever.util.Assert;

import java.io.InputStream;
import java.util.*;

/**
 * TODO: 深度测试，支持多个path
 * 作者：lizw <br/>
 * 创建时间：2020/09/30 15:51 <br/>
 */
public class ClassPathMyBatisMapperSql extends AbstractMyBatisMapperSql {
    /**
     * Resource.getURL().toExternalForm()返回的路径例如: <br/>
     * <pre>{@code
     * "clever-data-jdbc/out/production/resources/org/clever/jdbc/support/sql-error-codes.xml"
     * "spring-context-5.3.23.jar!/org/springframework/remoting/rmi/RmiInvocationWrapperRTD.xml"
     * }</pre>
     * 此变量用户定位用class文件所对应的包名，改变成: <br/>
     * <pre>{@code
     * "org/clever/jdbc/support/sql-error-codes.xml"
     * "org/springframework/remoting/rmi/RmiInvocationWrapperRTD.xml"
     * }</pre>
     */
    public static final List<String> CLASS_URL_PREFIX = new ArrayList<String>() {{
        // jar包
        add(".jar!/");
        // IDEA 自带编译器
        add("/out/production/classes/");
        add("/out/production/resources/");
        add("/out/test/classes/");
        add("/out/test/resources/");
        // gradle编译器
        add("/build/classes/java/main/");
        add("/build/classes/java/test/");
        add("/build/classes/kotlin/main/");
        add("/build/classes/kotlin/test/");
        add("/build/classes/groovy/main/");
        add("/build/classes/groovy/test/");
        add("/build/resources/main/");
        add("/build/resources/test/");
        // maven编译器
        add("/target/classes/");
        add("/target/test-classes/");
    }};

    /**
     * 加载Resource实现对象
     */
    private static final PathMatchingResourcePatternResolver PATH_MATCHING_RESOLVER = new PathMatchingResourcePatternResolver();
    /**
     * 扫描资源文件的表达式,如:<br/>
     * <pre>
     * /WEB-INF/*-context.xml
     * com/mycompany/**&#47;applicationContext.xml
     * file:C:/some/path/*-context.xml
     * classpath:com/mycompany/**&#47;applicationContext.xml
     * </pre>
     */
    @Getter
    private final String locationPattern;

    /**
     * @param locationPattern classpath路径模式
     */
    public ClassPathMyBatisMapperSql(String locationPattern) {
        Assert.isNotBlank(locationPattern, "参数locationPattern不能为空");
        this.locationPattern = locationPattern;
        initLoad();
    }

    @Override
    public String getAbsolutePath(String xmlPath) {
        return xmlPath;
    }

    @Override
    public boolean fileExists(String xmlPath) {
        final Resource resource = PATH_MATCHING_RESOLVER.getResource(xmlPath);
        if (resource.isFile()) {
            try {
                if (!resource.getFile().isFile()) {
                    return false;
                }
            } catch (Exception e) {
                log.warn("Resource.getFile()异常", e);
            }
        }
        boolean exists = resource.exists() && resource.isReadable();
        if (!exists) {
            return false;
        }
        long contentLength = -1L;
        try {
            contentLength = resource.contentLength();
        } catch (Exception ignored) {
        }
        return contentLength > 0L;
    }

    @SneakyThrows
    @Override
    public InputStream openInputStream(String xmlPath) {
        final Resource resource = PATH_MATCHING_RESOLVER.getResource(xmlPath);
        return resource.getInputStream();
    }

    @Override
    public void reloadAll() {
        Set<Resource> resourceSet = initResource();
        for (Resource resource : resourceSet) {
            try {
                final String absolutePath = resource.getURL().toExternalForm();
                log.info("# 解析文件: {}", absolutePath);
                reloadFile(getXmlPath(absolutePath), true);
            } catch (Exception e) {
                log.error("解析sql.xml文件失败 | path={}", resource, e);
            }
        }
    }

    @SneakyThrows
    @Override
    public Map<String, Long> getAllLastModified() {
        Map<String, Long> result = new HashMap<>();
        Set<Resource> resourceSet = initResource();
        for (Resource resource : resourceSet) {
            if (resource.isFile()) {
                result.put(resource.getFile().getAbsolutePath(), resource.lastModified());
            } else {
                result.put(resource.getURL().toExternalForm(), resource.lastModified());
            }
        }
        return result;
    }

    /**
     * 解析sql.xml资源文件
     */
    @SneakyThrows
    protected synchronized Set<Resource> initResource() {
        Set<Resource> resourceSet = new HashSet<>();
        Resource[] resources = PATH_MATCHING_RESOLVER.getResources(locationPattern);
        for (Resource resource : resources) {
            if (resource.isReadable() && resource.getURL().toExternalForm().toLowerCase().endsWith(".xml")) {
                resourceSet.add(resource);
            }
        }
        return resourceSet;
    }

    /**
     * 把绝对路径转换成class path路径
     */
    protected String getXmlPath(String absolutePath) {
        String classPath = absolutePath;
        for (String prefix : CLASS_URL_PREFIX) {
            int idx = classPath.indexOf(prefix);
            if (idx >= 0) {
                classPath = classPath.substring(idx + prefix.length());
                break;
            }
        }
        classPath = FilenameUtils.normalize(classPath, true);
        return classPath;
    }
}
