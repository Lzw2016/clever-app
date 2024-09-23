package org.clever.web.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.cors.CorsConfiguration;

import java.util.Collections;
import java.util.List;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/12/23 10:16 <br/>
 */
@ConfigurationProperties(prefix = CorsConfig.PREFIX)
public class CorsConfig extends CorsConfiguration {
    public static final String PREFIX = WebConfig.PREFIX + ".cors";
    /**
     * 启用 CorsFilter
     */
    @Setter
    @Getter
    private boolean enable = false;
    /**
     * 支持跨域的path(支持AntPath风格，默认：“/**”)
     */
    @Setter
    @Getter
    private List<String> pathPattern = Collections.singletonList("/**");
}
