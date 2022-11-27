package org.clever.data.jdbc.mybatis;

import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.clever.util.Assert;

import java.io.File;
import java.io.InputStream;
import java.util.*;

/**
 * TODO: 深度测试，支持多个path
 * 作者：lizw <br/>
 * 创建时间：2020/09/02 15:56 <br/>
 */
public class FileSystemMyBatisMapperSql extends AbstractMyBatisMapperSql {
    /**
     * sql.xml文件根路径 {@code LinkedHashMap<absoluteRootPath, rootFile>}
     */
    private final LinkedHashMap<String, File> rootFileMap = new LinkedHashMap<>();

    public FileSystemMyBatisMapperSql(String absolutePath) {
        this(Collections.singletonList(absolutePath));
    }

    public FileSystemMyBatisMapperSql(List<String> absolutePaths) {
        for (String absolutePath : absolutePaths) {
            File file = new File(absolutePath);
            String path = FilenameUtils.normalize(file.getAbsolutePath(), true);
            rootFileMap.put(path, file);
            Assert.isTrue(file.isDirectory(), String.format("路径:%s不存在或者不是一个文件夹", path));
        }
        initLoad();
    }

    /**
     * 获取sql.xml文件
     *
     * @param xmlPath sql.xml文件同路径，如：“org/clever/biz/dao/UserDao.xml”、“org/clever/biz/dao/UserDao.mysql.xml”
     * @return 不存在就返回 null
     */
    protected File getFile(String xmlPath) {
        for (String absoluteRootPath : rootFileMap.keySet()) {
            String absolutePath = FilenameUtils.concat(absoluteRootPath, xmlPath);
            File file = new File(absolutePath);
            if (file.isFile()) {
                return file;
            }
        }
        return null;
    }

    @Override
    public String getAbsolutePath(String xmlPath) {
        File file = getFile(xmlPath);
        if (file == null) {
            return null;
        }
        return file.getAbsolutePath();
    }

    @Override
    public boolean fileExists(String xmlPath) {
        return getFile(xmlPath) != null;
    }

    @SneakyThrows
    @Override
    public InputStream openInputStream(String xmlPath) {
        File file = getFile(xmlPath);
        Assert.notNull(file, String.format("文件不存在: %s", xmlPath));
        return FileUtils.openInputStream(file);
    }

    @Override
    public void reloadAll() {
        // 唯一的sql.xml文件地址
        HashSet<String> uniquePaths = new HashSet<>();
        // LinkedHashMap<absoluteRootPath, LinkedList<sql.xml的absolutePath>>
        LinkedHashMap<String, LinkedList<String>> absolutePathMap = new LinkedHashMap<>();
        // 查找所有的sql.xml文件
        for (Map.Entry<String, File> entry : rootFileMap.entrySet()) {
            String absoluteRootPath = entry.getKey();
            File rootFile = entry.getValue();
            LinkedList<String> absolutePaths = new LinkedList<>();
            Collection<File> files = FileUtils.listFiles(rootFile, new String[]{"xml"}, true);
            for (File file : files) {
                String absolutePath = FilenameUtils.normalize(file.getAbsolutePath(), true);
                if (uniquePaths.add(absolutePath)) {
                    absolutePaths.add(absolutePath);
                }
            }
            if (!absolutePaths.isEmpty()) {
                absolutePathMap.put(absoluteRootPath, absolutePaths);
            }
        }
        // 解析所有的sql.xml文件
        for (Map.Entry<String, LinkedList<String>> entry : absolutePathMap.entrySet()) {
            String absoluteRootPath = entry.getKey();
            LinkedList<String> absolutePaths = entry.getValue();
            for (String absolutePath : absolutePaths) {
                log.info("# 解析文件: {}", absolutePath);
                String xmlPath = absolutePath.substring(absoluteRootPath.length());
                if (xmlPath.startsWith("/") || xmlPath.startsWith("\\")) {
                    xmlPath = xmlPath.substring(1);
                }
                try {
                    reloadFile(xmlPath, true);
                } catch (Exception e) {
                    log.error("# 解析sql.xml文件失败 | path={}", absolutePath);
                }
            }
        }
    }
}
