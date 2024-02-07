package org.clever.js.api.pool;

import lombok.Data;
import org.apache.commons.pool2.impl.BaseObjectPoolConfig;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.clever.js.api.ScriptEngineInstance;

import java.io.Serializable;
import java.time.Duration;

/**
 * 引擎池配置
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2024/02/04 21:38 <br/>
 */
@Data
public class EnginePoolConfig implements Serializable {
    // --------------------------------------------------------------------------------------------------------------------------------------- 基本参数

    /**
     * GenericObjectPool 提供了后进先出(LIFO)与先进先出(FIFO)两种行为模式的池。<br />
     * 默认为true，即当池中有空闲可用的对象时，调用borrowObject方法会返回最近（后进）的实例
     */
    private boolean lifo = BaseObjectPoolConfig.DEFAULT_LIFO;
    /**
     * 当从池中获取资源或者将资源还回池中时 <br />
     * 是否使用java.util.concurrent.locks.ReentrantLock.ReentrantLock的公平锁机制 <br />
     * 默认为false <br />
     */
    private boolean fairness = BaseObjectPoolConfig.DEFAULT_FAIRNESS;

    // --------------------------------------------------------------------------------------------------------------------------------------- 数量控制参数

    /**
     * 链接池中最大空闲的对象数,默认也为8
     */
    private int maxIdle = GenericObjectPoolConfig.DEFAULT_MAX_IDLE;
    /**
     * 对象池中最少空闲的对象数,默认为0
     */
    private int minIdle = GenericObjectPoolConfig.DEFAULT_MIN_IDLE;
    /**
     * 链接池中最大对象数,默认为8
     */
    private int maxTotal = GenericObjectPoolConfig.DEFAULT_MAX_TOTAL;

    // --------------------------------------------------------------------------------------------------------------------------------------- 超时参数

    /**
     * 当对象池资源耗尽时，等待时间，超出则抛异常，默认为-1即永不超时
     */
    private Duration maxWait = BaseObjectPoolConfig.DEFAULT_MAX_WAIT;
    /**
     * 当这个值为true的时候，maxWaitMillis参数才能生效。为false的时候，当对象池没资源，则立马抛异常。默认为true
     */
    private boolean blockWhenExhausted = BaseObjectPoolConfig.DEFAULT_BLOCK_WHEN_EXHAUSTED;

    // --------------------------------------------------------------------------------------------------------------------------------------- Test参数

    /**
     * 默认false，create的时候检测是有有效，如果无效则从对象池中移除，并尝试获取继续获取
     */
    private boolean testOnCreate = BaseObjectPoolConfig.DEFAULT_TEST_ON_CREATE;
    /**
     * 默认false，borrow的时候检测是有有效，如果无效则从对象池中移除，并尝试获取继续获取
     */
    private boolean testOnBorrow = BaseObjectPoolConfig.DEFAULT_TEST_ON_BORROW;
    /**
     * 默认false，return的时候检测是有有效，如果无效则从对象池中移除，并尝试获取继续获取
     */
    private boolean testOnReturn = BaseObjectPoolConfig.DEFAULT_TEST_ON_RETURN;
    /**
     * 默认false，在evictor线程里头，当evictionPolicy.evict方法返回false时，而且testWhileIdle为true的时候则检测是否有效，如果无效则移除
     */
    private boolean testWhileIdle = BaseObjectPoolConfig.DEFAULT_TEST_WHILE_IDLE;

    // --------------------------------------------------------------------------------------------------------------------------------------- 检测参数

    /**
     * 空闲对象检测线程检测的周期，毫秒数。如果为负值，表示不运行检测线程。默认为-1
     */
    private Duration timeBetweenEvictionRuns = BaseObjectPoolConfig.DEFAULT_TIME_BETWEEN_EVICTION_RUNS;
    /**
     * 在每次空闲对象回收器线程(如果有)运行时检查的对象数量，默认为3
     */
    private int numTestsPerEvictionRun = BaseObjectPoolConfig.DEFAULT_NUM_TESTS_PER_EVICTION_RUN;
    /**
     * 对象空闲的最小时间，达到此值后空闲对象将可能会被移除。默认为1000L * 60L * 30L
     */
    private Duration minEvictableIdleTime = BaseObjectPoolConfig.DEFAULT_MIN_EVICTABLE_IDLE_DURATION;
    /**
     * 对象空闲的最小时间，达到此值后空闲链接将会被移除，且保留minIdle个空闲对象数。默认为-1
     */
    private Duration softMinEvictableIdleTime = BaseObjectPoolConfig.DEFAULT_SOFT_MIN_EVICTABLE_IDLE_DURATION;
    /**
     * evict策略的类名，默认为org.apache.commons.pool2.impl.DefaultEvictionPolicy
     */
    private String evictionPolicyClassName = BaseObjectPoolConfig.DEFAULT_EVICTION_POLICY_CLASS_NAME;
    /**
     * shutdown驱逐线程的超时时间
     */
    private Duration evictorShutdownTimeout = BaseObjectPoolConfig.DEFAULT_EVICTOR_SHUTDOWN_TIMEOUT;

    // --------------------------------------------------------------------------------------------------------------------------------------- JMX

    /**
     * 是否注册JMX
     */
    private boolean jmxEnabled = BaseObjectPoolConfig.DEFAULT_JMX_ENABLE;
    /**
     * JMX前缀
     */
    private String jmxNamePrefix = BaseObjectPoolConfig.DEFAULT_JMX_NAME_PREFIX;
    /**
     * 使用base + jmxNamePrefix + idx 来生成ObjectName
     */
    private String jmxNameBase = BaseObjectPoolConfig.DEFAULT_JMX_NAME_BASE;

    // ---------------------------------------------------------------------------------------------------------------------------------------

    public <E, T> GenericObjectPoolConfig<ScriptEngineInstance<E, T>> toGenericObjectPoolConfig() {
        GenericObjectPoolConfig<ScriptEngineInstance<E, T>> config = new GenericObjectPoolConfig<>();
        config.setLifo(isLifo());
        config.setFairness(isFairness());
        config.setMaxIdle(getMaxIdle());
        config.setMinIdle(getMinIdle());
        config.setMaxTotal(getMaxTotal());
        config.setMaxWait(getMaxWait());
        config.setBlockWhenExhausted(isBlockWhenExhausted());
        config.setTestOnCreate(isTestOnCreate());
        config.setTestOnBorrow(isTestOnBorrow());
        config.setTestOnReturn(isTestOnReturn());
        config.setTestWhileIdle(isTestWhileIdle());
        config.setTimeBetweenEvictionRuns(getTimeBetweenEvictionRuns());
        config.setNumTestsPerEvictionRun(getNumTestsPerEvictionRun());
        config.setMinEvictableIdleTime(getMinEvictableIdleTime());
        config.setSoftMinEvictableIdleTime(getSoftMinEvictableIdleTime());
        config.setEvictionPolicyClassName(getEvictionPolicyClassName());
        config.setEvictorShutdownTimeout(getEvictorShutdownTimeout());
        config.setJmxEnabled(isJmxEnabled());
        config.setJmxNamePrefix(getJmxNamePrefix());
        config.setJmxNameBase(getJmxNameBase());
        return config;
    }
}
