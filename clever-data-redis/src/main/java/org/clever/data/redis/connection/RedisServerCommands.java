package org.clever.data.redis.connection;

import org.clever.data.redis.core.types.RedisClientInfo;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Redis 支持的特定于服务器的命令
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 17:21 <br/>
 */
public interface RedisServerCommands {
    enum ShutdownOption {
        SAVE, NOSAVE
    }

    enum MigrateOption {
        COPY, REPLACE
    }

    /**
     * 在服务器上启动 {@literal Append Only File} 重写过程
     *
     * @see <a href="https://redis.io/commands/bgrewriteaof">Redis 文档: BGREWRITEAOF</a>
     */
    void bgReWriteAof();

    /**
     * 在服务器上开始后台保存数据库
     *
     * @see <a href="https://redis.io/commands/bgsave">Redis 文档: BGSAVE</a>
     */
    void bgSave();

    /**
     * 以秒为单位获取最后一次 {@link #bgSave()} 操作的时间
     *
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/lastsave">Redis 文档: LASTSAVE</a>
     */
    Long lastSave();

    /**
     * 在服务器上同步保存当前数据库快照
     *
     * @see <a href="https://redis.io/commands/save">Redis 文档: SAVE</a>
     */
    void save();

    /**
     * 获取当前所选数据库中可用键的总数
     *
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/dbsize">Redis 文档: DBSIZE</a>
     */
    Long dbSize();

    /**
     * 删除当前所选数据库的所有键
     *
     * @see <a href="https://redis.io/commands/flushdb">Redis 文档: FLUSHDB</a>
     */
    void flushDb();

    /**
     * 从<b>所有数据库</b>中删除所有<b>所有键</b>
     *
     * @see <a href="https://redis.io/commands/flushall">Redis 文档: FLUSHALL</a>
     */
    void flushAll();

    /**
     * 加载 {@literal default} 服务器信息，例如
     * <ul>
     * <li>memory</li>
     * <li>cpu utilization</li>
     * <li>replication</li>
     * </ul>
     *
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/info">Redis 文档: INFO</a>
     */
    Properties info();

    /**
     * 为给定的 {@code selection} 加载服务器信息
     *
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/info">Redis 文档: INFO</a>
     */
    Properties info(String section);

    /**
     * 关闭服务器
     *
     * @see <a href="https://redis.io/commands/shutdown">Redis 文档: SHUTDOWN</a>
     */
    void shutdown();

    /**
     * 关闭服务器
     *
     * @see <a href="https://redis.io/commands/shutdown">Redis 文档: SHUTDOWN</a>
     */
    void shutdown(RedisServerCommands.ShutdownOption option);

    /**
     * 从服务器加载给定 {@code pattern} 的配置参数
     *
     * @param pattern 不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/config-get">Redis 文档: CONFIG GET</a>
     */
    Properties getConfig(String pattern);

    /**
     * 将 {@code param} 的服务器配置设置为 {@code value}
     *
     * @param param 不能是 {@literal null}
     * @param value 不能是 {@literal null}
     * @see <a href="https://redis.io/commands/config-set">Redis 文档: CONFIG SET</a>
     */
    void setConfig(String param, String value);

    /**
     * 重置服务器上的统计计数器。<br>
     * 可以使用 {@link #info()} 检索计数器
     *
     * @see <a href="https://redis.io/commands/config-resetstat">Redis 文档: CONFIG RESETSTAT</a>
     */
    void resetConfigStats();

    /**
     * 重写 {@code redis.conf} 文件
     *
     * @see <a href="https://redis.io/commands/config-rewrite">Redis 文档: CONFIG REWRITE</a>
     */
    void rewriteConfig();

    /**
     * 在 {@link TimeUnit#MILLISECONDS} 中使用 {@code TIME} 命令请求服务器时间戳
     *
     * @return 当前服务器时间（以毫秒为单位）或 {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/time">Redis 文档: TIME</a>
     */
    default Long time() {
        return time(TimeUnit.MILLISECONDS);
    }

    /**
     * 使用 {@code TIME} 命令请求服务器时间戳
     *
     * @param timeUnit 目标单位
     * @return {@link TimeUnit} 中的当前服务器时间或 {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/time">Redis 文档: TIME</a>
     */
    Long time(TimeUnit timeUnit);

    /**
     * 关闭由 {@literal host:port} 标识的给定客户端连接
     *
     * @param host 不能是 {@literal null}
     * @param port 不能是 {@literal null}
     * @see <a href="https://redis.io/commands/client-kill">Redis 文档: CLIENT KILL</a>
     */
    void killClient(String host, int port);

    /**
     * 为当前连接分配给定名称
     *
     * @param name name
     * @see <a href="https://redis.io/commands/client-setname">Redis 文档: CLIENT SETNAME</a>
     */
    void setClientName(byte[] name);

    /**
     * 返回当前连接的名称
     *
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/client-getname">Redis 文档: CLIENT GETNAME</a>
     */
    String getClientName();

    /**
     * 请求有关已连接客户端的信息和统计信息
     *
     * @return {@link List} of {@link RedisClientInfo} objects or {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/client-list">Redis 文档: CLIENT LIST</a>
     */
    List<RedisClientInfo> getClientList();

    /**
     * 将 redis 复制设置更改为新的主服务器
     *
     * @param host 不能是 {@literal null}
     * @param port 不能是 {@literal null}
     * @see <a href="https://redis.io/commands/slaveof">Redis 文档: SLAVEOF</a>
     */
    void slaveOf(String host, int port);

    /**
     * 将服务器更改为主服务器
     *
     * @see <a href="https://redis.io/commands/slaveof">Redis 文档: SLAVEOF</a>
     */
    void slaveOfNoOne();

    /**
     * 以原子方式将密钥从源 Redis 实例传输到目标 Redis 实例。<br/>
     * 成功后，密钥将从原始实例中删除，并保证存在于目标实例中。
     *
     * @param key     不能是 {@literal null}
     * @param target  不能是 {@literal null}
     * @param dbIndex 不能是 {@literal null}
     * @param option  可以是 {@literal null}。默认为 {@link RedisServerCommands.MigrateOption#COPY}
     * @see <a href="https://redis.io/commands/migrate">Redis 文档: MIGRATE</a>
     */
    void migrate(byte[] key, RedisNode target, int dbIndex, RedisServerCommands.MigrateOption option);

    /**
     * 以原子方式将密钥从源 Redis 实例传输到目标 Redis 实例。 <br/>
     * 成功后，密钥将从原始实例中删除，并保证存在于目标实例中。
     *
     * @param key     不能是 {@literal null}
     * @param target  不能是 {@literal null}
     * @param dbIndex 不能是 {@literal null}
     * @param option  可以是 {@literal null}。默认为 {@link RedisServerCommands.MigrateOption#COPY}
     * @param timeout 不能是 {@literal null}
     * @see <a href="https://redis.io/commands/migrate">Redis 文档: MIGRATE</a>
     */
    void migrate(byte[] key, RedisNode target, int dbIndex, RedisServerCommands.MigrateOption option, long timeout);
}
