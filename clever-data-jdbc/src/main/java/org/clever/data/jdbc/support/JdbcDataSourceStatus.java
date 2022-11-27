package org.clever.data.jdbc.support;

import lombok.Data;

import java.io.Serializable;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/08/08 22:18 <br/>
 */
@Data
public class JdbcDataSourceStatus implements Serializable {
    /**
     * 总连接数
     */
    private int totalConnections;
    /**
     * 活动连接数
     */
    private int activeConnections;
    /**
     * 空闲连接数
     */
    private int idleConnections;
    /**
     * 等待连接的线程数
     */
    private int threadsAwaitingConnection;
}
