package org.clever.data.redis.connection;

import org.clever.util.Assert;
import org.clever.util.ObjectUtils;
import org.clever.util.StringUtils;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 17:30 <br/>
 */
public class RedisNode implements NamedNode {
    String id;
    String name;
    String host;
    int port;
    NodeType type;
    String masterId;

    /**
     * 使用给定的 {@code host}、{@code port} 创建一个新的 {@link RedisNode}
     *
     * @param host 不能是 {@literal null}
     * @param port 不能是 {@literal null}
     */
    public RedisNode(String host, int port) {
        Assert.notNull(host, "host must not be null!");
        this.host = host;
        this.port = port;
    }

    protected RedisNode() {
    }

    private RedisNode(RedisNode redisNode) {
        this.id = redisNode.id;
        this.name = redisNode.name;
        this.host = redisNode.host;
        this.port = redisNode.port;
        this.type = redisNode.type;
        this.masterId = redisNode.masterId;
    }

    /**
     * 将 {@code hostAndPort} 字符串解析为 {@link RedisNode}。支持 IPv4、IPv6 和主机名符号，包括端口。例如：
     * <pre>{@code
     * RedisNode.fromString("127.0.0.1:6379");
     * RedisNode.fromString("[aaaa:bbbb::dddd:eeee]:6379");
     * RedisNode.fromString("my.redis.server:6379");
     * }</pre>
     *
     * @param hostPortString 不能是 {@literal null} or empty.
     * @return 解析后的 {@link RedisNode}
     */
    public static RedisNode fromString(String hostPortString) {
        Assert.notNull(hostPortString, "HostAndPort must not be null");
        String host;
        String portString = null;
        if (hostPortString.startsWith("[")) {
            String[] hostAndPort = getHostAndPortFromBracketedHost(hostPortString);
            host = hostAndPort[0];
            portString = hostAndPort[1];
        } else {
            int colonPos = hostPortString.indexOf(':');
            if (colonPos >= 0 && hostPortString.indexOf(':', colonPos + 1) == -1) {
                // Exactly 1 colon. Split into host:port.
                host = hostPortString.substring(0, colonPos);
                portString = hostPortString.substring(colonPos + 1);
            } else {
                // 0 or 2+ colons. Bare hostname or IPv6 literal.
                host = hostPortString;
            }
        }
        int port = -1;
        try {
            assert portString != null;
            port = Integer.parseInt(portString);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(String.format("Unparseable port number: %s", hostPortString));
        }
        if (!isValidPort(port)) {
            throw new IllegalArgumentException(String.format("Port number out of range: %s", hostPortString));
        }
        return new RedisNode(host, port);
    }

    /**
     * 解析括号中的主机端口字符串，如果解析失败则抛出 IllegalArgumentException
     *
     * @param hostPortString 完整的括号主机端口规范。可能未指定帖子
     * @return 一个包含 2 个字符串的数组：主机和端口，按顺序排列
     * @throws IllegalArgumentException 如果解析括号内的主机端口字符串失败
     */
    private static String[] getHostAndPortFromBracketedHost(String hostPortString) {
        if (hostPortString.charAt(0) != '[') {
            throw new IllegalArgumentException(String.format("Bracketed host-port string must start with a bracket: %s", hostPortString));
        }
        int colonIndex = hostPortString.indexOf(':');
        int closeBracketIndex = hostPortString.lastIndexOf(']');
        if (!(colonIndex > -1 && closeBracketIndex > colonIndex)) {
            throw new IllegalArgumentException(String.format("Invalid bracketed host/port: %s", hostPortString));
        }
        String host = hostPortString.substring(1, closeBracketIndex);
        if (closeBracketIndex + 1 == hostPortString.length()) {
            return new String[]{host, ""};
        } else {
            if (!(hostPortString.charAt(closeBracketIndex + 1) == ':')) {
                throw new IllegalArgumentException(String.format("Only a colon may follow a close bracket: %s", hostPortString));
            }
            for (int i = closeBracketIndex + 2; i < hostPortString.length(); ++i) {
                if (!Character.isDigit(hostPortString.charAt(i))) {
                    throw new IllegalArgumentException(String.format("Port must be numeric: %s", hostPortString));
                }
            }
            return new String[]{host, hostPortString.substring(closeBracketIndex + 2)};
        }
    }

    /**
     * @return 可以是 {@literal null}
     */
    public String getHost() {
        return host;
    }

    /**
     * @return 此节点是否具有有效主机（不为空且不为空）
     */
    public boolean hasValidHost() {
        return StringUtils.hasText(host);
    }

    /**
     * @return 可以是 {@literal null}
     */
    public Integer getPort() {
        return port;
    }

    public String asString() {
        if (host != null && host.contains(":")) {
            return "[" + host + "]:" + port;
        }
        return host + ":" + port;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return 可以是 {@literal null}
     */
    public String getMasterId() {
        return masterId;
    }

    /**
     * @return 可以是 {@literal null}
     */
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return 可以是 {@literal null}
     */
    public NodeType getType() {
        return type;
    }

    public boolean isMaster() {
        return ObjectUtils.nullSafeEquals(NodeType.MASTER, getType());
    }

    public boolean isSlave() {
        return isReplica();
    }

    public boolean isReplica() {
        return ObjectUtils.nullSafeEquals(NodeType.SLAVE, getType());
    }

    /**
     * 获取 {@link RedisNodeBuilder} 以创建新的 {@link RedisNode}
     *
     * @return 从不为 {@literal null}
     */
    public static RedisNodeBuilder newRedisNode() {
        return new RedisNodeBuilder();
    }

    @Override
    public String toString() {
        return asString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ObjectUtils.nullSafeHashCode(host);
        result = prime * result + ObjectUtils.nullSafeHashCode(port);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RedisNode)) {
            return false;
        }
        RedisNode other = (RedisNode) obj;
        if (!ObjectUtils.nullSafeEquals(this.host, other.host)) {
            return false;
        }
        if (!ObjectUtils.nullSafeEquals(this.port, other.port)) {
            return false;
        }
        // noinspection RedundantIfStatement
        if (!ObjectUtils.nullSafeEquals(this.name, other.name)) {
            return false;
        }
        return true;
    }

    public enum NodeType {
        MASTER, SLAVE
    }

    /**
     * 用于创建新的 {@link RedisNode} 的构建器
     */
    public static class RedisNodeBuilder {
        private final RedisNode node;

        public RedisNodeBuilder() {
            node = new RedisNode();
        }

        /**
         * 定义节点名称
         */
        public RedisNodeBuilder withName(String name) {
            node.name = name;
            return this;
        }

        /**
         * 设置服务器的主机和端口
         *
         * @param host 不能是 {@literal null}
         * @param port 不能是 {@literal null}
         */
        public RedisNodeBuilder listeningAt(String host, int port) {

            Assert.notNull(host, "Hostname must not be null.");
            node.host = host;
            node.port = port;
            return this;
        }

        /**
         * 设置服务器的id
         */
        public RedisNodeBuilder withId(String id) {
            node.id = id;
            return this;
        }

        /**
         * 设置服务器角色
         */
        public RedisNodeBuilder promotedAs(NodeType type) {
            node.type = type;
            return this;
        }

        /**
         * 设置主节点的id
         */
        public RedisNodeBuilder slaveOf(String masterId) {
            return replicaOf(masterId);
        }

        /**
         * 设置主节点的id
         */
        public RedisNodeBuilder replicaOf(String masterId) {
            node.masterId = masterId;
            return this;
        }

        /**
         * 获取 {@link RedisNode}.
         */
        public RedisNode build() {
            return new RedisNode(this.node);
        }
    }

    private static boolean isValidPort(int port) {
        return port >= 0 && port <= 65535;
    }
}
