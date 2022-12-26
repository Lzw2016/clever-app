package org.clever.data.jdbc.config;

import lombok.Data;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/12/26 13:43 <br/>
 */
@Data
public class MybatisConfig {
    public static final String PREFIX = "mybatis";

    /**
     * 是否启用mybatis配置
     */
    private boolean enable = false;
    /**
     * mapper.xml文件路径配置
     */
    private List<MapperLocation> locations = new ArrayList<>();

    /**
     * 文件类型
     */
    public enum FileType {
        /**
         * 物理文件系统
         */
        FileSystem,
        /**
         * Java Jar包
         */
        Jar,
    }

    @Data
    public static class MapperLocation {
        /**
         * 文件系统类型
         */
        private FileType fileType = FileType.FileSystem;
        /**
         * mapper.xml文件路径
         */
        private String location;
        /**
         * ant风格的过滤器(为空则不过滤)
         */
        private String filter;
        /**
         * 是否实时动态加载 mapper.xml
         */
        private boolean watcher = false;
        /**
         * 文件检查时间间隔(默认1秒)
         */
        private Duration interval = Duration.ofSeconds(1);
    }
}
