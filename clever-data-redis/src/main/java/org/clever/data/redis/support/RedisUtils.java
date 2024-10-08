package org.clever.data.redis.support;

import lombok.SneakyThrows;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.*;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/11/12 13:53 <br/>
 */
public class RedisUtils {
    // --------------------------------------------------------------------------------------------
    // 读取 Redis 连接池状态
    // --------------------------------------------------------------------------------------------

    public static RedisPoolStatus getPoolStatus(RedisTemplate<?, ?> redisTemplate) {
        RedisConnectionFactory connectionFactory = redisTemplate.getConnectionFactory();
        if (connectionFactory instanceof LettuceConnectionFactory) {
            return getPoolStatus((LettuceConnectionFactory) connectionFactory);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    protected static RedisPoolStatus getPoolStatus(LettuceConnectionFactory lettuceConnectionFactory) {
        Object connectionProvider = getField(LettuceConnectionFactory.class, "connectionProvider", lettuceConnectionFactory);
        final String providerClassName = "org.springframework.data.redis.connection.lettuce.LettucePoolingConnectionProvider";
        if (connectionProvider != null && providerClassName.equals(connectionProvider.getClass().getName())) {
            Object pools = getField(Class.forName(providerClassName), "pools", connectionProvider);
            if (pools instanceof Map) {
                Collection<GenericObjectPool<?>> poolList = ((Map<?, GenericObjectPool<?>>) pools).values();
                return getPoolStatus(poolList);
            }
        }
        final String providerClassName_2 = "org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory$ExceptionTranslatingConnectionProvider";
        if (connectionProvider != null && providerClassName_2.equals(connectionProvider.getClass().getName())) {
            Object delegate = getField(connectionProvider.getClass(), "delegate", connectionProvider);
            if (delegate == null) {
                return null;
            }
            Object poolRef = getField(delegate.getClass(), "poolRef", delegate);
            if (poolRef instanceof Map) {
                Collection<GenericObjectPool<?>> poolList = ((Map<?, GenericObjectPool<?>>) poolRef).values();
                return getPoolStatus(poolList);
            }
        }
        return null;
    }

    protected static RedisPoolStatus getPoolStatus(Collection<GenericObjectPool<?>> poolList) {
        List<RedisPoolStatus> list = new ArrayList<>();
        poolList.forEach(tmpPool -> list.add(getPoolStatus(tmpPool)));
        if (list.isEmpty()) {
            return null;
        }
        RedisPoolStatus status = new RedisPoolStatus();
        status.setMeanActiveTimeMillis(0L);
        status.setMeanIdleTimeMillis(0L);
        for (RedisPoolStatus redisDataSourceStatus : list) {
            status.setTotalConnections(status.getTotalConnections() + redisDataSourceStatus.getTotalConnections());
            status.setActiveConnections(status.getActiveConnections() + redisDataSourceStatus.getActiveConnections());
            status.setIdleConnections(status.getIdleConnections() + redisDataSourceStatus.getIdleConnections());
            status.setThreadsAwaitingConnection(status.getThreadsAwaitingConnection() + redisDataSourceStatus.getThreadsAwaitingConnection());
            if (status.getMaxBorrowWaitTimeMillis() < redisDataSourceStatus.getMaxBorrowWaitTimeMillis()) {
                status.setMaxBorrowWaitTimeMillis(redisDataSourceStatus.getMaxBorrowWaitTimeMillis());
            }
            status.setMeanBorrowWaitTimeMillis(status.getMeanBorrowWaitTimeMillis() + redisDataSourceStatus.getMeanBorrowWaitTimeMillis());
            status.setMeanActiveTimeMillis(status.getMeanActiveTimeMillis() + redisDataSourceStatus.getMeanActiveTimeMillis());
            status.setMeanIdleTimeMillis(status.getMeanIdleTimeMillis() + redisDataSourceStatus.getMeanIdleTimeMillis());
        }
        status.setMeanBorrowWaitTimeMillis(status.getMeanBorrowWaitTimeMillis() / list.size());
        status.setMeanActiveTimeMillis(status.getMeanActiveTimeMillis() / list.size());
        status.setMeanIdleTimeMillis(status.getMeanIdleTimeMillis() / list.size());
        return status;
    }

    protected static RedisPoolStatus getPoolStatus(GenericObjectPool<?> genericObjectPool) {
        RedisPoolStatus status = new RedisPoolStatus();
        status.setIdleConnections(genericObjectPool.getNumIdle());
        status.setActiveConnections(genericObjectPool.getNumActive());
        status.setTotalConnections(status.getIdleConnections() + status.getActiveConnections());
        status.setThreadsAwaitingConnection(genericObjectPool.getNumWaiters());
        status.setMaxBorrowWaitTimeMillis(genericObjectPool.getMaxBorrowWaitDuration().toMillis());
        status.setMeanBorrowWaitTimeMillis(genericObjectPool.getMeanBorrowWaitDuration().toMillis());
        status.setMeanActiveTimeMillis(genericObjectPool.getMeanActiveDuration().toMillis());
        status.setMeanIdleTimeMillis(genericObjectPool.getMeanIdleDuration().toMillis());
        return status;
    }

    // --------------------------------------------------------------------------------------------
    // 读取 Redis 连接池状态
    // --------------------------------------------------------------------------------------------

    public static RedisInfo getRedisInfo(RedisTemplate<?, ?> redisTemplate) {
        RedisConnectionFactory connectionFactory = redisTemplate.getConnectionFactory();
        if (connectionFactory instanceof LettuceConnectionFactory) {
            return getRedisInfo((LettuceConnectionFactory) connectionFactory);
        }
        return null;
    }

    protected static RedisInfo getRedisInfo(LettuceConnectionFactory lettuceConnectionFactory) {
        RedisInfo redisInfo = new RedisInfo();
        if (lettuceConnectionFactory.getClusterConfiguration() != null) {
            redisInfo.setClusterInfo(getRedisClusterInfo(lettuceConnectionFactory.getClusterConfiguration()));
        }
        if (lettuceConnectionFactory.getSentinelConfiguration() != null) {
            redisInfo.setSentinelInfo(getRedisSentinelInfo(lettuceConnectionFactory.getSentinelConfiguration()));
        }
        if (lettuceConnectionFactory.getSocketConfiguration() != null) {
            redisInfo.setSocketInfo(getRedisSocketInfo(lettuceConnectionFactory.getSocketConfiguration()));
        }
        fill(redisInfo, lettuceConnectionFactory.getStandaloneConfiguration());
        Object configObj = getField(LettuceConnectionFactory.class, "configuration", lettuceConnectionFactory);
        fill(redisInfo, configObj);
        return redisInfo;
    }

    protected static RedisInfo.ClusterInfo getRedisClusterInfo(RedisClusterConfiguration configuration) {
        RedisInfo.ClusterInfo clusterInfo = new RedisInfo.ClusterInfo();
        clusterInfo.setMaxRedirects(configuration.getMaxRedirects());
        Set<RedisInfo.RedisNodeInfo> redisNodeInfos = new LinkedHashSet<>();
        clusterInfo.setNodes(redisNodeInfos);
        Set<RedisNode> nodes = configuration.getClusterNodes();
        for (RedisNode node : nodes) {
            redisNodeInfos.add(new RedisInfo.RedisNodeInfo(node));
        }
        return clusterInfo;
    }

    protected static RedisInfo.SentinelInfo getRedisSentinelInfo(RedisSentinelConfiguration configuration) {
        RedisInfo.SentinelInfo sentinelInfo = new RedisInfo.SentinelInfo();
        if (configuration.getMaster() != null) {
            sentinelInfo.setMaster(configuration.getMaster().getName());
        }
        sentinelInfo.setDatabase(configuration.getDatabase());
        Set<RedisInfo.RedisNodeInfo> redisNodeInfos = new LinkedHashSet<>();
        sentinelInfo.setSentinels(redisNodeInfos);
        return sentinelInfo;
    }

    protected static RedisInfo.SocketInfo getRedisSocketInfo(RedisSocketConfiguration configuration) {
        RedisInfo.SocketInfo socketInfo = new RedisInfo.SocketInfo();
        socketInfo.setSocket(configuration.getSocket());
        socketInfo.setDatabase(configuration.getDatabase());
        return socketInfo;
    }

    protected static RedisInfo.StandaloneInfo getRedisStandaloneInfo(RedisStandaloneConfiguration configuration) {
        RedisInfo.StandaloneInfo standaloneInfo = new RedisInfo.StandaloneInfo();
        standaloneInfo.setHostName(configuration.getHostName());
        standaloneInfo.setPort(configuration.getPort());
        standaloneInfo.setDatabase(configuration.getDatabase());
        return standaloneInfo;
    }

    protected static RedisInfo.StaticMasterReplicaInfo getRedisStaticMasterReplicaInfo(RedisStaticMasterReplicaConfiguration configuration) {
        RedisInfo.StaticMasterReplicaInfo staticMasterReplicaInfo = new RedisInfo.StaticMasterReplicaInfo();
        List<RedisInfo.StandaloneInfo> nodes = new ArrayList<>();
        for (RedisStandaloneConfiguration node : configuration.getNodes()) {
            nodes.add(new RedisInfo.StandaloneInfo(node));
        }
        staticMasterReplicaInfo.setNodes(nodes);
        staticMasterReplicaInfo.setDatabase(configuration.getDatabase());
        return staticMasterReplicaInfo;
    }

    protected static void fill(RedisInfo redisInfo, Object configObj) {
        if (configObj == null) {
            return;
        }
        if (redisInfo.getClusterInfo() == null && configObj instanceof RedisClusterConfiguration) {
            redisInfo.setClusterInfo(getRedisClusterInfo((RedisClusterConfiguration) configObj));
        } else if (redisInfo.getSocketInfo() == null && configObj instanceof RedisSocketConfiguration) {
            redisInfo.setSocketInfo(getRedisSocketInfo((RedisSocketConfiguration) configObj));
        } else if (redisInfo.getSentinelInfo() == null && configObj instanceof RedisSentinelConfiguration) {
            redisInfo.setSentinelInfo(getRedisSentinelInfo((RedisSentinelConfiguration) configObj));
        } else if (redisInfo.getStandaloneInfo() == null && configObj instanceof RedisStandaloneConfiguration) {
            fill(redisInfo, (RedisStandaloneConfiguration) configObj);
        } else if (configObj instanceof RedisStaticMasterReplicaConfiguration) {
            redisInfo.setStaticMasterReplicaInfo(getRedisStaticMasterReplicaInfo((RedisStaticMasterReplicaConfiguration) configObj));
        }
    }

    protected static void fill(RedisInfo redisInfo, RedisStandaloneConfiguration redisStandaloneConfiguration) {
        RedisInfo.StandaloneInfo standaloneInfo = getRedisStandaloneInfo(redisStandaloneConfiguration);
        if (redisInfo.getClusterInfo() == null
            && redisInfo.getSentinelInfo() == null
            && redisInfo.getSocketInfo() == null
            && redisInfo.getStaticMasterReplicaInfo() == null
            || !Objects.equals(standaloneInfo.getHostName(), "localhost")
            || !Objects.equals(standaloneInfo.getPort(), 6379)) {
            redisInfo.setStandaloneInfo(standaloneInfo);
        }
    }

    // --------------------------------------------------------------------------------------------
    // 内部函数
    // --------------------------------------------------------------------------------------------

    @SneakyThrows
    private static Object getField(Class<?> clazz, String name, Object target) {
        Field field = clazz.getDeclaredField(name);
        field.setAccessible(true);
        return ReflectionUtils.getField(field, target);
    }
}
