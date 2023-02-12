package org.clever.data.redis.connection.lettuce;

import io.lettuce.core.LettuceFutures;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.async.RedisKeyAsyncCommands;
import io.lettuce.core.api.async.RedisServerAsyncCommands;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;
import org.clever.data.redis.connection.RedisNode;
import org.clever.data.redis.connection.RedisServerCommands;
import org.clever.data.redis.core.types.RedisClientInfo;
import org.clever.util.Assert;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/01/30 22:21 <br/>
 */
class LettuceServerCommands implements RedisServerCommands {
    private final LettuceConnection connection;

    LettuceServerCommands(LettuceConnection connection) {
        this.connection = connection;
    }

    @Override
    public void bgReWriteAof() {
        connection.invokeStatus().just(RedisServerAsyncCommands::bgrewriteaof);
    }

    @Override
    public void bgSave() {
        connection.invokeStatus().just(RedisServerAsyncCommands::bgsave);
    }

    @Override
    public Long lastSave() {
        return connection.invoke().from(RedisServerAsyncCommands::lastsave).get(LettuceConverters::toLong);
    }

    @Override
    public void save() {
        connection.invokeStatus().just(RedisServerAsyncCommands::save);
    }

    @Override
    public Long dbSize() {
        return connection.invoke().just(RedisServerAsyncCommands::dbsize);
    }

    @Override
    public void flushDb() {
        connection.invokeStatus().just(RedisServerAsyncCommands::flushdb);
    }

    @Override
    public void flushAll() {
        connection.invokeStatus().just(RedisServerAsyncCommands::flushall);
    }

    @Override
    public Properties info() {
        return connection.invoke().from(RedisServerAsyncCommands::info).get(LettuceConverters.stringToProps());
    }

    @Override
    public Properties info(String section) {
        Assert.hasText(section, "Section must not be null or empty!");
        return connection.invoke().from(RedisServerAsyncCommands::info, section).get(LettuceConverters.stringToProps());
    }

    @Override
    public void shutdown() {
        connection.invokeStatus().just(it -> {
            it.shutdown(true);
            return new CompletedRedisFuture<>(null);
        });
    }

    @Override
    public void shutdown(ShutdownOption option) {
        if (option == null) {
            shutdown();
            return;
        }
        boolean save = ShutdownOption.SAVE.equals(option);
        connection.invokeStatus().just(it -> {
            it.shutdown(save);
            return new CompletedRedisFuture<>(null);
        });
    }

    @Override
    public Properties getConfig(String pattern) {
        Assert.hasText(pattern, "Pattern must not be null or empty!");
        return connection.invoke().from(RedisServerAsyncCommands::configGet, pattern).get(LettuceConverters.mapToPropertiesConverter());
    }

    @Override
    public void setConfig(String param, String value) {
        Assert.hasText(param, "Parameter must not be null or empty!");
        Assert.hasText(value, "Value must not be null or empty!");
        connection.invokeStatus().just(RedisServerAsyncCommands::configSet, param, value);
    }

    @Override
    public void resetConfigStats() {
        connection.invokeStatus().just(RedisServerAsyncCommands::configResetstat);
    }

    @Override
    public void rewriteConfig() {
        connection.invokeStatus().just(RedisServerAsyncCommands::configRewrite);
    }

    @Override
    public Long time(TimeUnit timeUnit) {
        Assert.notNull(timeUnit, "TimeUnit must not be null.");
        return connection.invoke().from(RedisServerAsyncCommands::time).get(LettuceConverters.toTimeConverter(timeUnit));
    }

    @Override
    public void killClient(String host, int port) {
        Assert.hasText(host, "Host for 'CLIENT KILL' must not be 'null' or 'empty'.");
        String client = String.format("%s:%s", host, port);
        connection.invoke().just(RedisServerAsyncCommands::clientKill, client);
    }

    @Override
    public void setClientName(byte[] name) {
        Assert.notNull(name, "Name must not be null!");
        connection.invoke().just(RedisServerAsyncCommands::clientSetname, name);
    }

    @Override
    public String getClientName() {
        return connection.invoke().from(RedisServerAsyncCommands::clientGetname).get(LettuceConverters::toString);
    }

    @Override
    public List<RedisClientInfo> getClientList() {
        if (connection.isPipelined()) {
            throw new UnsupportedOperationException("Cannot be called in pipeline mode.");
        }
        return connection.invoke().from(RedisServerAsyncCommands::clientList).get(LettuceConverters.stringToRedisClientListConverter());
    }

    @Override
    public void slaveOf(String host, int port) {
        Assert.hasText(host, "Host must not be null for 'SLAVEOF' command.");
        connection.invoke().just(RedisServerAsyncCommands::replicaof, host, port);
    }

    @Override
    public void slaveOfNoOne() {
        connection.invoke().just(RedisServerAsyncCommands::replicaofNoOne);
    }

    @Override
    public void migrate(byte[] key, RedisNode target, int dbIndex, MigrateOption option) {
        migrate(key, target, dbIndex, option, Long.MAX_VALUE);
    }

    @Override
    public void migrate(byte[] key, RedisNode target, int dbIndex, MigrateOption option, long timeout) {
        Assert.notNull(key, "Key must not be null!");
        Assert.notNull(target, "Target node must not be null!");
        connection.invoke().just(RedisKeyAsyncCommands::migrate, target.getHost(), target.getPort(), key, dbIndex, timeout);
    }

    public RedisClusterCommands<byte[], byte[]> getConnection() {
        return connection.getConnection();
    }

    static class CompletedRedisFuture<T> extends CompletableFuture<T> implements RedisFuture<T> {
        public CompletedRedisFuture(T value) {
            complete(value);
        }

        @Override
        public String getError() {
            return "";
        }

        @Override
        public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
            return LettuceFutures.awaitAll(timeout, unit, this);
        }
    }
}
