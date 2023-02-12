package org.clever.data.redis.connection.lettuce;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/01/30 17:32 <br/>
 */
class LettuceClusterGeoCommands extends LettuceGeoCommands {
    LettuceClusterGeoCommands(LettuceClusterConnection connection) {
        super(connection);
    }
}
