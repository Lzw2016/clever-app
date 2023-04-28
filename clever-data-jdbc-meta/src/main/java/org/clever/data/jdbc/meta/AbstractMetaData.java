package org.clever.data.jdbc.meta;

import org.apache.commons.lang3.StringUtils;
import org.clever.data.jdbc.meta.model.Column;
import org.clever.data.jdbc.meta.model.Schema;
import org.clever.data.jdbc.meta.model.Table;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/04/28 10:15 <br/>
 */
public abstract class AbstractMetaData implements DataBaseMetaData {
    /**
     * 忽略Schema名(全小写过滤)
     */
    private final Set<String> ignoreSchemas = new HashSet<>();
    /**
     * 忽略表名(全小写过滤)
     */
    private final Set<String> ignoreTables = new HashSet<>();
    /**
     * 忽略表前缀(全小写过滤)
     */
    private final Set<String> ignoreTablesPrefix = new HashSet<>();
    /**
     * 忽略表后缀(全小写过滤)
     */
    private final Set<String> ignoreTablesSuffix = new HashSet<>();

    /**
     * 忽略Schema名(全小写过滤)
     */
    public void addIgnoreSchema(String schemaName) {
        if (StringUtils.isBlank(schemaName)) {
            return;
        }
        ignoreSchemas.add(schemaName);
    }

    /**
     * 忽略表名(全小写过滤)
     */
    public void addIgnoreTable(String tableName) {
        if (StringUtils.isBlank(tableName)) {
            return;
        }
        ignoreTables.add(tableName);
    }

    /**
     * 忽略表前缀(全小写过滤)
     */
    public void addIgnoreTablePrefix(String tablePrefix) {
        if (StringUtils.isBlank(tablePrefix)) {
            return;
        }
        ignoreTablesPrefix.add(tablePrefix);
    }

    /**
     * 忽略表后缀(全小写过滤)
     */
    public void addIgnoreTableSuffix(String tableSuffix) {
        if (StringUtils.isBlank(tableSuffix)) {
            return;
        }
        ignoreTablesSuffix.add(tableSuffix);
    }

    /**
     * 忽略Schema名(全小写过滤)
     */
    public Set<String> getIgnoreSchemas() {
        return ignoreSchemas.stream().map(StringUtils::lowerCase).collect(Collectors.toSet());
    }

    /**
     * 忽略表名(全小写过滤)
     */
    public Set<String> getIgnoreTables() {
        return ignoreTables.stream().map(StringUtils::lowerCase).collect(Collectors.toSet());
    }

    /**
     * 忽略表前缀(全小写过滤)
     */
    public Set<String> getIgnoreTablesPrefix() {
        return ignoreTablesPrefix.stream().map(StringUtils::lowerCase).collect(Collectors.toSet());
    }

    /**
     * 忽略表后缀(全小写过滤)
     */
    public Set<String> getIgnoreTablesSuffix() {
        return ignoreTablesSuffix.stream().map(StringUtils::lowerCase).collect(Collectors.toSet());
    }

    @Override
    public List<Schema> getSchemas() {
        return getSchemas(null, null);
    }

    @Override
    public Schema getSchema(String schemaName) {
        List<Schema> schemas = getSchemas(Collections.singletonList(schemaName), null);
        return schemas.isEmpty() ? null : schemas.get(0);
    }

    @Override
    public Schema getSchema() {
        String currentSchema = currentSchema();
        List<Schema> schemas = getSchemas(Collections.singletonList(currentSchema), null);
        return schemas.isEmpty() ? null : schemas.get(0);
    }

    @Override
    public Table getTable(String schemaName, String tableName) {
        List<Schema> schemas = getSchemas(Collections.singletonList(schemaName), Collections.singletonList(tableName));
        if (schemas.isEmpty()) {
            return null;
        }
        Schema schema = schemas.get(0);
        return schema.getTables().isEmpty() ? null : schema.getTables().get(0);
    }

    /**
     * 生成 where in 条件 sql “f in ( whereIn )” 中的 whereIn 片段
     */
    protected String createWhereIn(Map<String, Object> params, Collection<?> values) {
        StringBuilder whereIn = new StringBuilder();
        int idx = 0;
        for (Object value : values) {
            if (idx > 0) {
                whereIn.append(", ");
            }
            idx++;
            String paramName = "param_" + params.size();
            whereIn.append(":").append(paramName);
            params.put(paramName, value);
        }
        return whereIn.toString();
    }

    /**
     * 对 Schema 集合进行深度排序
     */
    protected void sort(List<Schema> schemas) {
        schemas.sort(Comparator.comparing(Schema::getName));
        for (Schema schema : schemas) {
            List<Table> tables = schema.getTables();
            tables.sort(Comparator.comparing(Table::getName));
            for (Table table : tables) {
                List<Column> columns = table.getColumns();
                columns.sort(Comparator.comparing(Column::getOrdinalPosition));
            }
        }
    }
}
