package org.clever.data.jdbc.support.sqlparser.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Setter;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * jsqlparser 缓存 Caffeine 缓存抽象类
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2025/01/12 18:32 <br/>
 */
public abstract class AbstractCaffeineJsqlParseCache implements JSqlParseCache {
    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    protected final Cache<String, byte[]> cache;
    @Setter
    protected boolean async = false;
    @Setter
    protected Executor executor;

    public AbstractCaffeineJsqlParseCache(Cache<String, byte[]> cache) {
        this.cache = cache;
    }

    public AbstractCaffeineJsqlParseCache(Consumer<Caffeine<Object, Object>> consumer) {
        Caffeine<Object, Object> caffeine = Caffeine.newBuilder();
        consumer.accept(caffeine);
        this.cache = caffeine.build();
    }

    @Override
    public void putStatement(String sql, Statement value) {
        this.put(sql, value);
    }

    @Override
    public void putStatements(String sql, Statements value) {
        this.put(sql, value);
    }

    @Override
    public Statement getStatement(String sql) {
        return this.get(sql);
    }

    @Override
    public Statements getStatements(String sql) {
        return this.get(sql);
    }

    /**
     * 获取解析对象，异常清空缓存逻辑
     *
     * @param sql sql
     * @return 返回泛型对象
     */
    @SuppressWarnings("unchecked")
    protected <T> T get(String sql) {
        byte[] bytes = cache.getIfPresent(sql);
        if (null != bytes) {
            try {
                return (T) deserialize(sql, bytes);
            } catch (Exception e) {
                cache.invalidate(sql);
                log.error("deserialize error", e);
            }
        }
        return null;
    }

    /**
     * 存储解析对象
     *
     * @param sql   sql
     * @param value 解析对象
     */
    protected void put(String sql, Object value) {
        if (async) {
            if (executor != null) {
                CompletableFuture.runAsync(() -> cache.put(sql, serialize(value)), executor);
            } else {
                CompletableFuture.runAsync(() -> cache.put(sql, serialize(value)));
            }
        } else {
            cache.put(sql, serialize(value));
        }
    }

    /**
     * 序列化
     */
    protected abstract byte[] serialize(Object obj);

    /**
     * 反序列化
     */
    protected abstract Object deserialize(String sql, byte[] bytes);
}
