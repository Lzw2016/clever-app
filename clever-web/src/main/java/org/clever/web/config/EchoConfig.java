package org.clever.web.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/12/07 19:49 <br/>
 */
@ConfigurationProperties(prefix = EchoConfig.PREFIX)
@Data
public class EchoConfig {
    public static final String PREFIX = WebConfig.PREFIX + ".echo";

    /**
     * 启用 EchoFilter
     */
    private boolean enable = true;
    /**
     * 忽略地址(支持AntPath风格)
     */
    private List<String> ignorePaths = new ArrayList<>() {{
        add("/**/*.png");
        add("/**/*.ico");
        add("/**/*.jpg");
        add("/**/*.gif");
        add("/**/*.js");
        add("/**/*.css");
        add("/**/*.html");
        add("/**/*.ts");
        add("/**/*.tsx");
        add("/**/*.map");
        add("/**/*.ttf");
        add("/**/*.woff");
        add("/**/*.woff2");
    }};
}
