package org.clever.rabbitmq.pool;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.clever.core.Assert;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/03/09 14:18 <br/>
 */
@Slf4j
public class ConnectionFactoryWrapper extends BasePooledObjectFactory<Connection> {
    private final ConnectionFactory connectionFactory;
    /**
     * 数据源名称
     */
    @Getter
    private final String name;
    /**
     * 连接序列号
     */
    private final AtomicLong connectionSerialNO = new AtomicLong(0);

    public ConnectionFactoryWrapper(String name, ConnectionFactory connectionFactory) {
        Assert.isNotBlank(name, "参数 name 不能为空");
        Assert.notNull(connectionFactory, "参数 connectionFactory 不能为null");
        this.name = name;
        this.connectionFactory = connectionFactory;
    }

    /**
     * 创建一个新的 Connection
     */
    @Override
    public Connection create() throws Exception {
        return connectionFactory.newConnection(createConnectionName());
    }

    /**
     * 封装为池化对象
     */
    @Override
    public PooledObject<Connection> wrap(Connection obj) {
        return new DefaultPooledObject<>(obj);
    }

    /**
     * 验证对象是否可用
     */
    @Override
    public boolean validateObject(PooledObject<Connection> p) {
        try {
        } catch (Exception e) {
            log.warn("验证 Connection | 结果:验证失败", e);
            return false;
        }
        return true;
    }

    /**
     * 激活对象，从池中取对象时会调用此方法
     */
    @Override
    public void activateObject(PooledObject<Connection> p) throws Exception {
    }

    /**
     * 钝化对象，向池中返还对象时会调用此方法
     */
    @Override
    public void passivateObject(PooledObject<Connection> p) throws Exception {
    }

    /**
     * 销毁对象
     */
    @Override
    public void destroyObject(PooledObject<Connection> p) throws Exception {
        Connection con = p.getObject();
        if (con != null) {
            con.close();
        }
    }

    private String createConnectionName() {
        return String.format("%s_%s", this.name, connectionSerialNO.incrementAndGet());
    }
}
