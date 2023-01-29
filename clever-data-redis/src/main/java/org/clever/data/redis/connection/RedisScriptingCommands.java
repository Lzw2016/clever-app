package org.clever.data.redis.connection;

import java.util.List;

/**
 * 脚本命令
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 17:25 <br/>
 */
public interface RedisScriptingCommands {
    /**
     * 刷新lua脚本缓存
     *
     * @see <a href="https://redis.io/commands/script-flush">Redis 文档: SCRIPT FLUSH</a>
     */
    void scriptFlush();

    /**
     * 终止当前lua脚本执行
     *
     * @see <a href="https://redis.io/commands/script-kill">Redis 文档: SCRIPT KILL</a>
     */
    void scriptKill();

    /**
     * 将lua脚本加载到脚本缓存中，而不执行它。<br>
     * 通过调用 {@link #evalSha(byte[], ReturnType, int, byte[]...)} 来执行脚本。
     *
     * @param script 不能是 {@literal null}
     * @return {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/script-load">Redis 文档: SCRIPT LOAD</a>
     */
    String scriptLoad(byte[] script);

    /**
     * 检查脚本缓存中是否存在给定的 不能是 {@literal null}
     *
     * @return 在管道/事务中使用时，每个给定的scriptSha在返回的 {@link List} 或 {@literal null} 中有一个条目
     * @see <a href="https://redis.io/commands/script-exists">Redis 文档: SCRIPT EXISTS</a>
     */
    List<Boolean> scriptExists(String... scriptShas);

    /**
     * 计算给定的 {@code script}
     *
     * @param script      不能是 {@literal null}
     * @param returnType  不能是 {@literal null}
     * @param numKeys     不能是 {@literal null}
     * @param keysAndArgs 不能是 {@literal null}
     * @return 脚本的结果。 {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/eval">Redis 文档: EVAL</a>
     */
    <T> T eval(byte[] script, ReturnType returnType, int numKeys, byte[]... keysAndArgs);

    /**
     * 计算给定 {@code scriptSha}
     *
     * @param scriptSha   不能是 {@literal null}
     * @param returnType  不能是 {@literal null}
     * @param numKeys     不能是 {@literal null}
     * @param keysAndArgs 不能是 {@literal null}
     * @return 脚本的结果。 {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/evalsha">Redis 文档: EVALSHA</a>
     */
    <T> T evalSha(String scriptSha, ReturnType returnType, int numKeys, byte[]... keysAndArgs);

    /**
     * 计算给定 {@code scriptSha}
     *
     * @param scriptSha   不能是 {@literal null}
     * @param returnType  不能是 {@literal null}
     * @param numKeys     不能是 {@literal null}
     * @param keysAndArgs 不能是 {@literal null}
     * @return 脚本的结果。 {@literal null} 在管道/事务中使用时
     * @see <a href="https://redis.io/commands/evalsha">Redis 文档: EVALSHA</a>
     */
    <T> T evalSha(byte[] scriptSha, ReturnType returnType, int numKeys, byte[]... keysAndArgs);
}
