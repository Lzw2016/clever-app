package org.clever.data.jdbc.mybatis;

import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.clever.util.Assert;

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

    public FileSystemMyBatisMapperSql(String absolutePath) {
        rootPath = new File(absolutePath);
        rootAbsolutePath = FilenameUtils.normalize(rootPath.getAbsolutePath(), true);
        Assert.isTrue(rootPath.isDirectory(), "路径：" + rootAbsolutePath + "不存在或者不是一个文件夹");
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
            result.put(file.getAbsolutePath(), file.lastModified());
        }
        return result;
    }

    @Override
    public String getAbsolutePath(String xmlPath) {
        return FilenameUtils.concat(rootAbsolutePath, xmlPath);
    }
}
