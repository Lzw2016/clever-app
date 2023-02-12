package org.clever.data.redis.connection.lettuce;

import io.lettuce.core.api.sync.RedisServerCommands;
import org.clever.dao.InvalidDataAccessApiUsageException;
import org.clever.data.redis.connection.ClusterCommandExecutor.MultiNodeResult;
import org.clever.data.redis.connection.ClusterCommandExecutor.NodeResult;
import org.clever.data.redis.connection.RedisClusterNode;
import org.clever.data.redis.connection.RedisClusterServerCommands;
import org.clever.data.redis.connection.convert.Converters;
import org.clever.data.redis.connection.lettuce.LettuceClusterConnection.LettuceClusterCommandCallback;
import org.clever.data.redis.core.types.RedisClientInfo;
import org.clever.util.Assert;
import org.clever.util.CollectionUtils;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/01/30 22:20 <br/>
 */
class LettuceClusterServerCommands extends LettuceServerCommands implements RedisClusterServerCommands {
    private final LettuceClusterConnection connection;

    LettuceClusterServerCommands(LettuceClusterConnection connection) {
        super(connection);
        this.connection = connection;
    }

    @Override
    public void bgReWriteAof(RedisClusterNode node) {
        executeCommandOnSingleNode(RedisServerCommands::bgrewriteaof, node);
    }

    @Override
    public void bgSave(RedisClusterNode node) {
        executeCommandOnSingleNode(RedisServerCommands::bgsave, node);
    }

    @Override
    public Long lastSave(RedisClusterNode node) {
        return executeCommandOnSingleNode(client -> client.lastsave().getTime(), node).getValue();
    }

    @Override
    public void save(RedisClusterNode node) {
        executeCommandOnSingleNode(RedisServerCommands::save, node);
    }

    @Override
    public Long dbSize() {
        Collection<Long> dbSizes = executeCommandOnAllNodes(RedisServerCommands::dbsize).resultsAsList();
        if (CollectionUtils.isEmpty(dbSizes)) {
            return 0L;
        }
        Long size = 0L;
        for (Long value : dbSizes) {
            size += value;
        }
        return size;
    }

    @Override
    public Long dbSize(RedisClusterNode node) {
        return executeCommandOnSingleNode(RedisServerCommands::dbsize, node).getValue();
    }

    @Override
    public void flushDb() {
        executeCommandOnAllNodes(RedisServerCommands::flushdb);
    }

    @Override
    public void flushDb(RedisClusterNode node) {
        executeCommandOnSingleNode(RedisServerCommands::flushdb, node);
    }

    @Override
    public void flushAll() {
        executeCommandOnAllNodes(RedisServerCommands::flushall);
    }

    @Override
    public void flushAll(RedisClusterNode node) {
        executeCommandOnSingleNode(RedisServerCommands::flushall, node);
    }

    @Override
    public Properties info(RedisClusterNode node) {
        return LettuceConverters.toProperties(executeCommandOnSingleNode(RedisServerCommands::info, node).getValue());
    }

    @Override
    public Properties info() {
        Properties infos = new Properties();
        List<NodeResult<Properties>> nodeResults = executeCommandOnAllNodes(
                client -> LettuceConverters.toProperties(client.info())
        ).getResults();
        for (NodeResult<Properties> nodeProperties : nodeResults) {
            for (Entry<Object, Object> entry : nodeProperties.getValue().entrySet()) {
                infos.put(nodeProperties.getNode().asString() + "." + entry.getKey(), entry.getValue());
            }
        }
        return infos;
    }

    @Override
    public Properties info(String section) {
        Assert.hasText(section, "Section must not be null or empty!");
        Properties infos = new Properties();
        List<NodeResult<Properties>> nodeResults = executeCommandOnAllNodes(
                client -> LettuceConverters.toProperties(client.info(section))
        ).getResults();
        for (NodeResult<Properties> nodeProperties : nodeResults) {
            for (Entry<Object, Object> entry : nodeProperties.getValue().entrySet()) {
                infos.put(nodeProperties.getNode().asString() + "." + entry.getKey(), entry.getValue());
            }
        }
        return infos;
    }

    @Override
    public Properties info(RedisClusterNode node, String section) {
        Assert.hasText(section, "Section must not be null or empty!");
        return LettuceConverters.toProperties(executeCommandOnSingleNode(client -> client.info(section), node).getValue());
    }

    @Override
    public void shutdown(RedisClusterNode node) {
        executeCommandOnSingleNode((LettuceClusterCommandCallback<Void>) client -> {
            client.shutdown(true);
            return null;
        }, node);
    }

    @Override
    public Properties getConfig(String pattern) {
        Assert.hasText(pattern, "Pattern must not be null or empty!");
        List<NodeResult<Map<String, String>>> mapResult = executeCommandOnAllNodes(client -> client.configGet(pattern)).getResults();
        Properties properties = new Properties();
        for (NodeResult<Map<String, String>> entry : mapResult) {
            String prefix = entry.getNode().asString();
            entry.getValue().forEach((key, value) -> properties.setProperty(prefix + "." + key, value));
        }
        return properties;
    }

    @Override
    public Properties getConfig(RedisClusterNode node, String pattern) {
        Assert.hasText(pattern, "Pattern must not be null or empty!");
        return executeCommandOnSingleNode(client -> Converters.toProperties(client.configGet(pattern)), node).getValue();
    }

    @Override
    public void setConfig(String param, String value) {
        Assert.hasText(param, "Parameter must not be null or empty!");
        Assert.hasText(value, "Value must not be null or empty!");
        executeCommandOnAllNodes(client -> client.configSet(param, value));
    }

    @Override
    public void setConfig(RedisClusterNode node, String param, String value) {
        Assert.hasText(param, "Parameter must not be null or empty!");
        Assert.hasText(value, "Value must not be null or empty!");
        executeCommandOnSingleNode(client -> client.configSet(param, value), node);
    }

    @Override
    public void resetConfigStats() {
        executeCommandOnAllNodes(RedisServerCommands::configResetstat);
    }

    @Override
    public void resetConfigStats(RedisClusterNode node) {
        executeCommandOnSingleNode(RedisServerCommands::configResetstat, node);
    }

    @Override
    public void rewriteConfig() {
        executeCommandOnAllNodes(RedisServerCommands::configRewrite);
    }

    @Override
    public void rewriteConfig(RedisClusterNode node) {
        executeCommandOnSingleNode(RedisServerCommands::configRewrite, node);
    }

    @Override
    public Long time(RedisClusterNode node, TimeUnit timeUnit) {
        return convertListOfStringToTime(executeCommandOnSingleNode(RedisServerCommands::time, node).getValue(), timeUnit);
    }

    @Override
    public List<RedisClientInfo> getClientList() {
        List<String> map = executeCommandOnAllNodes(RedisServerCommands::clientList).resultsAsList();
        ArrayList<RedisClientInfo> result = new ArrayList<>();
        for (String infos : map) {
            result.addAll(LettuceConverters.toListOfRedisClientInformation(infos));
        }
        return result;
    }

    @Override
    public List<RedisClientInfo> getClientList(RedisClusterNode node) {
        return LettuceConverters.toListOfRedisClientInformation(executeCommandOnSingleNode(RedisServerCommands::clientList, node).getValue());
    }

    @Override
    public void slaveOf(String host, int port) {
        throw new InvalidDataAccessApiUsageException("SlaveOf is not supported in cluster environment. Please use CLUSTER REPLICATE.");
    }

    @Override
    public void slaveOfNoOne() {
        throw new InvalidDataAccessApiUsageException("SlaveOf is not supported in cluster environment. Please use CLUSTER REPLICATE.");
    }

    private <T> NodeResult<T> executeCommandOnSingleNode(LettuceClusterCommandCallback<T> command, RedisClusterNode node) {
        return connection.getClusterCommandExecutor().executeCommandOnSingleNode(command, node);
    }

    private <T> MultiNodeResult<T> executeCommandOnAllNodes(final LettuceClusterCommandCallback<T> cmd) {
        return connection.getClusterCommandExecutor().executeCommandOnAllNodes(cmd);
    }

    private static Long convertListOfStringToTime(List<byte[]> serverTimeInformation, TimeUnit timeUnit) {
        Assert.notEmpty(serverTimeInformation, "Received invalid result from server. Expected 2 items in collection.");
        Assert.isTrue(
                serverTimeInformation.size() == 2,
                "Received invalid number of arguments from redis server. Expected 2 received " + serverTimeInformation.size()
        );
        return Converters.toTimeMillis(
                LettuceConverters.toString(serverTimeInformation.get(0)),
                LettuceConverters.toString(serverTimeInformation.get(1)),
                timeUnit
        );
    }
}
