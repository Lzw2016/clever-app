package org.clever.data.redis.connection.lettuce;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/01/30 17:35 <br/>
 */
class LettuceClusterHashCommands extends LettuceHashCommands {
    LettuceClusterHashCommands(LettuceClusterConnection connection) {
        super(connection);
    }
}
