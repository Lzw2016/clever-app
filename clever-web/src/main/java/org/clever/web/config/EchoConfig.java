package org.clever.web.config;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/12/07 19:49 <br/>
 */
@Data
public class EchoConfig {
    public static final String PREFIX = WebConfig.PREFIX + ".echo";

    /**
     * 忽略地址(支持AntPath风格)
     */
    private List<String> ignorePaths = new ArrayList<String>() {{
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
