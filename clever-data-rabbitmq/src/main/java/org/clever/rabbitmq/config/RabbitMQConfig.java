package org.clever.rabbitmq.config;

import lombok.Data;

import java.util.Collections;
import java.util.Map;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/03/08 16:15 <br/>
 */
@Data
public class RabbitMQConfig {
    public static final String PREFIX = "rabbitmq";

    /**
     * 是否启用 rabbitmq 配置
     */
    private boolean enable = false;
    /**
     * 默认的 rabbitmq 名称
     */
    private String defaultName = "default";
    /**
     * rabbitmq 全局配置
     */
    private RabbitMQProperties global = new RabbitMQProperties();
    /**
     * rabbitmq 数据源集合(数据源名称 --> 数据源配置)
     */
    private Map<String, RabbitMQProperties> dataSource = Collections.emptyMap();
}
