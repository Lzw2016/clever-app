package org.clever.data.jdbc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/12/26 13:43 <br/>
 */
@ConfigurationProperties(prefix = MybatisConfig.PREFIX)
@Data
public class MybatisConfig {
    public static final String PREFIX = "mybatis";

    /**
     * 是否启用mybatis配置
     */
    private boolean enable = false;
    /**
     * 是否实时动态加载 mapper.xml
     */
    private boolean watcher = false;
    /**
     * 文件检查时间间隔(默认1秒)
     */
    private Duration interval = Duration.ofSeconds(1);
    /**
     * mapper.xml文件路径配置
     */
    private List<MapperLocation> locations = Collections.emptyList();

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
         * <pre>
         * 1.classpath路径
         *   classpath:com/mycompany/**&#47;*.xml
         *   classpath*:com/mycompany/**&#47;*.xml
         * 2.本机绝对路径
         *   file:/home/www/public/
         *   file:D:/resources/static/
         * 3.本机相对路径
         *   ../public/static
         *   ./public
         *   ../../dist
         * </pre>
         */
        private String location;
        /**
         * ant风格的过滤器(为空则不过滤)
         * <pre>
         *  1.dao/**&#47;*.xml
         *  2.**&#47;*.xml
         * </pre>
         */
        private String filter;
    }
}
