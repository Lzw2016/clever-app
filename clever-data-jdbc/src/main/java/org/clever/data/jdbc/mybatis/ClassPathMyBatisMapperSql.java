package org.clever.data.jdbc.mybatis;

import lombok.SneakyThrows;
import org.clever.core.io.Resource;
import org.clever.core.io.support.PathMatchingResourcePatternResolver;
import org.clever.util.Assert;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * TODO: 深度测试，支持多个path
 * 作者：lizw <br/>
 * 创建时间：2020/09/30 15:51 <br/>
 */
public class ClassPathMyBatisMapperSql extends AbstractMyBatisMapperSql {
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
    private final List<String> locationPatterns;
    /**
     * 解析后的sql.xml文件集合
     */
    private final Set<Resource> resourceSet;

    /**
     * @param locationPattern classpath路径模式
     */
    public ClassPathMyBatisMapperSql(String locationPattern) {
        this(Collections.singletonList(locationPattern));
    }

    /**
     * @param locationPatterns classpath路径模式
     */
    public ClassPathMyBatisMapperSql(List<String> locationPatterns) {
        Assert.notEmpty(locationPatterns, "locationPatterns不能为空");
        for (String locationPattern : locationPatterns) {
            Assert.isNotBlank(locationPattern, "locationPattern不能为空");
        }
        this.locationPatterns = locationPatterns;
        this.resourceSet = initResource();
        initLoad();
    }

    /**
     * 解析sql.xml资源文件
     */
    @SneakyThrows
    protected synchronized Set<Resource> initResource() {
        Set<Resource> resourceSet = new HashSet<>();
        for (String locationPattern : locationPatterns) {
            int xmlFileSize = 0;
            Resource[] resources = PATH_MATCHING_RESOLVER.getResources(locationPattern);
            for (Resource resource : resources) {
                if (resource.isReadable() && resource.getURL().toExternalForm().toLowerCase().endsWith(".xml")) {
                    resourceSet.add(resource);
                    xmlFileSize++;
                }
            }
            log.debug("locationPattern={}加载完成 | resource-size={} | xml-file-size={}", locationPattern, resources.length, xmlFileSize);
        }
        log.info("Resource加载完成! | xml-file-size={}", resourceSet.size());
        return resourceSet;
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
        for (Resource resource : resourceSet) {
            try {
                final String absolutePath = resource.getURL().toExternalForm();
                log.info("# 解析文件: {}", absolutePath);
                reloadFile(absolutePath, true);
            } catch (Exception e) {
                log.error("解析sql.xml文件失败 | path={}", resource, e);
            }
        }
    }
}
