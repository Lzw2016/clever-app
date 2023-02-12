package org.clever.data.redis.connection.lettuce;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/01/30 22:19 <br/>
 */
class LettuceClusterZSetCommands extends LettuceZSetCommands {
    LettuceClusterZSetCommands(LettuceClusterConnection connection) {
        super(connection);
    }
}
