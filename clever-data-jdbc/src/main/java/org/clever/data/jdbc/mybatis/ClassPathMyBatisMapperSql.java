package org.clever.data.jdbc.mybatis;

import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.Assert;
import org.clever.core.ResourcePathUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.AntPathMatcher;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 从classpath中读取sql.xml文件
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2020/09/30 15:51 <br/>
 */
public class ClassPathMyBatisMapperSql extends AbstractMyBatisMapperSql {
    private static final AntPathMatcher FILTER_MATCHER = new AntPathMatcher();

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
     * /WEB-INF/*.xml
     * com/mycompany/**&#47;*.xml
     * file:C:/some/path/*.xml
     * classpath:com/mycompany/**&#47;*.xml
     * </pre>
     */
    @Getter
    private final String locationPattern;
    /**
     * ant风格的过滤器(为空则不过滤)
     */
    private final String filter;

    /**
     * @param locationPattern classpath路径模式
     * @param filter          ant风格的过滤字符串
     */
    public ClassPathMyBatisMapperSql(String locationPattern, String filter) {
        Assert.isNotBlank(locationPattern, "参数locationPattern不能为空");
        this.locationPattern = locationPattern;
        this.filter = StringUtils.trim(filter);
    }

    /**
     * @param locationPattern classpath路径模式
     */
    public ClassPathMyBatisMapperSql(String locationPattern) {
        this(locationPattern, null);
    }

    @Override
    public boolean fileExists(String xmlPath) {
        final Resource resource = PATH_MATCHING_RESOLVER.getResource(xmlPath);
        return ResourcePathUtils.isExistsFile(resource);
    }

    @SneakyThrows
    @Override
    public InputStream openInputStream(String xmlPath) {
        Resource resource = PATH_MATCHING_RESOLVER.getResource(xmlPath);
        return resource.getInputStream();
    }

    @Override
    public String getXmlPath(String absolutePath) {
        String classPath = FilenameUtils.normalize(absolutePath, true);
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

    @SneakyThrows
    @Override
    public Map<String, Long> getAllLastModified() {
        Map<String, Long> result = new HashMap<>();
        Resource[] resources = PATH_MATCHING_RESOLVER.getResources(locationPattern);
        for (Resource resource : resources) {
            if (resource.isReadable() && resource.getURL().toExternalForm().toLowerCase().endsWith(".xml")) {
                String absolutePath;
                if (resource.isFile()) {
                    absolutePath = resource.getFile().getAbsolutePath();
                } else {
                    absolutePath = resource.getURL().toExternalForm();
                }
                if (StringUtils.isBlank(filter) || FILTER_MATCHER.match(filter, StringUtils.replace(absolutePath, "\\", "/"))) {
                    result.put(absolutePath, resource.lastModified());
                }
            }
        }
        return result;
    }

    @Override
    public String getAbsolutePath(String xmlPath) {
        return xmlPath;
    }
}
