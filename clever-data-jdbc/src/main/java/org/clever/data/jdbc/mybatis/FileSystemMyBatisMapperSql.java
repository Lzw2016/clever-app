package org.clever.data.jdbc.mybatis;

import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.Assert;
import org.springframework.util.AntPathMatcher;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 从操作系统的文件系统中读取sql.xml文件
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2020/09/02 15:56 <br/>
 */
public class FileSystemMyBatisMapperSql extends AbstractMyBatisMapperSql {
    private static final AntPathMatcher FILTER_MATCHER = new AntPathMatcher();

    /**
     * 根文件
     */
    @Getter
    private final File rootPath;
    /**
     * 根文件的绝对路径
     */
    @Getter
    private final String rootAbsolutePath;
    /**
     * ant风格的过滤器(为空则不过滤)
     */
    private final String filter;

    /**
     * @param rootPath sql.xml文件根路径
     * @param filter   ant风格的过滤字符串
     */
    public FileSystemMyBatisMapperSql(String rootPath, String filter) {
        this.rootPath = new File(rootPath);
        this.rootAbsolutePath = FilenameUtils.normalize(this.rootPath.getAbsolutePath(), true);
        Assert.isTrue(this.rootPath.isDirectory(), "路径：" + rootAbsolutePath + "不存在或者不是一个文件夹");
        this.filter = StringUtils.trim(filter);
    }

    /**
     * @param rootPath sql.xml文件根路径
     */
    public FileSystemMyBatisMapperSql(String rootPath) {
        this(rootPath, null);
    }

    @Override
    public boolean fileExists(String xmlPath) {
        String absolutePath = getAbsolutePath(xmlPath);
        return new File(absolutePath).isFile();
    }

    @SneakyThrows
    @Override
    public InputStream openInputStream(String xmlPath) {
        String absolutePath = getAbsolutePath(xmlPath);
        return FileUtils.openInputStream(new File(absolutePath));
    }

    @Override
    public String getXmlPath(String absolutePath) {
        absolutePath = FilenameUtils.normalize(absolutePath, true);
        return absolutePath.substring(rootAbsolutePath.length() + 1);
    }

    @Override
    public Map<String, Long> getAllLastModified() {
        Map<String, Long> result = new HashMap<>();
        Collection<File> files = FileUtils.listFiles(rootPath, new String[]{"xml"}, true);
        for (File file : files) {
            String xmlPath = getXmlPath(file.getAbsolutePath());
            if (StringUtils.isBlank(filter) || FILTER_MATCHER.match(filter, xmlPath)) {
                result.put(file.getAbsolutePath(), file.lastModified());
            }
        }
        return result;
    }

    @Override
    public String getAbsolutePath(String xmlPath) {
        return FilenameUtils.concat(rootAbsolutePath, xmlPath);
    }
}
